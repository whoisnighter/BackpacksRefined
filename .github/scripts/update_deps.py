"""Bumps the Minecraft-bound dependency versions in stonecutter.properties.toml.

Dependabot cannot do this. The versions live in a Stonecutter properties file it does not read, and
even if it did, it would offer every node the newest Fabric API, the one built for the newest
Minecraft. This asks Modrinth instead, once per node, for the newest upload of each mod built for that
node's loader and Minecraft version, and writes it back in place, comments and layout untouched.

    python .github/scripts/update_deps.py            # rewrite the file
    python .github/scripts/update_deps.py --dry-run  # only report
    python .github/scripts/update_deps.py --summary build/deps-summary.md
    python .github/scripts/update_deps.py --check    # only report where README.md disagrees

Rules:
- A node is `<minecraft>-<loader>`, declared in settings.gradle.kts with `node("<minecraft>", ...)`.
  Its values are read and rewritten in its loader table, `[fabric."26.3"]`, or else in the shared
  `["26.3"]` table. A candidate has to list that Minecraft version and be built for that loader.
- Mods track the newest: `deps.fabric_api`, `deps.modmenu` and `deps.cloth_config` from Modrinth, and
  `deps.mixinextras` (nested in the Forge jar only) from Maven Central, releases only. Only release
  uploads are taken, unless the pinned version is itself a beta or alpha: a node on a pre-release stays
  on that channel until a release catches up. A candidate has to be published after the pinned version.
  Nothing is ever downgraded.
- The loaders are never bumped. `deps.fabric_loader`, `deps.neoforge` and `deps.forge` are what the
  build compiles against and the lower bound the manifests write, so a bump would make every player
  update their loader along with the mod. What this moves is `deps.*_latest` beside each, which nothing
  else reads, and the pull request prints the loader's changelog from the pin up to it. A person raises
  a pin by copying its `_latest` over it, when the mod needs a newer loader.
- Fabric Loader is one global value, from Fabric's meta API, stable builds only. NeoForge comes from
  maven.neoforged.net, the newest build for the same Minecraft version, releases only unless the pin is
  a beta. Forge is the build files.minecraftforge.net promotes as latest for the node's version.
- README.md's Requirements table prints Fabric Loader, Fabric API and the NeoForge and Forge pins, so a
  bump of Fabric API rewrites it there too, and only there: CHANGELOG.md records what a past release was
  built against. `--check` goes the other way and fails when README.md does not name a pinned version,
  which is what a bump made by hand leaves behind; it needs no network.

Standard library only, so CI needs nothing but a Python interpreter.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROPERTIES = ROOT / "stonecutter.properties.toml"
SETTINGS = ROOT / "settings.gradle.kts"
README = ROOT / "README.md"
LOADERS = ("fabric", "neoforge", "forge")

MODRINTH = "https://api.modrinth.com/v2"
FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader"
NEOFORGE_METADATA = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
FORGE_PROMOTIONS = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json"
MIXINEXTRAS_METADATA = "https://repo1.maven.org/maven2/io/github/llamalad7/mixinextras-forge/maven-metadata.xml"
# Beside every NeoForge and Forge build, a text file with the changelog of every build in its line.
NEOFORGE_CHANGELOG = "https://maven.neoforged.net/releases/net/neoforged/neoforge/{v}/neoforge-{v}-changelog.txt"
FORGE_CHANGELOG = "https://maven.minecraftforge.net/net/minecraftforge/forge/{v}/forge-{v}-changelog.txt"
FABRIC_LOADER_RELEASES = "https://api.github.com/repos/FabricMC/fabric-loader/releases?per_page=100"
# Modrinth asks every client for a User-Agent that identifies the project.
USER_AGENT = "whoisnighter/BackpacksRefined dependency updater (github.com/whoisnighter/BackpacksRefined)"

# `deps.<key>` -> Modrinth project. Add a line when a build script gains a Modrinth `deps.*` property.
MODRINTH_DEPS = {
    "fabric_api": "fabric-api",
    "modmenu": "modmenu",
    "cloth_config": "cloth-config",
}
# The keys README.md prints, so a bump rewrites it and `--check` asks for them.
README_KEYS = {"fabric_api"}
LOADER_KEYS = {"fabric_loader_latest", "neoforge_latest", "forge_latest"}


@dataclass
class Change:
    node: str | None
    """The node, or None for a global value."""
    key: str
    old: str
    new: str
    url: str
    project: str | None = None
    changelogs: tuple[tuple[str, str], ...] = ()
    """(version, changelog) of every upload after the pinned one up to the new one, oldest first."""


def request(url: str) -> urllib.request.Request:
    return urllib.request.Request(url, headers={"User-Agent": USER_AGENT})


def get_text(url: str) -> str:
    with urllib.request.urlopen(request(url), timeout=30) as response:
        return response.read().decode("utf-8")


def get_json(url: str):
    return json.loads(get_text(url))


def numeric(version: str) -> tuple[int, ...]:
    return tuple(int(part) for part in re.findall(r"\d+", version))


def nodes() -> dict[str, str]:
    """Each node mapped to its Minecraft version, from the `node(...)` lines of settings.gradle.kts."""
    found: dict[str, str] = {}
    for args in re.findall(r'^\s*node\(([^)]*)\)', SETTINGS.read_text(encoding="utf-8"), re.MULTILINE):
        minecraft, *loaders = re.findall(r'"([^"]+)"', args)
        for loader in loaders:
            found[f"{minecraft}-{loader}"] = minecraft
    if not found:
        sys.exit(f"No node(...) lines found in {SETTINGS.name}.")
    return found


def split(node: str) -> tuple[str, str]:
    minecraft, loader = node.rsplit("-", 1)
    return minecraft, loader


class Properties:
    """The properties file as lines, so a rewrite keeps every comment and blank line."""

    def __init__(self, path: Path):
        self.path = path
        # newline="" keeps CRLF on a Windows checkout, so the rewrite only touches the changed values.
        with path.open(encoding="utf-8", newline="") as file:
            self.lines = file.read().splitlines(keepends=True)

    @staticmethod
    def _pattern(key: str) -> re.Pattern[str]:
        return re.compile(r'^(\s*' + re.escape(key) + r'\s*=\s*")([^"]*)(".*)$', re.DOTALL)

    def _find_in(self, table: tuple[str | None, str] | None, key: str) -> tuple[int, str] | None:
        current: tuple[str | None, str] | None = None
        pattern = self._pattern(key)
        for index, line in enumerate(self.lines):
            header = re.match(r'^\s*\[(?:(' + "|".join(LOADERS) + r')\.)?"([^"]+)"\]\s*$', line)
            if header:
                current = (header.group(1), header.group(2))
                continue
            if current == table:
                match = pattern.match(line)
                if match:
                    return index, match.group(2)
        return None

    def find(self, node: str | None, key: str) -> tuple[int, str] | None:
        """Line index and value of `key` for `node`: its loader table, else its version's shared table,
        or the top level when node is None."""
        if node is None:
            return self._find_in(None, key)
        minecraft, loader = split(node)
        return self._find_in((loader, minecraft), key) or self._find_in((None, minecraft), key)

    def has_node(self, node: str) -> bool:
        minecraft, loader = split(node)
        return any(line.strip() == f'[{loader}."{minecraft}"]' for line in self.lines)

    def set(self, index: int, key: str, value: str) -> None:
        match = self._pattern(key).match(self.lines[index])
        assert match, self.lines[index]
        self.lines[index] = match.group(1) + value + match.group(3)

    def save(self) -> None:
        self.path.write_text("".join(self.lines), encoding="utf-8", newline="")


def check_modrinth(props: Properties, node: str, key: str, project: str, changes: list[Change],
                   warnings: list[str]) -> None:
    found = props.find(node, f"deps.{key}")
    if found is None:
        return
    index, pinned = found
    minecraft, loader = split(node)
    try:
        current = get_json(f"{MODRINTH}/project/{project}/version/{urllib.parse.quote(pinned, safe='')}")
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        warnings.append(f"`{node}` `deps.{key}` = `{pinned}` was not found on Modrinth ({project}); skipped.")
        return

    query = urllib.parse.urlencode({"loaders": json.dumps([loader]), "game_versions": json.dumps([minecraft]),
                                    "include_changelog": "true"})
    channels = {"release", current["version_type"]}
    candidates = sorted((v for v in get_json(f"{MODRINTH}/project/{project}/version?{query}")
                         if v["version_type"] in channels), key=lambda v: v["date_published"], reverse=True)
    if not candidates or candidates[0]["date_published"] <= current["date_published"]:
        return
    newest = candidates[0]
    if newest["version_number"] == pinned:
        return
    props.set(index, f"deps.{key}", newest["version_number"])
    changes.append(Change(
        node, key, pinned, newest["version_number"],
        f"https://modrinth.com/mod/{project}/version/{newest['id']}", project,
        tuple((v["version_number"], (v.get("changelog") or "").strip())
              for v in reversed(candidates) if v["date_published"] > current["date_published"]),
    ))


def tracked(props: Properties, node: str | None, key: str, warnings: list[str]) -> tuple[int, str] | None:
    """`deps.<key>_latest`, the newest loader build seen, for a node that pins `deps.<key>`."""
    if props.find(node, f"deps.{key}") is None:
        return None
    found = props.find(node, f"deps.{key}_latest")
    if found is None:
        warnings.append(f"`{node or 'all'}` pins `deps.{key}` but has no `deps.{key}_latest`; add one, equal to the pin.")
    return found


def track(props: Properties, node: str | None, key: str, newest: str | None, url: str,
          changes: list[Change], warnings: list[str]) -> None:
    found = tracked(props, node, key, warnings)
    if found is None or newest is None:
        return
    index, seen = found
    if numeric(newest) <= numeric(seen):
        return
    props.set(index, f"deps.{key}_latest", newest)
    changes.append(Change(node, f"{key}_latest", seen, newest, url))


def newest_fabric_loader() -> str | None:
    stable = [v["version"] for v in get_json(FABRIC_META) if v.get("stable")]
    return max(stable, key=numeric) if stable else None


_neoforge: list[str] | None = None


def newest_neoforge(pinned: str) -> str | None:
    """NeoForge's version starts with the Minecraft version and ends with the build, `26.3.0.83`, so only
    builds sharing all but the last part are candidates. Betas only when the pin is one."""
    global _neoforge
    if _neoforge is None:
        _neoforge = re.findall(r"<version>([^<]+)</version>", get_text(NEOFORGE_METADATA))
    prefix = ".".join(pinned.split("-")[0].split(".")[:-1]) + "."
    beta = "-" in pinned
    candidates = [v for v in _neoforge if v.startswith(prefix) and (beta or "-" not in v)]
    return max(candidates, key=numeric) if candidates else None


def newest_forge(minecraft: str) -> str | None:
    return get_json(FORGE_PROMOTIONS)["promos"].get(f"{minecraft}-latest")


def check_mixinextras(props: Properties, node: str, changes: list[Change]) -> None:
    found = props.find(node, "deps.mixinextras")
    if found is None:
        return
    index, pinned = found
    release = re.search(r"<release>([^<]+)</release>", get_text(MIXINEXTRAS_METADATA))
    if release is None or "-" in release.group(1) or numeric(release.group(1)) <= numeric(pinned):
        return
    props.set(index, "deps.mixinextras", release.group(1))
    changes.append(Change(node, "mixinextras", pinned, release.group(1),
                          "https://github.com/LlamaLad7/MixinExtras/releases"))


def read(path: Path) -> str:
    with path.open(encoding="utf-8", newline="") as file:
        return file.read()


def update_readme(changes: list[Change], dry_run: bool) -> list[str]:
    """Rewrites the versions README.md prints, so it keeps saying what the build uses."""
    swaps = {c.old: c.new for c in changes if c.key in README_KEYS}
    if not swaps:
        return []
    text = original = read(README)
    made = []
    for old, new in swaps.items():
        if old in text:
            text = text.replace(old, new)
            made.append(f"{old} -> {new}")
    if text != original and not dry_run:
        README.write_text(text, encoding="utf-8", newline="")
    return made


def check_readme(props: Properties) -> list[str]:
    """Every pin README.md is meant to print, checked against the properties file. No network."""
    text = read(README)
    problems = []
    loader = props.find(None, "deps.fabric_loader")
    if loader and loader[1] not in text:
        problems.append(f"README.md does not name Fabric Loader {loader[1]}, which the build pins.")
    for node in nodes():
        if not props.has_node(node):
            continue
        for key in ("neoforge", "forge", *README_KEYS):
            found = props.find(node, f"deps.{key}")
            if found and found[1] not in text:
                problems.append(f"README.md does not name {key} {found[1]}, which `{node}` pins.")
    return problems


# GitHub caps a pull request body at 65536 characters.
ENTRY_LIMIT = 4000
SECTION_LIMIT = 40000
# ` - 47.4.28 text` in Forge's file, `` - `21.1.264` text`` in NeoForge's. Indented lines continue it.
CHANGELOG_ENTRY = re.compile(r"^ - `?(\d[^`\s]*)`?\s?(.*)$")


def maven_changelog(url: str, low: str, high: str) -> list[tuple[str, str]]:
    entries: list[tuple[str, list[str]]] = []
    for line in get_text(url).splitlines():
        match = CHANGELOG_ENTRY.match(line)
        if match:
            entries.append((match.group(1), [match.group(2)]))
        elif not line[:1].isspace():
            entries.append(("", []))
        elif entries and line.strip() and not line.strip().lower().startswith("co-authored-by:"):
            entries[-1][1].append(line.strip())
    return [(v, "\n".join(lines)) for v, lines in entries if v and numeric(low) < numeric(v) <= numeric(high)]


def fabric_loader_changelog(low: str, high: str) -> list[tuple[str, str]]:
    req = request(FABRIC_LOADER_RELEASES)
    if os.environ.get("GITHUB_TOKEN"):
        req.add_header("Authorization", f"Bearer {os.environ['GITHUB_TOKEN']}")
    with urllib.request.urlopen(req, timeout=30) as response:
        releases = json.load(response)
    found = [(r["tag_name"], (r.get("body") or "").strip()) for r in releases
             if numeric(low) < numeric(r["tag_name"]) <= numeric(high)]
    return sorted(found, key=lambda entry: numeric(entry[0]), reverse=True)


def details(title: str, body: str) -> list[str]:
    # A fence, so a changelog's `#123` and `@name` do not link into this repository.
    return ["<details>", f"<summary>{title}</summary>", "", "~~~~text", body, "~~~~", "", "</details>", ""]


def loader_changelogs(props: Properties, warnings: list[str]) -> list[str]:
    """What changed in each loader from the pin up to the newest build, read from the properties file
    so a range an earlier pull request opened is printed again until its pin catches up."""
    ranges = []
    low, high = props.find(None, "deps.fabric_loader"), props.find(None, "deps.fabric_loader_latest")
    if low and high:
        ranges.append(("Fabric Loader", low[1], high[1], FABRIC_LOADER_RELEASES,
                       lambda a=low[1], b=high[1]: fabric_loader_changelog(a, b)))
    for node, minecraft in nodes().items():
        for key, name, template, build in (("neoforge", "NeoForge", NEOFORGE_CHANGELOG, "{v}"),
                                           ("forge", "Forge", FORGE_CHANGELOG, f"{minecraft}-{{v}}")):
            low, high = props.find(node, f"deps.{key}"), props.find(node, f"deps.{key}_latest")
            if low and high:
                url = template.format(v=build.format(v=high[1]))
                ranges.append((f"{name} on `{node}`", low[1], high[1], url,
                               lambda u=url, a=low[1], b=high[1]: maven_changelog(u, a, b)))
    out, size = [], 0
    for name, low, high, url, fetch in ranges:
        if numeric(high) <= numeric(low):
            continue
        try:
            entries = fetch()
        except (urllib.error.URLError, TimeoutError, ValueError, KeyError) as error:
            warnings.append(f"The {name} changelog was not read ({error}); it is at {url}.")
            entries = []
        body = "\n".join(f"{v}: {t}" if t else v for v, t in entries) or "(no changelog)"
        if size + len(body) > SECTION_LIMIT // 4:
            out += [f"- {name} `{low}` -> `{high}`: cut, the body is full. Read it at {url}.", ""]
            continue
        size += len(body)
        out += details(f"{name}: <code>{low}</code> -> <code>{high}</code>", body)
    if not out:
        return []
    return ["### Loader changelogs", "",
            "Every build after the pin, up to the newest. Players are asked only for the pin. Raise it, by "
            "copying the `_latest` value over it, when the mod needs a newer loader.", ""] + out


def mod_changelogs(changes: list[Change]) -> list[str]:
    by_project: dict[str, dict[str, list[str]]] = {}
    for change in changes:
        for number, text in change.changelogs:
            versions = by_project.setdefault(change.project, {}).setdefault(text, [])
            if number not in versions:
                versions.append(number)
    out, size = [], 0
    for project, texts in sorted(by_project.items()):
        for text, versions in texts.items():
            label = ", ".join(f"<code>{v}</code>" for v in versions)
            body = (text or "(no changelog)")[:ENTRY_LIMIT]
            if size + len(body) > SECTION_LIMIT:
                out += [f"- `{project}` {label}: cut, the body is full. Read it on Modrinth.", ""]
                continue
            size += len(body)
            out += details(f"<code>{project}</code> {label}", body)
    return ["### Changelogs", "", "Every upload each bump moves past.", ""] + out if out else []


def summary(changes: list[Change], warnings: list[str], readme: list[str], loader_logs: list[str]) -> str:
    out = ["Updates the Minecraft-bound dependencies in `stonecutter.properties.toml`. Each candidate is an "
           "upload for its node's loader that lists the node's Minecraft version.", ""]
    if changes:
        out += ["| Node | Dependency | From | To |", "| --- | --- | --- | --- |"]
        out += [f"| {c.node or 'all'} | `{c.key}` | `{c.old}` | [`{c.new}`]({c.url}) |" for c in changes] + [""]
    notes = []
    if any(c.key in LOADER_KEYS for c in changes):
        notes.append("A `_latest` loader value only tracks the newest build. The build and the manifests stay on "
                     "`deps.fabric_loader`, `deps.neoforge` and `deps.forge`, so nothing players need changes.")
    if readme:
        notes.append(f"README.md was rewritten to match: {', '.join(f'`{s}`' for s in readme)}")
    notes += warnings
    if notes:
        out += ["### Notes", ""] + [f"- {n}" for n in notes] + [""]
    return "\n".join(out + loader_logs + mod_changelogs(changes))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dry-run", action="store_true", help="report what would change without writing")
    parser.add_argument("--check", action="store_true", help="only report pins README.md does not name")
    parser.add_argument("--summary", type=Path, help="write a Markdown summary here, for a pull request body")
    args = parser.parse_args()
    props = Properties(PROPERTIES)

    if args.check:
        problems = check_readme(props)
        for problem in problems:
            print(problem, file=sys.stderr)
        if not problems:
            print("README.md names every pinned version.")
        return 1 if problems else 0

    changes: list[Change] = []
    warnings: list[str] = []

    def guarded(what: str, step) -> None:
        # One source being down does not hold back the bumps the others found; tomorrow's run retries.
        try:
            step()
        except (urllib.error.URLError, TimeoutError, ValueError, KeyError) as error:
            warnings.append(f"{what} was not checked ({error}).")

    guarded("`deps.fabric_loader_latest`", lambda: track(
        props, None, "fabric_loader", newest_fabric_loader(),
        "https://github.com/FabricMC/fabric-loader/releases", changes, warnings))
    for node, minecraft in nodes().items():
        if not props.has_node(node):
            warnings.append(f"`{node}` is in settings.gradle.kts but has no loader table; skipped.")
            continue
        for key, project in MODRINTH_DEPS.items():
            guarded(f"`{node}` `deps.{key}`",
                    lambda key=key, project=project: check_modrinth(props, node, key, project, changes, warnings))
        _, loader = split(node)
        if loader == "neoforge":
            pin = props.find(node, "deps.neoforge")
            guarded(f"`{node}` `deps.neoforge_latest`", lambda: track(
                props, node, "neoforge", newest_neoforge(pin[1]) if pin else None,
                "https://projects.neoforged.net/neoforged/neoforge", changes, warnings))
        if loader == "forge":
            guarded(f"`{node}` `deps.forge_latest`", lambda: track(
                props, node, "forge", newest_forge(minecraft),
                "https://files.minecraftforge.net/net/minecraftforge/forge/", changes, warnings))
            guarded(f"`{node}` `deps.mixinextras`", lambda: check_mixinextras(props, node, changes))

    for change in changes:
        print(f"{change.node or 'all'}: {change.key} {change.old} -> {change.new}")
    for warning in warnings:
        print(f"warning: {warning}", file=sys.stderr)
        if os.environ.get("GITHUB_ACTIONS") == "true":
            print(f"::warning::{warning}")
    if not changes:
        print("Everything is up to date.")

    readme: list[str] = []
    if changes:
        if not args.dry_run:
            props.save()
        readme = update_readme(changes, args.dry_run)
        for swap in readme:
            print(f"README.md: {swap}")
    if args.summary:
        args.summary.parent.mkdir(parents=True, exist_ok=True)
        args.summary.write_text(summary(changes, warnings, readme, loader_changelogs(props, warnings)),
                                encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
