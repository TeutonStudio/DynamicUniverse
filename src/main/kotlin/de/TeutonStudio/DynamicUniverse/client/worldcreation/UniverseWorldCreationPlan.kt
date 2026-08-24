package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.runtime.UniverseLevelStemPlan
import de.TeutonStudio.DynamicUniverse.worldtype.CelestialGroup
import de.TeutonStudio.DynamicUniverse.worldtype.CelestialGroupKind
import de.TeutonStudio.DynamicUniverse.worldtype.Galaxy
import de.TeutonStudio.DynamicUniverse.worldtype.Planet
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionLayer
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionRole
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionStack
import de.TeutonStudio.DynamicUniverse.worldtype.Star
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldType

/**
 * Freezes the editable tree into the server-neutral world type before world generation
 * starts. A later server bridge consumes this exact result instead of re-deriving stacks.
 */
fun UniverseWorldCreationDraft.toWorldType(id: String = "dynamicuniverse:created"): UniverseWorldType {
    require(validation().isValid) { "A Universe with incompatible bedrock/air boundaries cannot be created." }
    val universeDimension = DimensionId("dynamicuniverse:created/universe")
    return UniverseWorldType(
        id = id,
        universeDimension = universeDimension,
        galaxies = universe.galaxies.mapIndexed { galaxyIndex, galaxy ->
            Galaxy("galaxy_$galaxyIndex", galaxy.entries.mapIndexedNotNull { entryIndex, entry ->
                when (entry) {
                    is EditableCloud -> CelestialGroup("cloud_$entryIndex", CelestialGroupKind.CLOUD, planets = emptyList())
                    is EditableSolarSystem -> CelestialGroup(
                        id = "system_$entryIndex",
                        kind = CelestialGroupKind.SOLAR_SYSTEM,
                        star = Star("star_${galaxyIndex}_$entryIndex", listOf(
                            entry.star.dimensionStack.toStack("star/$galaxyIndex/$entryIndex", entry.star.name, 1, false),
                        )),
                        planets = entry.planets.mapIndexed { planetIndex, planet ->
                            planet.toWorldPlanet(
                                "planet_${galaxyIndex}_${entryIndex}_$planetIndex",
                                "planet/$galaxyIndex/$entryIndex/$planetIndex",
                                isPrimarySpawnPlanet = galaxyIndex == 0 && entryIndex == 0 && planetIndex == 0,
                            )
                        },
                    )
                }
            })
        },
    )
}

/** The complete pre-server plan: logical topology, generator templates, and verified planes. */
fun UniverseWorldCreationDraft.toLevelStemPlan(id: String = "dynamicuniverse:created"): UniverseLevelStemPlan =
    UniverseLevelStemPlan.builtIns(toWorldType(id))

private fun EditablePlanet.toWorldPlanet(id: String, path: String, isPrimarySpawnPlanet: Boolean): Planet = Planet(
    id = id,
    planetCoreSize = coreSize.toDouble(),
    stacks = listOf(dimensionStack.toStack(path, name, radialScale, isPrimarySpawnPlanet)),
    moons = moons.mapIndexed { index, moon -> moon.toWorldPlanet("${id}_moon_$index", "$path/moon/$index", false) },
)

private fun EditableDimensionStack.toStack(path: String, name: String, factor: Int, isPrimarySpawnPlanet: Boolean): PlanetDimensionStack =
    PlanetDimensionStack(
        id = "main",
        layersInnerToOuter = layers.mapIndexed { index, layer ->
            PlanetDimensionLayer(
                id = layer.id,
                role = when (layer.role) {
                    EditableDimensionRole.CORE -> PlanetDimensionRole.PLANET_CORE
                    EditableDimensionRole.SHELL -> PlanetDimensionRole.INNER
                    EditableDimensionRole.SURFACE -> PlanetDimensionRole.SURFACE
                    EditableDimensionRole.SKY -> PlanetDimensionRole.SKY
                },
                // The first planet's playable surface and Nether retain vanilla level keys so
                // normal spawn and vanilla Nether travel enter Terra's stack. Other layers stay
                // distinct, data-driven dimensions.
                dimension = when {
                    isPrimarySpawnPlanet && layer.descriptorId == PlanetDimensionCatalog.OVERWORLD_ID -> DimensionId("minecraft:overworld")
                    isPrimarySpawnPlanet && layer.descriptorId == PlanetDimensionCatalog.NETHER_ID -> DimensionId("minecraft:the_nether")
                    isPrimarySpawnPlanet && layer.descriptorId == PlanetDimensionCatalog.UNDERGROUND_ID -> DimensionId(PlanetDimensionCatalog.UNDERGROUND_ID)
                    isPrimarySpawnPlanet && layer.descriptorId == PlanetDimensionCatalog.AETHER_ID -> DimensionId(PlanetDimensionCatalog.AETHER_ID)
                    else -> DimensionId("dynamicuniverse:created/$path/${layer.id}")
                },
                toOuterScale = if (index == layers.lastIndex) null else DimensionScale(factor.toLong()),
                innerBoundarySurface = layer.innerBoundarySurface,
                outerBoundarySurface = layer.outerBoundarySurface,
            )
        },
    )
