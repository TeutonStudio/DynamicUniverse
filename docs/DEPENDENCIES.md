# Dependency policy

Only these external mod dependencies are declared:

| Dependency | Role | Required |
|---|---|---|
| Minecraft | Game | Yes |
| NeoForge | Loader/API | Yes |
| Kotlin for Forge | Kotlin runtime | Yes |
| Immersive Portals | Seam rendering/traversal adapter | Optional |
| Sable | Physics adapter | Optional |
| Distant Horizons | Horizontal LOD adapter | Optional, no hard artifact dependency |
| Just Zoom | `runClient` zoom control | Development only |
| Konkrete | Required library for Just Zoom | Development only |
| Just Enough Items (JEI) | `runClient` recipe/item viewer | Development only |
| Xaero's Minimap | `runClient` minimap | Development only |

Dynamic Universe does not depend on TerrArchitecture, Galacticraft, DynamicDimensions, Create, or Distant Horizons at compile/runtime by default. Sable is compile-only for optional API adapters; an installation that uses the adapter must provide the Sable mod itself.

The three client quality-of-life mods are Gradle `runtimeOnly` dependencies. They
are available to `runClient`, but are not compile dependencies and are not
included in the published Dynamic Universe artifact.
