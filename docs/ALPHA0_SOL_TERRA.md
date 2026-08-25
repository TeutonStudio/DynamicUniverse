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

For core edge period `K`, the period of layer `d` is `K × factor^d`. The
render-only pseudo radius is `period / (2π)`; it is not a cosmic collision
radius. Every air-to-air layer pair owns an automatic ten-block transition
buffer, split five blocks on each side.

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
layer. A server-side vertical-boundary listener loops its actual lower and upper
build-height overflows back into the End without an editor hint or Terra portal
connection.

The Overworld-to-Aether route is a separate optional vertical air seam, not a
Terra radial layer: rising through the Overworld's upper build bound enters the
Aether at its lower bound; falling below the Aether returns to the Overworld
top. The seam activates only when Aether has registered its own level stem.
