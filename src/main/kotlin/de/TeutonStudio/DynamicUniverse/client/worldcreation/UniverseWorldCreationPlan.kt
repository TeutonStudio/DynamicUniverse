package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
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
                        star = Star("star_${galaxyIndex}_$entryIndex", listOf(entry.star.dimensionStack.toStack("star/$galaxyIndex/$entryIndex", entry.star.name, 1))),
                        planets = entry.planets.mapIndexed { planetIndex, planet ->
                            Planet(
                                id = "planet_${galaxyIndex}_${entryIndex}_$planetIndex",
                                planetCoreSize = planet.coreSize.toDouble(),
                                stacks = listOf(planet.dimensionStack.toStack("planet/$galaxyIndex/$entryIndex/$planetIndex", planet.name, planet.dimensionTransitionFactor)),
                            )
                        },
                    )
                }
            })
        },
    )
}

private fun EditableDimensionStack.toStack(path: String, name: String, factor: Int): PlanetDimensionStack =
    PlanetDimensionStack(
        id = "main",
        layersInnerToOuter = layers.mapIndexed { index, layer ->
            PlanetDimensionLayer(
                id = layer.id,
                role = when (layer.role) {
                    EditableDimensionRole.CORE -> PlanetDimensionRole.PLANET_CORE
                    EditableDimensionRole.INNER -> PlanetDimensionRole.INNER
                    EditableDimensionRole.SKY -> PlanetDimensionRole.SKY
                },
                dimension = DimensionId("dynamicuniverse:created/$path/${layer.id}"),
                toOuterScale = if (index == layers.lastIndex) null else DimensionScale(factor.toLong()),
                innerBoundarySurface = layer.innerBoundarySurface,
                outerBoundarySurface = layer.outerBoundarySurface,
            )
        },
    )
