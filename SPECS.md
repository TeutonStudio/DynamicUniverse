# GalactiCraft specification

## Product boundary

GalactiCraft is a clean Kotlin/NeoForge implementation for Minecraft 1.21.1. Its Java/Kotlin package root is exactly `de.TeutonStudio.GalactiCraft`; its valid lowercase NeoForge mod id is `galacticraft`.

It does not fork the legacy Forge Galacticraft source tree or the in-progress Fabric Galacticraft 5 implementation. It consumes the MIT-licensed NeoForge artifact of DynamicDimensions as a runtime dependency.

## Planet world type

`GALACTICRAFT_PLANETARY` is the world type. A planet is a celestial body plus one or more named radial `DimensionStack`s. Each stack has at least two layers and has these mandatory endpoint invariants:

- its innermost layer is connected to the owning planet's core;
- its outermost layer is connected to the cosmos;
- intermediate layers are connected only to their immediate neighbours;
- every adjacent coordinate scale is an explicit positive rational ratio.

The default Earth template includes a five-layer main stack: core, deep Nether, Nether, Overworld, and sky. `8:1` is a default ratio only; template authors may choose another ratio per boundary.

## Universe world type (planned)

`GALACTICRAFT_UNIVERSE` will be a separate world type and configuration flow. It must not reinterpret or migrate existing `GALACTICRAFT_PLANETARY` templates. Its editable draft is a vertical hierarchy:

```text
Universe
  Galaxy
    Celestial group (solar system or cloud)
      Star (solar-system groups only)
      Planet
        fixed universe endpoint <- configurable radial dimensions <- fixed planet-core endpoint
```

The neutral `celestial group` node deliberately supports both requested alternatives: `SOLAR_SYSTEM` has one configurable star and zero or more planets; `CLOUD` has zero or more planets and no star. A group kind may not be changed in place when that would discard a configured star; the editor must ask for an explicit, loss-aware conversion.

Each planet exposes a planet settings action and an ordered dimension editor. The editor may add, remove, and reorder only intermediate dimensions; the universe and planet-core endpoints remain mandatory. Every boundary between adjacent dimensions has an explicit positive rational dimension-change factor. A planet also has an independently stored planet-core size. It must not silently overwrite the body's physical collision radius; a later physics contract must state whether and how those values are related.

The customization action is a capability of the selected world type, not a global toggle:

- `GALACTICRAFT_UNIVERSE`: enabled and opens the Universe hierarchy editor.
- Dimension Stack / “Mehr”: shown disabled, with a keyboard-accessible explanatory dialog or tooltip. This avoids opening an unsafe editor for a topology that can be corrupted by it.
- every other world type: enabled again and delegated to that world's existing customization flow without state leaking from the Dimension Stack selection.

The present codebase has no client world-creation screen or world-type-selection adapter. Therefore this requirement is specified as an integration boundary, not implemented by changing the server-side domain model. The detailed UI, draft, validation, and rollout plan is in [docs/UNIVERSE_WORLD_TYPE_PLAN.md](docs/UNIVERSE_WORLD_TYPE_PLAN.md).

## Dynamic planet lifecycle

Only an operator command or an equivalent server-side administrative API may spawn a planet. The action is excluded from survival because it changes the global gravity field and can perturb existing orbits. Spawn is transactional at the domain level: validate the body, all stacks, dimension ids, and cosmological placement before DynamicDimensions receives any creation request.

DynamicDimensions loads existing world data on restart but deliberately does not track which dimensions belong to an application. GalactiCraft therefore persists the complete planet manifest, including body positions and velocities, and reloads every registered stack during server startup. Deletion is intentionally not exposed by the first command surface because dimension deletion permanently removes world data.

## Dynamic cosmos and collision model

The cosmos simulation is server authoritative and deterministic for a world seed plus tick. It evaluates gravity from all registered bodies and stores position, velocity, mass, radius, and collision material per body. A collision is resolved as a sphere-sphere billiard impulse along the contact normal. The coefficient of restitution is configurable per collision material; `1.0` is perfectly elastic, and the default is below one to avoid unbounded numerical energy.

The first release treats a collision as a physical impulse. Fragmentation, terrain deformation, and player damage are deliberately separate future systems.

## Non-goals of the first vertical slice

- client-side rendering of a full orbital map or visible spherical planets;
- survival craft recipes for planet creation;
- automatic deletion or migration of spawned stacks;
- a second portal engine. Traversal and rendering adapters are separate from stack topology.
