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

The client adds a `Universe …` entry point to Minecraft's regular Create World screen. It opens a separate DynamicUniverse selection layer and never replaces or modifies the vanilla tabs, their settings, or their final Create action.

The selection layer exposes Vanilla/other world types, `Universe`, and `Dimension Stack`. The customization control remains available for Vanilla/other types and Universe. For Dimension Stack it is visibly disabled, and an adjacent explanation screen documents that unconstrained edits could create contradictory transitions and damaged world data.

Universe customization is a vertical hierarchy: galaxy → solar system → star and planet. The star has its own settings entry. The planet settings currently edit a validated creation draft: number of dimensions between space and planet core (`0..8`), a power-of-two coordinate transition factor (`1..64`), and core size (`8..128`, in blocks). The draft is held only for the lifetime of its Create World screen. Persisting it into a created save and applying it to generators/portals is deliberately deferred to the server-side creation bridge, so this UI cannot alter an existing world or silently change vanilla generation.

## Horizontal connection topology

Each world layer has a finite square period `L`. X and Z wrap independently, forming a torus. The canonical coordinate is:

```text
canonical(x) = floorMod(x + L/2, L) - L/2
```

Crossing east returns on the west edge; crossing north returns on the south edge. Velocity and facing are preserved. The topology core has no portal or renderer dependency.

Immersive Portals may render the seam as a continuous horizontal connection when present. Distant Horizons may consume the same canonical period for LOD adjacency. Both integrations are optional adapters: no client class or external API is reachable from common/server code.
