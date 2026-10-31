"""Run agent client scripts on one node, unattended, as the agent workflow (agent.yml) does.

    python .github/scripts/run_agent.py <node> <script.jsonl>... [--summary <file>] [--captures <dir>]

For each script it does what the script's header says a person would: make the world it names with
`tools/agent/new_world.py` (the `-Pquickplay=<World>` in its runClient line), write the config a
`printf '<json>' > run/<node>/config/<file>` line asks for, run the client with `-Pagent`, read the
DONE line, and `drive.py --verify` the answers. The config is put back as it was afterwards.

A node with no world yet, as in a fresh CI checkout, first runs `tools/agent/ci/test-world.jsonl`,
which makes one for new_world.py to take its settings from.

Each script's captures are moved to `<captures>/<node>/<script>/`, so the next script's do not mix
with them. `--summary` appends a Markdown table of the results (GITHUB_STEP_SUMMARY in CI). The exit
code is 1 when any script did not finish or did not verify.

It runs whatever display it is given: in CI, under `xvfb-run`.
"""

import argparse
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BOOTSTRAP = ROOT / "tools/agent/ci/test-world.jsonl"
GRADLE = str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))
QUICKPLAY = re.compile(r"-Pquickplay=(\S+)")
CONFIG = re.compile(r"""^//\s+printf '(.*)' > run/<node>/(config/\S+)\s*$""")
DONE = re.compile(r"\[BackpacksAgent\] (DONE .*?)(?:, out=.*)?$")


def header(script):
    return [line for line in script.read_text(encoding="utf-8").splitlines() if line.startswith("//")]


def run(node, script, captures):
    """Runs one script; returns (done line, verified)."""
    lines = header(script)
    world = next((m.group(1) for line in lines if (m := QUICKPLAY.search(line))), None)
    configs = [(ROOT / "run" / node / m.group(2), m.group(1)) for line in lines if (m := CONFIG.match(line))]

    if world:
        subprocess.run([sys.executable, str(ROOT / "tools/agent/new_world.py"), node, world], cwd=ROOT, check=True)
    saved = {}
    for path, content in configs:
        saved[path] = path.read_bytes() if path.exists() else None
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    relative = script.resolve().relative_to(ROOT).as_posix()
    command = [GRADLE, f":{node}:runClient", f"-Pagent={relative}", "--console=plain"]
    if world:
        command.append(f"-Pquickplay={world}")
    log = captures / node / f"{script.stem}.gradle.log"
    log.parent.mkdir(parents=True, exist_ok=True)
    try:
        with log.open("w", encoding="utf-8") as out:
            # The task exits 0 whatever the game did; the DONE line and the verify say how it went.
            subprocess.run(command, cwd=ROOT, stdout=out, stderr=subprocess.STDOUT)
    finally:
        for path, content in saved.items():
            if content is None:
                path.unlink(missing_ok=True)
            else:
                path.write_bytes(content)

    latest = ROOT / "run" / node / "logs" / "latest.log"
    done = "no DONE line"
    if latest.exists():
        for line in latest.read_text(encoding="utf-8", errors="replace").splitlines():
            if m := DONE.search(line):
                done = m.group(1)
        shutil.copy(latest, captures / node / f"{script.stem}.latest.log")

    queue = ROOT / "run" / node / "agent" / "client"
    verified = subprocess.run([sys.executable, str(ROOT / "tools/agent/drive.py"), str(queue), str(script),
                               "--verify", "--max-age", "30"], cwd=ROOT).returncode == 0

    screenshots = queue / "screenshots"
    if screenshots.is_dir():
        target = captures / node / script.stem
        shutil.rmtree(target, ignore_errors=True)
        shutil.move(str(screenshots), str(target))
    return done, verified


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("node")
    parser.add_argument("scripts", nargs="+", type=Path)
    parser.add_argument("--summary", type=Path)
    parser.add_argument("--captures", type=Path, default=ROOT / "build" / "agent-ci")
    arguments = parser.parse_args()
    captures = arguments.captures.resolve()

    results = []
    saves = ROOT / "run" / arguments.node / "saves"
    if not any(saves.glob("*/level.dat")):
        print(f"No world in {saves}: making one with {BOOTSTRAP.name}", flush=True)
        results.append((BOOTSTRAP, *run(arguments.node, BOOTSTRAP, captures)))

    if all(verified for _, _, verified in results):
        for script in arguments.scripts:
            print(f"::group::{arguments.node} {script.name}", flush=True)
            results.append((script, *run(arguments.node, script, captures)))
            print("::endgroup::", flush=True)

    failed = [script for script, done, verified in results if not (verified and done.startswith("DONE script answered"))]
    table = [f"### Agent client on {arguments.node}", "", "| Script | Run | Verify |", "|---|---|---|"]
    for script, done, verified in results:
        table.append(f"| `{script.relative_to(ROOT).as_posix() if script.is_absolute() else script.as_posix()}` "
                     f"| {done} | {'passed' if verified else '**failed**'} |")
    table.append("")
    print("\n".join(table))
    if arguments.summary:
        with arguments.summary.open("a", encoding="utf-8") as out:
            out.write("\n".join(table) + "\n")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
