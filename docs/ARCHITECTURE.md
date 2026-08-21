# Architecture

GalactiCraft keeps the three required concepts separate:

```text
DynamicCosmos       owns bodies, gravity, orbital state, and collisions
PlanetWorldType     owns planet identity and administrative lifecycle
DimensionStack      owns layers, adjacent scales, and endpoint topology
DynamicDimensions   creates/loads the corresponding NeoForge ServerLevels
```

`DimensionStack` never calculates a solar orbit, and `DynamicCosmos` never decides which layer is a Nether. This boundary prevents diverging spatial models.

## Runtime sequence

1. The server loads the persistent planet manifest.
2. The manifest validates every stack endpoint and scale.
3. Each saved layer is loaded with `DynamicDimensionRegistry.loadDynamicDimension` using its configured generation template. `PlanetManifestData` is a NeoForge `SavedData` record, so DynamicDimensions' intentional lack of application-level tracking does not lose planets on restart.
4. The cosmos registers the stored celestial body.
5. An operator can spawn a validated template. Its layers use stable dimension ids under `galacticraft:planets/<planet>/<stack>/<layer>`.

The DynamicDimensions call may register a level on the following tick. Teleport/portal code must therefore wait for the level availability callback rather than assuming immediate access.

## TerrArchitecture concept alignment

The architecture adopts the useful separation from TerrArchitecture's DynamicGlobus/DimensionStack work: radial stack geometry, global cosmic state, and traversal/rendering are separate authorities. Unlike its fixed Earth-oriented assumptions, GalactiCraft exposes scale ratios and stack count as planet-template data and adds a multi-body collision model.
