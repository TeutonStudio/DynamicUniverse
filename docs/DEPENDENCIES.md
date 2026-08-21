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

Dynamic Universe does not depend on TerrArchitecture, Galacticraft, DynamicDimensions, Create, or Distant Horizons at compile/runtime by default. Sable is compile-only for optional API adapters; an installation that uses the adapter must provide the Sable mod itself.
