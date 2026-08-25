# Dependency policy

Only these external mod dependencies are declared:

| Dependency | Role | Required |
|---|---|---|
| Minecraft | Game | Yes |
| NeoForge | Loader/API | Yes |
| Kotlin for Forge | Kotlin runtime | Yes |
| Immersive Portals | Seam rendering/traversal adapter | Optional |
| Sable | Physics adapter | Optional |
| Distant Horizons | Horizontal and IP-aperture-clipped LOD adapter on native OpenGL | Optional, no hard artifact dependency |
| Just Zoom | `runClient` zoom control | Development only |
| Konkrete | Required library for Just Zoom | Development only |
| Just Enough Items (JEI) | `runClient` recipe/item viewer | Development only |
| Xaero's Minimap | `runClient` minimap | Development only |
| Jade | Targeted block/entity inspection and JEI integration | Development only |
| NBTviewer | Item and looked-at entity SNBT inspection | Development only |
| spark | Tick, heap, and CPU profiler (`/spark`) | Development only |
| BetterF3 | Configurable dimension/chunk/runtime debug HUD | Development only |

Dynamic Universe does not depend on TerrArchitecture, Galacticraft, DynamicDimensions, Create, or Distant Horizons at compile/runtime by default. Sable is compile-only for optional API adapters; an installation that uses the adapter must provide the Sable mod itself.

When both Distant Horizons and Immersive Portals are installed, portal-local terrain LODs are
enabled only when DH exposes its native OpenGL framebuffer override and IP has an active stencil
target. Other DH rendering engines deliberately fall back to normal target chunks inside portals;
this prevents a full-screen LOD composite from escaping the portal aperture.

The development quality-of-life and diagnostics mods are Gradle `runtimeOnly`
dependencies. They are available to `runClient`, but are not compile dependencies
and are not included in the published Dynamic Universe artifact. Their Modrinth
version IDs pin NeoForge 1.21.1 releases; Gradle downloads them into the development
runtime instead of bundling their JARs into this repository.

`runModpackClient` is not defined in this standalone repository. A consuming
modpack may add these same runtime-only artifacts to its own development profile.
