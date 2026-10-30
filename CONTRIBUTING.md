# Contributing to Backpacks Refined

Bug reports, translations, ideas and code are all welcome.

## Reporting a bug

Open an [issue](https://github.com/whoisnighter/BackpacksRefined/issues/new/choose) with the bug report
form. Give the full jar name (for example `BackpacksRefined-0.1.0+26.3-fabric.jar`), the other mods
installed, the steps that cause the problem, and the log or crash report.

## Translations

Language files are in `src/main/resources/assets/backpacksrefined/lang/`. To add a language, copy
`en_us.json`, rename it to the game's locale code, and translate the values only. `checkLang` fails
when a file lacks a key `en_us.json` has.

## Code

1. Fork the repository and branch from `main`.
2. Read [AGENTS.md](AGENTS.md) for the layout and the rules the build checks.
3. Build and check at least one node per loader you touched:

   ```bash
   ./gradlew :26.3-fabric:check :26.3-neoforge:check :1.20.1-forge:check
   ```

4. Reset the active node before committing: `./gradlew "Reset active project"`.
