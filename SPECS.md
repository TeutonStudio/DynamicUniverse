# Dynamic Universe specification

## Identity

- Mod id: `dynamicuniverse`
- Kotlin package root: `de.TeutonStudio.DynamicUniverse`
- Target: Minecraft 1.21.1 / NeoForge 21.1.243 / Java 21

## Dynamic cosmos

The server owns every celestial body's mass, radius, position, velocity, and restitution. Pairwise gravity is evaluated on each simulation tick. Sphere collisions use a normal impulse, like billiard balls; the material restitution decides how much relative speed survives the impact.

Planets are created only by administrative server actions. Survival gameplay cannot create celestial bodies, because an added mass changes all current gravitational accelerations and orbital trajectories.

## Universe world type foundation

`DYNAMIC_UNIVERSE` is a server-side world-type configuration. It represents a vertical hierarchy of galaxies, celestial groups, optional stars, planets, and radial dimension stacks. A celestial group is either a solar system with exactly one star or a cloud with no star.

Each planet owns one or more core-to-surface stacks. The innermost planet-core layer and the outermost sky layer are mandatory. Every radial boundary, including the final sky-to-Universe boundary, has an explicit positive rational coordinate factor. The technical connection graph exposes both directed routes for every boundary, while actual portal rendering and entity transfer remain optional adapters.

Planet-core size is a separate, positive configuration value. It does not silently alter a celestial body's collision radius.

## World-creation UI

`dynamicuniverse:universe` is a data-driven Minecraft `WorldPreset`. It is added to the vanilla `#minecraft:normal` preset tag, so it appears in the existing World Type selector alongside Standard, Flat, and Single Biome. Its unedited Terra default installs the core, deep-Nether, sky, star, and universe level stems before server startup; the Terra surface and Nether retain the vanilla level keys for normal spawning and Nether travel. Planet cores and sky/universe use void generation, with a generated Bedrock cube shell in each planet core. A customized draft replaces these stems during the same pre-server creation phase and never alters a pre-existing save.

NeoForge's client-only `RegisterPresetEditorsEvent` binds the existing vanilla `Customize` button to the Universe editor when, and only when, the Universe preset is selected. The editor uses one vertical, Flat-World-style expandable tree list: galaxies and solar systems expand in place; a galaxy contains solar systems and clouds; an expanded solar system exposes a separate Sun settings row and its planets. A solar system always has one Sun and one or more planets; a galaxy may contain both solar systems and clouds. Opening a planet, Sun, or cloud setting returns directly to the preserved tree rather than through intermediate hierarchy screens.

The planet settings currently edit a validated creation draft: number of dimensions between space and planet core (`0..8`), an integer denominator for the coordinate transition factor (`1/4..1/64`, adjustable one integer at a time), and core size (`8..128`, in blocks). The draft is held only for the lifetime of its Create World screen. A server-side creation adapter hands its frozen `UniverseWorldType` to `UniverseWorldCreationBridge` only after every local level has been registered and the generator adapters have reported their Bedrock planes. The bridge writes one versioned `SavedData` record per save and restores the runtime graph only after all levels are available. The client editor never owns the persisted state and cannot alter an existing world or silently change vanilla generation.

When Immersive Portals is present and Universe is selected, its `Dimension Stack` button in the More tab is visibly disabled with an explanation. DynamicUniverse also clears an already-pending Dimension Stack selection through a client-only optional compatibility adapter, so a user cannot first confirm a stack and then create an incompatible Universe world. A dedicated-server Dimension Stack preset remains outside the client world-creation UI and must be disabled in that server's Immersive Portals configuration.

## Horizontal connection topology

Each world layer has a finite square period `L`. X and Z wrap independently, forming a torus. The canonical coordinate is:

```text
canonical(x) = floorMod(x + L/2, L) - L/2
```

Crossing east returns on the west edge; crossing north returns on the south edge. Velocity and facing are preserved. The topology core has no portal or renderer dependency.

Immersive Portals may render the seam as a continuous horizontal connection when present. Distant Horizons may consume the same canonical period for LOD adjacency. Both integrations are optional adapters: no client class or external API is reachable from common/server code.
