# Universe globe projection

`UniverseSpace` is a complete, authoritative continuous R³ zone; Minecraft's `All`
level is only an invisible finite attachment for rebased simulation bubbles. It has no vertical
cycle. A celestial globe is a client-side geometric sphere whose surface is a
presentation of the **outermost local T² layer** of a body. It is not an Immersive-Portals
surface and never owns collision, terrain or traversal authority.

## Atlas contract

The default `SIX_CHART` atlas maps the six faces of a display cube to a 3×2 partition of the
source torus. This covers the finite source period exactly once, but face boundaries are declared
visual seams: a torus cannot be continuously and bijectively represented by a sphere. Renderers
must use `GlobeSample.seamWeight` to blend, mask or cloud-cover those boundaries.

`EQUIRECTANGULAR_DEBUG` exists solely for diagnostics. Its `polarSingularity` flag is true at
the two poles, where longitude is mathematically undefined. Gameplay code must not use either
atlas to infer a world coordinate without an explicit, server-validated chart selection.

## Data flow

1. `UniverseGeometryCompiler` derives one `CelestialGlobeGeometry` per Star, Planet or Moon.
   Alpha selects its first stack deterministically; the editor can later expose this as a body
   display setting.
2. `UniverseGlobeRenderSync` sends geometry, visual configuration and celestial positions to
   clients. It deliberately transfers no terrain data.
3. `GlobeRenderPlanner` produces a floating-origin-safe draw plan and LOD choice.
4. A terrain provider may fill `GlobeTileCache` only with server-authorized top-down source
   tiles. The cache is renderer-neutral so texture/FBO ownership can stay in a client adapter.

The GPU adapter renders world tiles into a dedicated target without entering Immersive-Portals
recursion, and must not grant clients visibility of arbitrary unloaded surface chunks. It renders
the mesh/atlas presentation while the server continues to own all collisions and crossings.
