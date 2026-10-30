# platform/

The seam between the mod and everything that differs per node.

- `Loader` is not in this directory: every loader has its own copy, with the same name and the same
  public signatures, in `src/main/<loader>/java/com/backpacksrefined/platform/Loader.java`. The build
  compiles exactly one. Common code calls `Loader.whatever()` without knowing which.
- A difference between Minecraft versions goes behind a class in a `platform` package, written with
  Stonecutter conditionals (`//? if >=1.21.11 {`). `checkVersionSeam` fails a `//?` anywhere else
  outside `mixin/`.
- When a new method is added to one `Loader`, add it to all three.
