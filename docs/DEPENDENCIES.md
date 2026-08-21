# Dependency strategy

| Dependency | Role | Required |
|---|---|---|
| Minecraft 1.21.1 | Game target | Yes |
| NeoForge 21.1.243 | Loader/API | Yes |
| Kotlin for Forge 5.11.0 | Kotlin runtime/loader bridge | Yes |
| DynamicDimensions 0.10.0 | Runtime add/load/remove dimension API | Yes |
| Create: Aeroworks | Compatibility target for the selected 1.21.1 NeoForge ecosystem | No |

DynamicDimensions is used as an external runtime mod rather than copied into this repository. Its NeoForge API supports loading existing dimension data, but the application is responsible for remembering dimensions across restarts; GalactiCraft's planet manifest fulfills that responsibility.

Create: Aeroworks is not a hard dependency. GalactiCraft is built for the same Minecraft/NeoForge baseline and keeps physics integration behind an adapter so a normal server remains launchable without Create, Aeronautics, Aeroworks, or Sable.
