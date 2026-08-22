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

## Bedrock apertures

A Bedrock-to-Bedrock boundary is an eligible radial aperture. Its generator
adapter records the exact lower or upper Bedrock plane for each local
dimension. When a player breaks Bedrock exactly on such a registered plane,
the server maps the horizontal coordinate through the stack scale and removes
the corresponding Bedrock block from the adjacent level as well. The source
break then completes normally, leaving one matching hole on both sides.

This rule is intentionally narrow: ordinary Bedrock elsewhere, AIR boundaries,
the core's missing lower boundary, and the sky-to-Universe transition never
open an aperture. If the mapped target is no longer Bedrock, the source break
is left alone rather than overwriting player changes. The generated chunk data
persists both holes; runtime setup installs the immutable geometry manifest and
the generator-reported planes through `BedrockApertureRuntime.install` after
the local levels exist.

## Local profiles and the End

Changing a body created from a template immediately materializes a local custom
profile. The UI keeps both names visible, for example `Luna · Mond Custom`, and
retains the original template only as provenance. Template updates never change
an existing local profile.

The End is a red, isolated universe. It is never selectable as a Terra stack
layer. Its vertical lower and upper overflows loop back into the End without an
editor hint or Terra portal connection.
