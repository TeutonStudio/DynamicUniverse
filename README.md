# GalactiCraft

GalactiCraft is a Kotlin mod for Minecraft 1.21.1 and NeoForge. It models each planet as one or more radial dimension stacks and registers planets dynamically only through privileged server administration.

The default Earth blueprint is:

```text
Planetary core -> deep Nether -> Nether -> Overworld -> sky layer -> cosmos
```

The horizontal scale between layers is data-driven, not hard-coded to `x8`. The first and last layer of every stack are automatically bound to the planet core and the cosmos respectively. Planet creation also enters the new celestial body into the gravity/orbit simulation; survival players cannot create bodies.

See [SPECS.md](SPECS.md), the [Universe world-type plan](docs/UNIVERSE_WORLD_TYPE_PLAN.md), the [portal and Sable transport plan](docs/PORTAL_SABLE_TRANSPORT_PLAN.md), [architecture](docs/ARCHITECTURE.md), [configuration](docs/CONFIGURATION.md), and [dependencies](docs/DEPENDENCIES.md).
