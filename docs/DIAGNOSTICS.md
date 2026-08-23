# Development diagnostics

The diagnostic dependencies are only loaded by the Gradle development runtime. They
do not change Dynamic Universe persistence behavior and are not embedded in the
published mod JAR.

Start the profile with:

```bash
./gradlew runClient
```

## Collecting a persistence or player-data report

1. Reproduce the fault in a disposable world, recording the player UUID, dimension
   ID, position, and the exact action that preceded the invalid state.
2. Use Jade while targeting the player, relevant block entity, or portal to confirm
   its resolved type and visible state. The existing JEI runtime remains the recipe
   and item/registry lookup tool.
3. With NBTviewer, inspect the held stack and the looked-at entity SNBT. Its entity
   overlay is toggled with `N`; capture the displayed data rather than editing it.
4. Use BetterF3 together with vanilla `F3+G` to capture the current dimension,
   coordinates, chunk boundaries, and client timing. `F3+G` is the supported
   chunk-boundary diagnostic for this NeoForge 1.21.1 profile.
5. For a tick stall or unexpectedly repeated lifecycle work, run `/spark profiler
   start`, reproduce the issue briefly, then run `/spark profiler stop`. Preserve
   the generated spark report URL with `logs/latest.log`.

For server-authoritative player data, vanilla `/data get entity <player>` is useful
for the live entity compound. Dynamic Universe's saved definition is separate
server data (`data/dynamicuniverse_universe.dat` in a world save), so it must be
inspected from a copy of the affected world after the server is stopped. Never edit
the original save while diagnosing an invalid-data report.

## Scope and licensing

Jade, NBTviewer, spark, and BetterF3 are downloaded from their authors' Modrinth
releases as pinned development `runtimeOnly` dependencies. They are not shaded,
repackaged, or distributed by Dynamic Universe. Keep their upstream notices and
licenses with any separate modpack distribution.
