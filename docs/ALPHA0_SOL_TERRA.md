# alpha0.sol.terra

`alpha0.sol.terra` resolves exactly one local radial dimension stack for every
planetary body. A stack is selected from registered **templates**; creation
then gives every selected layer its own local dimension binding. A template is
therefore never a globally shared Minecraft level.

## Boundary language

The core has no lower connection and exposes Bedrock at its upper boundary.
All other templates declare both boundaries through inspected worldgen data:

| Lower boundary | Upper boundary | Derived role |
| --- | --- | --- |
| Bedrock | Bedrock | shell |
| Bedrock | air | surface |
| Air | air | sky |

Air-to-Bedrock is forbidden. Adjacent layers must expose the same medium at
their shared boundary. The editor renders such a mismatch as an error row and
offers only an inserted descriptor that matches both sides. If no descriptor
can bridge it, the user must replace, remove, or reorder a neighbour.

Catalog entries are green when a generator adapter has verified their boundary
evidence, orange when a loaded, enterable dimension is discovered but still
unresolved, and red when it belongs to a separate universe. Orange entries may
be selected but block creation until evidence is available.

## Geometry

The body kind determines the coordinate factor for every radial layer step:

- planet: 8;
- dwarf planet: 4;
- moon: 2.

For core edge period `K`, the period of layer `d` is `K × factor^d`. These
periods describe the canonical horizontal coordinates of the local Minecraft
layers; they do not directly define the rendered planet radius.

Every planet selects one **surface projection layer**. For Terra this is the
Overworld surface unless a local custom profile explicitly chooses another
surface role. If that layer has square canonical period `L`, its complete
horizontal area is

```text
A_surface = L²
```

The `UniverseSpace` projection sphere is chosen to have exactly that area:

```text
4π r_projection² = A_surface
r_projection = sqrt(A_surface / (4π)) = L / (2 sqrt(π))
```

`r_projection` is a render/portal radius. It is neither the celestial collision
radius nor the former circumference-derived pseudo radius `L / (2π)`. The
purpose of the area-derived radius is to let the complete canonical surface be
represented on the planetary sphere with an equal-area projection, so equal
areas of the source surface occupy equal areas on the globe.

This does not make the two surfaces topologically identical. A horizontally
wrapped Minecraft surface is a torus `T²`, whereas the Universe projection
shell is a sphere `S²`. The projection must therefore use a deterministic atlas
with an accepted seam, chart boundary or singular point rather than pretending
a globally continuous one-to-one homeomorphism exists. Every canonical surface
location must nevertheless remain addressable from the sphere.

The sphere itself is not built from blocks. It is a curved representation and
portal surface embedded in `UniverseSpace`. Far away it may be rendered from a
coarse planet texture; with decreasing distance the renderer may refine through
height/biome data and terrain LOD into local portal patches. Crossing such a
patch resolves the selected spherical point back to the corresponding planet
coordinate and enters the outermost enterable sky layer. The local dimension
stack remains locally Euclidean.

Every air-to-air layer pair owns an automatic ten-block transition buffer,
split five blocks on each side.

The outermost sky layer connects to `UniverseSpace` as a seamless transition
for Creative flight and complete Sable vehicles. Coordinate selection and
physical object scale stay separate: the latter remains 1:1.

## Dynamic Bedrock apertures

A Bedrock-to-Bedrock boundary is an eligible dynamic aperture. Generator
adapters still report the exact lower or upper Bedrock plane; the runtime then
uses the immutable `DimensionConnection` only to choose the opposite endpoint
of a **new** opening.

The first destroyed boundary block establishes two anchors. The configured
stack scale maps that first source anchor to the other dimension. Once the
opening exists, every further cell is mapped **locally 1:1** as an offset from
those anchors. A factor of eight therefore maps a first Nether cell `10` to an
outer-layer anchor `80`, but extending the Nether opening to `11` opens `81`,
not `88`. Physical portal scale also remains 1:1.

Each opening is stored as a set of local two-dimensional cells. Four-neighbour
adjacency is evaluated in the layer's toroidal coordinate system, so an opening
may grow across an X/Z seam without splitting. Destroying a cell adjacent to
one aperture extends it. Destroying a cell that joins multiple apertures merges
them only when their local counterpart mapping agrees; otherwise the server
rejects the destruction rather than inventing a discontinuous portal.

Boundary destruction is server-authoritative and transactional. Before either
side changes, both endpoint planes, dimensions, target Bedrock and aperture
compatibility are validated. The vanilla break is cancelled and both block
changes are committed together. A failed mutation restores the previous block
states. Portal entities are a derived presentation layer and are not allowed to
become the authority for the logical opening.

Mutable openings are persisted separately in `BoundaryApertureSaveData` under
`dynamicuniverse_apertures`. `UniverseSaveData` remains the immutable definition
of the Universe, its stacks and generator-reported Bedrock planes. Immersive
Portals entities carry deterministic aperture tags and are rebuilt from the
logical aperture state; stale persisted entities with the same tag are removed
before materialization.

## Planet-core aperture mapping

The innermost Bedrock-to-Bedrock connection has a protected cube-shell
counterpart. A player may start a new opening on either the first non-core
layer or the planet-core shell. Starting from the core reserves the selected
interior shell cell and creates a free Deep-layer counterpart atomically;
cube edges and corners remain ineligible.

A persisted `CoreBoundaryAperture` contains its planet/connection identity,
creation sequence, deep-layer anchor, local aperture shape and a fixed
core-shell placement (face, local origin and quarter-turn rotation). This
makes a core-originated opening reversible and prevents later resolution from
moving an existing hole.

For a Deep-originated aperture the resolver searches deterministic candidates
over the six cube faces, quarter-turn rotations and `(u,v)` offsets. The chosen
candidate is persisted. Every placement is valid only when the complete shape
lies on one face, stays inside the configured edge margin and does not overlap
or directly touch an already assigned core opening.

On server start the core holes are reconstructed from their persisted placements.
Legacy records without a placement are resolved deterministically once. If an
aperture grows, its fixed local mapping is extended; obsolete projected cells
are restored to Bedrock and new cells are opened in one transaction.

## Local profiles and the End

Changing a body created from a template immediately materializes a local custom
profile. The UI keeps both names visible, for example `Luna · Mond Custom`, and
retains the original template only as provenance. Template updates never change
an existing local profile.

The End is a red, isolated universe. It is never selectable as a Terra stack
layer. The End and the technical All host loop their actual lower and upper
build-height overflows back into themselves without an editor hint or Terra portal
connection. With Immersive Portals available this is materialized as global
horizontal portals; the server-side listener is the fallback without that optional mod.

The Overworld-to-Aether route is a separate optional vertical air seam, not a
Terra radial layer: rising through the Overworld's upper build bound enters the
Aether at its lower bound; falling below the Aether returns to the Overworld
top. The seam activates only when Aether has registered its own level stem. With
Immersive Portals it is represented by two global horizontal portal planes and
therefore does not impose a dimension-change loading screen. The Overworld plane
is a ceiling visible from below; the Aether plane is a floor visible from above.
Arrivals are offset four blocks into their target level, preventing Aether's
lower-bound "descending" transition. DynamicUniverse also suppresses Aether's
standard travel-screen packets only while this IP-owned seam is active.
