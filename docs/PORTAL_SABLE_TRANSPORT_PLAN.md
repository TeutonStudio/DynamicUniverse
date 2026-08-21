# Portal, planet-core, Universe, and Sable transport plan

## Decision summary

Planet-core worlds, intermediate radial layers, planet surfaces, and the Universe are **dynamic Minecraft dimensions**. Immersive Portals connects those dimensions and supplies the seamless visual and traversal layer. Sable sub-levels are not planet worlds: they are optional, moving local structures inside one level and are reserved for ships, stations, and other vehicles.

```text
DynamicDimensions:   creates and reloads persistent ServerLevels
GalactiCraft:        owns topology, celestial state, transforms, gravity, and manifests
Immersive Portals:   renders and traverses declared links between loaded levels
Sable:               simulates optional movable structures inside one loaded level
```

No integration may make Immersive Portals the authority for dynamic dimension ownership or make Sable the authority for a planet's radial topology.

## World model

### Universe

The Universe is one global dynamic dimension with an inertial `UniverseFrame`. It represents interplanetary space, not a visual backdrop. Its baseline gameplay physics is vacuum: zero static gravity, zero pressure, and zero universal drag. GalactiCraft supplies the actual, time-varying gravitational field from celestial bodies.

Ships and players in the Universe are test objects by default. They do not register as `CelestialBody` instances and therefore cannot perturb planetary orbits. A separate, opt-in large-vessel model would be needed before a vehicle may affect the N-body simulation.

### Planet and radial layers

Every planet has a local `PlanetFrame` and one or more `DimensionStack`s. The ordered layers are separate `ServerLevel`s:

```text
planet core -> inner layers -> surface/sky -> Universe
```

The core and Universe are fixed endpoints; intermediate layers are configurable. A core world is a normal block/chunk/entity world, not a miniature spherical sub-level. `planetCoreSize` initially means the configured radial core-boundary size. It is stored independently from `CelestialBody.radius`; a later physics rule must explicitly constrain their relationship rather than silently replacing collision radius.

Each surface/layer uses planet-local coordinates and derives its local "down" from its `PlanetFrame`. The outer Universe link maps between the moving planet frame and the inertial Universe frame. Body motion changes that outer transform but does not move or regenerate the planet's local chunks.

## Portal graph and lifecycle

### Ownership and creation

1. GalactiCraft validates the complete hierarchy, all dynamic-dimension IDs, factors, and celestial placement.
2. DynamicDimensions creates or loads every needed level.
3. The portal bridge waits until each target `ServerLevel` is available.
4. It creates a deterministic pair of Immersive Portal entities for every directed radial boundary and then enables traversal.
5. Only then is the manifest marked complete.

The logical `PortalLinkId` is persisted. Raw portal entity UUIDs are cache data only: after restart or repair, portal entities are recreated from the logical graph. The bridge must remove only newly created, empty levels and links on a failed creation transaction; it must never remove a previously persisted player world as rollback.

### Link invariants

- Each link has a source layer, target layer, explicit scale, translation, rotation, and inverse link.
- The scale is the same positive rational factor already validated by `DimensionStack`; no default `8:1` or implicit inverse is allowed.
- Rotation is represented as a quaternion, not independent yaw/pitch values.
- Target chunks are available before a player can cross. Client-side remote chunk loading is released after the player no longer views the link.
- GalactiCraft alone owns its horizontal wrapping and radial-link graph. Immersive Portals' generic dimension-stack UI and world-wrapping features must not reconfigure GalactiCraft layers.

## Frame, velocity, and scale contract

For a layer-to-layer link with scale `s`, rotation `R`, and translation `t`:

```text
pTarget = t + R(s * pSource)
vTarget =     R(s * vSource)
aTarget =     R(s * aSource)
```

For the moving outer link, local and inertial frame velocity are also related to the parent body's velocity. With planet rotation deliberately disabled in the first slice:

```text
vUniverse = bodyVelocity + R(s * vPlanetLocal)
vPlanetLocal = R^-1(vUniverse - bodyVelocity) / s
```

Do not rely only on Immersive Portals' coordinate transform for this. The portal traversal adapter must correct entity velocity, orientation, passenger state, and pending gravity at the same authoritative server transition. Planetary rotation, if introduced later, adds the angular `omega x r` term and requires the same correction for accelerations.

## Gravity, atmosphere, and collisions

`DynamicCosmos` currently accelerates celestial bodies only. Add a read-only `EnvironmentField` that evaluates bodies at a global position for players, loose entities, and supported vehicles. It converts the resulting acceleration to the active layer's coordinate frame and units.

Sable dimension-physics data supplies a *static* base gravity, pressure, drag, and magnetic field. It cannot be the source for moving N-body gravity. The integration rules are:

| Location | Sable baseline | GalactiCraft contribution |
| --- | --- | --- |
| Universe | gravity `0`, pressure `0`, drag `0` | complete dynamic cosmic field |
| planet-local layer | static local baseline only when deliberately configured | perturbations and any dynamic field, never a duplicate base value |
| Sable vehicle | Sable vehicle physics | same environment field in global coordinates, transformed to its pose |

Collision resolution remains celestial-body-to-celestial-body logic. An ordinary ship or player never becomes a collision sphere. A planet collision must trigger a defined cosmology event before its portal frame and surface state can change; otherwise the physical body state and its linked worlds diverge.

## Sable transport boundary

Sable sub-levels contain normal chunks, entities, and block entities but have a dynamic pose within a level. Their plot coordinates are not global coordinates. Any GalactiCraft distance, gravity, portal-target, or collision calculation involving a sub-level must use Sable Companion projection utilities.

### Supported first slice

- Players, mobs, and items not tracking a sub-level may traverse a GalactiCraft portal.
- A ship can remain and operate inside one planet layer or in the Universe.
- A portal surface detects a tracking Sable sub-level and denies direct entry with a clear message; docking or dismounting is required.

### Experimental later slice: ship transfer

Never assume that a Sable sub-level can be moved to another `ServerLevel` by a normal entity teleport. Implement an explicit, recoverable transfer transaction:

1. stop physics and lock the ship against assembly/splitting;
2. capture the logical pose, linear/angular velocity, passengers, contained entities, block entities, constraints, and ownership;
3. project the source pose to global coordinates and apply the portal/frame transform;
4. create and validate a target sub-level in the destination level;
5. restore ship state and riders relative to its new pose; apply transformed linear/angular velocity and the target environment field;
6. commit ownership and only then remove the source sub-level.

If any stage fails, unlock and retain the source ship. This adapter requires a version-pinned direct Sable integration. Sable Companion alone is sufficient for safe optional position compatibility, but not for creating or relocating active sub-levels.

## Compatibility gates

Immersive Portals and Sable are both intrusive mods. Their combination has a reported crash/incompatibility history, so ship transfer remains disabled until the exact supported version set passes the tests below. GalactiCraft must start and retain ordinary radial-dimension functionality when neither mod, only Immersive Portals, or only Sable is present.

Before enabling a version set:

1. start a dedicated server and client with the exact NeoForge, Immersive Portals, Sable, and Sable Companion versions;
2. test normal entity crossing in both directions for every layer scale;
3. test entity tracking, dismount, relog, death, portal cancellation, and chunk unload while on a Sable ship;
4. test target-world delayed loading, server restart, and deterministic portal reconstruction;
5. profile recursive portal rendering, remote chunk tickets, and many visible radial links;
6. stress an experimental ship transfer with passengers, inventories, block entities, constraints, and failure injection.

## Delivery sequence

1. Introduce pure common/server data: `UniverseFrame`, `PlanetFrame`, `PortalLink`, `EnvironmentField`, and versioned manifest records. Test transforms and scale/velocity round trips.
2. Add DynamicDimensions orchestration and transactional portal-graph lifecycle without either optional mod loaded.
3. Add an Immersive Portals-only bridge behind a client/server optional-mod boundary. Ship code must not be referenced here.
4. Add Sable Companion as the soft compatibility layer for projection, distance checks, and tracking detection.
5. Enable ordinary-entity portal travel after the compatibility suite passes.
6. Build the direct Sable ship-transfer adapter only behind an experimental server setting and a strict tested-version gate.

## Acceptance criteria

- Existing GalactiCraft templates and manifests remain unchanged and load without Immersive Portals or Sable.
- A radial boundary has an inverse portal pair and preserves the declared scale, velocity direction, and endpoint topology.
- A player cannot cross into an unavailable target level or leave a partially created portal graph.
- Universe gravity is applied once, in a global frame; Sable static physics never duplicates it.
- A normal entity can enter and leave a planet without energy or coordinate discontinuities caused by the moving planet frame.
- A tracking Sable ship cannot silently duplicate, lose riders, or cross dimensions through an ordinary entity portal.
- Failed experimental ship transfer leaves the source ship intact.

## Research references

- [Immersive Portals API](https://qouteall.fun/immptl/wiki/API-for-Other-Mods.html) — portal transforms, remote chunk loading, and multi-world client support.
- [Immersive Portals NeoForge releases](https://github.com/iPortalTeam/ImmersivePortalsModForNeo/releases) — version baseline for Minecraft 1.21.1.
- [Sable overview](https://github.com/ryanhcode/sable) — sub-level model and compatibility warning.
- [Sable Companion](https://github.com/ryanhcode/sable-companion) — safe projection and distance utilities.
- [Sable dimension physics](https://github.com/ryanhcode/sable/wiki/Dimension-Physics-Data) and [entity tracking](https://github.com/ryanhcode/sable/wiki/Working-With-Entities).
- [Reported Sable/Immersive Portals incompatibility](https://github.com/ryanhcode/sable/issues/155).
