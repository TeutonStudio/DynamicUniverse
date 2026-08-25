# Dynamic Universe specification

## Identity

- Mod id: `dynamicuniverse`
- Kotlin package root: `de.TeutonStudio.DynamicUniverse`
- Target: Minecraft 1.21.1 / NeoForge 21.1.243 / Java 21

## Dynamic cosmos

The server owns every celestial body's mass, radius, position, velocity, and restitution. Pairwise gravity is evaluated on each simulation tick. Sphere collisions use a normal impulse, like billiard balls; the material restitution decides how much relative speed survives the impact.

Planets are created only by administrative server actions. Survival gameplay cannot create celestial bodies, because an added mass changes all current gravitational accelerations and orbital trajectories.

## All / UniverseSpace runtime

`All` is only an invisible Minecraft attachment required while vanilla entities still need a `Level`. It is not a Universe dimension and must never be presented as one: `UniverseSpace` is the authoritative continuous, unbounded, directionless R³ zone. It has no Minecraft world border, vertical limit, orbital plane, global up, horizontal seam, or End-style vertical cycle.

`UniverseSpatialObject` is the shared spatial basis; `CelestialSpatialObject` adds mass, radius and restitution for `DynamicCosmos`. Position, velocity and object-local orientation are held in `UniverseKinematicState`. `UniverseGeometryManifest` holds immutable dimension-stack and planet-local-space identity only. Moving planet anchors, spatial objects, bubble layout and optional compatibility reservations belong to the independently versioned `UniverseRuntimeState` persisted by `UniverseSaveData` and restored by `UniverseWorldCreationBridge`.

The technical `UniverseHost` may keep several `SimulationBubble`s in named slots. A rebase changes bubble-local coordinates only and leaves global positions invariant. Membership is explicit, remote bubbles stay independent, and intersecting bubbles are merely merge candidates for a later hand-off implementation. A reserved `SablePlotyardReservation` prevents a normal host bubble from occupying Sable's plotyard area.

The finite attachment is rebased before any native host bound is reached. A rebase translates the local attachment frame and preserves the global X/Y/Z position; it is not a modulo wrap and therefore never changes R³ topology. The client hides attachment blocks and renders celestial bodies as geometric sphere meshes. Every visible sphere point samples a vertical, cached view of the body's outermost toroidal layer through a declared atlas; it is not a curved Immersive-Portal surface.

Sable 2.0.3 is optional. `compat.sable.SableUniverseBridge` contains no direct Sable API reference, so an existing universe save restores without Sable. A Sable-side adapter can bind a sublevel to a spatial object and projects its local pose through that object before performing UniverseSpace work. Sable is supplied as `runtimeOnly` to `runClient`; final portal shells, sphere rendering and physical sublevel transfer are intentionally out of scope.

## Vertical dimension seams

The End is an isolated vertical loop. UniverseSpace is not: its invisible host uses continuous Bubble rebasing before a native build-height bound is reached, preserving the global R³ position rather than re-entering on an opposite side.

`VerticalDimensionSeam` models cross-level air seams separately from radial planet stacks. The built-in optional seam maps the top of `minecraft:overworld` to the bottom of `aether:the_aether`; falling through the Aether bottom returns to the Overworld top. It becomes active only when the Aether level exists, preserving normal worlds without Aether. With Immersive Portals installed, the Overworld ceiling faces downward and is rendered from below, while the Aether floor faces upward and is rendered from above. Each target is placed four blocks inside its legal build range, never exactly on the Aether lower bound. The optional Aether compatibility hook suppresses Aether's own travel-screen packets for this IP-owned seam; ordinary Aether worlds remain unchanged. Without Immersive Portals, the server-side transition listener retains equivalent topology as a compatibility fallback. External Aether stems remain supplied by Aether itself and are never replaced with cloned vanilla worldgen.

## Universe world type foundation

`DYNAMIC_UNIVERSE` is a server-side world-type configuration. It represents a vertical hierarchy of galaxies, celestial groups, optional stars, planets, and radial dimension stacks. A celestial group is either a solar system with exactly one star or a cloud with no star.

Each planet owns one or more core-to-surface stacks. The innermost planet-core layer and the outermost sky layer are mandatory. Every radial boundary, including the final sky-to-Universe boundary, has an explicit positive rational coordinate factor. The technical connection graph exposes both directed routes for every boundary, while actual portal rendering and entity transfer remain optional adapters.

Planet-core size is a separate, positive configuration value. It does not silently alter a celestial body's collision radius.

## World-creation UI

`dynamicuniverse:universe` is a data-driven Minecraft `WorldPreset`. It is added to the vanilla `#minecraft:normal` preset tag, so it appears in the existing World Type selector alongside Standard, Flat, and Single Biome. Its unedited Terra default installs the core, deep-Nether, sky, star, and universe level stems before server startup; the Terra surface and Nether retain the vanilla level keys for normal spawning and Nether travel. Planet cores and sky use void generation, with a generated Bedrock cube shell in each planet core. The All stem uses the dedicated `dynamicuniverse:universe_space` dimension type and void generator as a technical host; this does not remove Minecraft's finite internal build range, which floating-origin bubbles hide from UniverseSpace consumers. A customized draft replaces these stems during the same pre-server creation phase and never alters a pre-existing save.

NeoForge's client-only `RegisterPresetEditorsEvent` binds the existing vanilla `Customize` button to the Universe editor when, and only when, the Universe preset is selected. The editor uses one vertical, Flat-World-style expandable tree list: galaxies and solar systems expand in place; a galaxy contains solar systems and clouds; an expanded solar system exposes a separate Sun settings row and its planets. A solar system always has one Sun and one or more planets; a galaxy may contain both solar systems and clouds. Opening a planet, Sun, or cloud setting returns directly to the preserved tree rather than through intermediate hierarchy screens.

The planet settings currently edit a validated creation draft: number of dimensions between space and planet core (`0..8`), an integer denominator for the coordinate transition factor (`1/4..1/64`, adjustable one integer at a time), and core size (`8..128`, in blocks). The draft is held only for the lifetime of its Create World screen. A server-side creation adapter hands its frozen `UniverseWorldType` to `UniverseWorldCreationBridge` only after every local level has been registered and the generator adapters have reported their Bedrock planes. The bridge writes one versioned `SavedData` record per save: static world geometry and a separately versioned moving R³ runtime state. The client editor never owns the persisted state and cannot alter an existing world or silently change vanilla generation.

When Immersive Portals is present and Universe is selected, its `Dimension Stack` button in the More tab is visibly disabled with an explanation. DynamicUniverse also clears an already-pending Dimension Stack selection through a client-only optional compatibility adapter, so a user cannot first confirm a stack and then create an incompatible Universe world. A dedicated-server Dimension Stack preset remains outside the client world-creation UI and must be disabled in that server's Immersive Portals configuration.

## Horizontal connection topology

Each world layer has a finite square period `L`. X and Z wrap independently, forming a torus. The canonical coordinate is:

```text
canonical(x) = floorMod(x + L/2, L) - L/2
```

Crossing east returns on the west edge; crossing north returns on the south edge. Velocity and facing are preserved. The topology core has no portal or renderer dependency.

Immersive Portals may render the seam as a continuous horizontal connection when present. Distant Horizons may consume the same canonical period for LOD adjacency. Both integrations are optional adapters: no client class or external API is reachable from common/server code.

## Distant Horizons portal rendering

Distant Horizons remains optional. During a normal world render, it renders only the player's
current level. During an Immersive-Portals sub-render, DynamicUniverse derives a client-only
`PortalLodRenderScope` from IP's active target dimension and recursion layer; the physical player
is deliberately not used as the target-world identity. On DH's native OpenGL renderer with an
active IP stencil target, the optional adapter binds DH to that target so terrain LODs are clipped
by the portal aperture. DH engines without that framebuffer capability, absent IP, missing stencil
state, or an unknown foreign target retain safe vanilla target chunks. The fallback is visual only:
it never changes portal traversal, chunk ownership, saved data, or server behaviour.
