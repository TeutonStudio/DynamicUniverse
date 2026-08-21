# Configuration and templates

Planet templates are server data. A template contains a body definition and a non-empty list of stacks. Configuration changes affect only future planet spawns; a spawned planet's manifest is immutable except through an explicit migration API.

## Earth default

```json
{
  "id": "galacticraft:earth",
  "body": { "mass": 5.972e24, "radius": 6371000.0, "collisionMaterial": "rock" },
  "stacks": [{
    "id": "main",
    "layers": [
      { "id": "core", "generationTemplate": "minecraft:the_nether", "toOuterScale": "8:1" },
      { "id": "deep_nether", "generationTemplate": "minecraft:the_nether", "toOuterScale": "8:1" },
      { "id": "nether", "generationTemplate": "minecraft:the_nether", "toOuterScale": "8:1" },
      { "id": "overworld", "generationTemplate": "minecraft:overworld", "toOuterScale": "8:1" },
      { "id": "sky", "generationTemplate": "minecraft:overworld" }
    ]
  }]
}
```

`toOuterScale` describes how an `(x,z)` coordinate maps from this layer to the next outer layer. Thus `8:1` maps `(x,z)` to `(8x,8z)`. Ratios must be positive, finite, and safe for `Long` coordinate arithmetic.

More than one stack may be declared per planet, for example a main geology stack plus an artificial habitat stack. Each gets its own core and cosmos endpoint binding while sharing the same celestial body.

## Physics values

- `mass`: strictly positive kilograms in the simulation's real-world-style scale.
- `radius`: strictly positive metres used for collision detection.
- `collisionMaterial.restitution`: `[0, 1]`; default `0.65`.
- `initialPosition` and `initialVelocity`: required for an administratively spawned body and validated against existing bodies before creation.
