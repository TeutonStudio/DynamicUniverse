package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface
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

/** The no-customisation Terra topology supplied by the data-driven Universe world preset. */
object UniverseDefaultWorldType {
    val worldType: UniverseWorldType = UniverseWorldType(
        id = "dynamicuniverse:created",
        universeDimension = DimensionId("dynamicuniverse:created/universe"),
        galaxies = listOf(
            Galaxy("galaxy_0", listOf(
                CelestialGroup(
                    id = "system_0",
                    kind = CelestialGroupKind.SOLAR_SYSTEM,
                    star = Star("star_0_0", listOf(starStack())),
                    planets = listOf(Planet("planet_0_0_0", 32.0, listOf(terraStack()))),
                ),
                CelestialGroup("cloud_1", CelestialGroupKind.CLOUD, planets = emptyList()),
            )),
        ),
    )

    val plan: UniverseLevelStemPlan = UniverseLevelStemPlan.builtIns(worldType)

    private fun starStack() = PlanetDimensionStack(
        id = "main",
        layersInnerToOuter = listOf(
            layer("core", PlanetDimensionRole.PLANET_CORE, "dynamicuniverse:created/star/0/0/core", 1, null, BoundarySurface.BEDROCK),
            layer("radiative_zone", PlanetDimensionRole.SURFACE, "dynamicuniverse:created/star/0/0/radiative_zone", 1, BoundarySurface.BEDROCK, BoundarySurface.AIR),
            layer("corona", PlanetDimensionRole.SKY, "dynamicuniverse:created/star/0/0/corona", null, BoundarySurface.AIR, BoundarySurface.AIR),
        ),
    )

    private fun terraStack() = PlanetDimensionStack(
        id = "main",
        layersInnerToOuter = listOf(
            layer("core", PlanetDimensionRole.PLANET_CORE, "dynamicuniverse:created/planet/0/0/0/core", 8, null, BoundarySurface.BEDROCK),
            layer("nether", PlanetDimensionRole.INNER, "minecraft:the_nether", 8, BoundarySurface.BEDROCK, BoundarySurface.BEDROCK),
            layer("underground", PlanetDimensionRole.INNER, "undergarden:undergarden", 8, BoundarySurface.BEDROCK, BoundarySurface.BEDROCK),
            layer("overworld", PlanetDimensionRole.SURFACE, "minecraft:overworld", 8, BoundarySurface.BEDROCK, BoundarySurface.AIR),
            layer("aether", PlanetDimensionRole.SKY, "aether:the_aether", null, BoundarySurface.AIR, BoundarySurface.AIR),
        ),
    )

    private fun layer(
        id: String,
        role: PlanetDimensionRole,
        dimension: String,
        scale: Long?,
        inner: BoundarySurface?,
        outer: BoundarySurface,
    ) = PlanetDimensionLayer(
        id = id,
        role = role,
        dimension = DimensionId(dimension),
        toOuterScale = scale?.let(::DimensionScale),
        innerBoundarySurface = inner,
        outerBoundarySurface = outer,
    )
}
