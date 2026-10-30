<div align="center">

<img src="src/main/resources/assets/backpacksrefined/icon.png" alt="Backpacks Refined logo" width="180">

# Backpacks Refined

A simple, immersive backpack mod for **Fabric**, **NeoForge** and **Forge**.

</div>

## Requirements

| Minecraft | Java | Fabric Loader (min.) | Fabric API | NeoForge / Forge |
| :--- | :---: | :--- | :--- | :--- |
| 26.3 | 25 | 0.19.5 | 0.165.0+26.3 | 26.3.0.83 |
| 26.2 | 25 | 0.19.5 | 0.161.0+26.2 | – |
| 1.21.11 | 21 | 0.19.5 | 0.141.6+1.21.11 | – |
| 1.21.1 | 21 | – | – | 21.1.262 |
| 1.20.1 | 17 | – | – | Forge 47.4.28 |

Versions listed are minimums, except Forge, where any 47 build works. Install on the server **and** every client. Each jar names its loader: `-fabric`, `-neoforge` or `-forge`.

## Building
One source tree built for every version with [Stonecutter](https://stonecutter.kikugie.dev). Gradle runs on Java 25 and downloads the Java version each node needs.

```bash
git clone https://github.com/whoisnighter/BackpacksRefined.git
cd BackpacksRefined
./gradlew buildAll
```

Jars land in `build/libs/`, one per version and loader. Nodes are named `<minecraft>-<loader>`: `26.3-fabric`, `26.3-neoforge`, `26.2-fabric`, `1.21.11-fabric`, `1.21.1-neoforge` and `1.20.1-forge`.

| Task | Command |
| :--- | :--- |
| Build one node | `./gradlew ":26.3-fabric:buildAndCollect"` |
| Checks | `./gradlew ":26.3-fabric:check"` |
| Dev client / server | `./gradlew ":26.3-fabric:runClient"` / `":26.3-fabric:runServer"` |
| Active node's client | `./gradlew runActiveClient` |
| Switch the active node | `./gradlew "Set active project to 1.21.1-neoforge"`, then `"Reset active project"` before committing |
| Publish to Modrinth and CurseForge | `./gradlew publishAll` (a dry run without the tokens in `.env`) |

Contributions are welcome, see [CONTRIBUTING.md](CONTRIBUTING.md). Translation PRs are welcome too.

## Credits
* Based on [Backpacks for Dummies](https://modrinth.com/mod/backpacks-for-dummies) by [Spydnel](https://github.com/Spydnel), under the MIT License.
* Licensed under the [MIT License](LICENSE). Full credits in [CREDITS.md](CREDITS.md).
