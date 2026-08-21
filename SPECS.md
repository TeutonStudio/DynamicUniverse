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

## Horizontal connection topology

Each world layer has a finite square period `L`. X and Z wrap independently, forming a torus. The canonical coordinate is:

```text
canonical(x) = floorMod(x + L/2, L) - L/2
```

Crossing east returns on the west edge; crossing north returns on the south edge. Velocity and facing are preserved. The topology core has no portal or renderer dependency.

Immersive Portals may render the seam as a continuous horizontal connection when present. Distant Horizons may consume the same canonical period for LOD adjacency. Both integrations are optional adapters: no client class or external API is reachable from common/server code.
