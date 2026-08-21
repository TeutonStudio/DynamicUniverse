# Universe world type — implementation plan

## Scope and current-state audit

This plan is intentionally additive. At the planning baseline, the repository contains:

- one server-side marker, `PlanetWorldType.GALACTICRAFT_PLANETARY`;
- `PlanetTemplate` and `DimensionStack`, which already validate a core-to-cosmos radial sequence and explicit adjacent scale ratios;
- no client-side world-creation UI, world-type picker, customization button, or UI test fixture.

Consequently, this branch does **not** alter Kotlin runtime code, existing template data, dimension IDs, or the current command-based planet lifecycle. The first implementation must introduce the UI adapter behind a dedicated client boundary and keep common/server code free of client classes.

## Product decisions

### Hierarchy vocabulary

Use `CelestialGroup` beneath a galaxy rather than choosing one global meaning for the currently open “solar system or cloud” question.

| Group kind | Children | Own settings action |
| --- | --- | --- |
| `SOLAR_SYSTEM` | exactly one star, zero or more planets | system action and a separate star action |
| `CLOUD` | zero or more planets | cloud action; no star action |

The visible tree stays vertical and always follows this order:

```text
Universe
  Galaxy [settings]
    Solar system / cloud [settings]
      Sun [settings]                 (solar system only)
      Planet [settings]
        Dimensions [settings]
```

A planet's dimension editor presents an ordered radial path from the universe endpoint to the planet-core endpoint. The endpoints are fixed, visible, and non-removable; only dimensions between them are editable. It maps to the existing inner-to-outer `DimensionStack` direction at the persistence boundary, so the current core-to-cosmos invariant remains true.

### Planet and star editing

Planet settings include:

- planet identity and physical-body values already required by creation;
- `planetCoreSize`, stored separately from collision radius until physics defines a formal relation;
- an ordered list of intermediate dimensions, each with a stable draft ID, generation template, and role;
- one positive rational `dimensionChangeFactor` for every adjacent boundary. The UI labels direction explicitly (inner → outer) and shows both numerator and denominator; it must never infer a missing factor.

The star has its own settings action rather than being edited through the containing system. The first implementation should limit it to values that the server-side cosmos can validate; no rendering or terrain promises are implied.

## Customization-button contract

Introduce a client-side `WorldTypeCustomizationCapability` resolver at the point where the selected world type is rendered. It returns an enabled target or a disabled explanation; it does not mutate the selected type.

| Selected type | Button presentation | Activation |
| --- | --- | --- |
| Universe | enabled | opens the Universe hierarchy editor |
| Dimension Stack / “Mehr” | visible but disabled | tooltip and keyboard-accessible information dialog explain that customization is unavailable because it can make the stack topology invalid |
| any other type | enabled | invokes that type's established customization target |

The integration must use the actual world-type registry IDs when that UI exists; “Mehr” is a product label, not a stable identifier. Selection changes must recompute the resolver result so a disabled stack selection cannot leave the button disabled for the next type.

## Draft, persistence, and validation boundary

The editor uses a versioned, client-local `UniverseDraft`. It becomes a server creation request only after full validation and confirmation. Do not write partial hierarchy state to the world or call DynamicDimensions while the user is editing.

Before creating anything, validate atomically:

1. names and stable IDs are unique within their parent and valid for eventual namespaced runtime IDs;
2. a solar system has one star; a cloud has none;
3. every planet has both fixed endpoints, at least the required generated layer count, unique dimension IDs, and a valid generation template;
4. every adjacent factor is present, finite, positive, and safe for `Long` coordinate mapping;
5. core size and celestial-body collision radius are positive and obey any later defined relationship;
6. celestial body placement, mass, and velocity pass the existing server-side cosmos checks.

On any failure, preserve the draft and identify the exact tree node and field. On success, translate only the selected/confirmed planet definitions to the existing `PlanetTemplate`/manifest pipeline. Hierarchy metadata requires an explicitly versioned manifest extension; old manifests must load without it and retain their current behavior.

## Implementation sequence

1. Add a registry-safe Universe world-type definition and the pure configuration data model, including tests for group-kind, endpoint, factor, and core-size validation. Do not change `GALACTICRAFT_PLANETARY` semantics.
2. Add the client-only world-creation adapter and capability resolver. Test Universe enabled, Dimension Stack visibly disabled with accessible explanation, and another type re-enabled after a selection change.
3. Build the vertical tree editor, including separate galaxy, group, star, planet, and dimension settings actions. Keep edits in `UniverseDraft` only.
4. Add a server request/validation layer, transactional manifest extension, and restart compatibility tests before invoking DynamicDimensions.
5. Add migration only if a concrete product requirement appears; otherwise retain existing standalone planet templates unchanged.

## Acceptance checks

- Selecting Universe enables customization and opens a vertical Galaxy → group → planet hierarchy; solar systems show a separate Sun settings action, clouds do not.
- Selecting Dimension Stack / “Mehr” keeps the action visible but unavailable and exposes the reason without requiring a mouse.
- Selecting another world type immediately restores its normal customization action.
- A planet cannot save with a missing universe/core endpoint, missing factor, invalid ratio, or non-positive core size.
- Cancelling or failing validation creates no dimensions, bodies, or manifest records.
- Existing planetary templates and manifests load and behave unchanged.
