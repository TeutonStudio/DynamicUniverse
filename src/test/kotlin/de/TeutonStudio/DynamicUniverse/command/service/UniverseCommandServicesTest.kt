package de.TeutonStudio.DynamicUniverse.command.service

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionPosition
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.runtime.UniverseRuntimeRegistry
import de.TeutonStudio.DynamicUniverse.worldtype.CelestialGroup
import de.TeutonStudio.DynamicUniverse.worldtype.CelestialGroupKind
import de.TeutonStudio.DynamicUniverse.worldtype.Galaxy
import de.TeutonStudio.DynamicUniverse.worldtype.Planet
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionLayer
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionRole
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionStack
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UniverseCommandServicesTest {
    private val core = DimensionId("dynamicuniverse:earth_core")
    private val sky = DimensionId("dynamicuniverse:earth_sky")
    private val space = DimensionId("dynamicuniverse:space")
    private val universe = UniverseWorldType(
        id = "dynamicuniverse:sol",
        universeDimension = space,
        galaxies = listOf(Galaxy("milky_way", listOf(CelestialGroup(
            id = "sol", kind = CelestialGroupKind.CLOUD, planets = listOf(Planet(
                id = "earth", planetCoreSize = 32.0, stacks = listOf(PlanetDimensionStack(
                    id = "main",
                    layersInnerToOuter = listOf(
                        PlanetDimensionLayer("core", PlanetDimensionRole.PLANET_CORE, core, DimensionScale(8)),
                        PlanetDimensionLayer(
                            "sky", PlanetDimensionRole.SKY, sky,
                            innerBoundarySurface = BoundarySurface.BEDROCK,
                            outerBoundarySurface = BoundarySurface.AIR,
                        ),
                    ),
                    outerToUniverseScale = DimensionScale(2),
                )),
            ))
        )))),
    )
    private val runtime = UniverseRuntimeRegistry().also { it.register(universe) }
    private val query = RegistryUniverseQueryService(runtime)

    @Test
    fun `inspects registered planet stacks and layers`() {
        val planet = query.inspectPlanet("dynamicuniverse:sol#earth")
        val layer = query.inspectLayer("dynamicuniverse:sol#earth", "main", "core")

        assertTrue(planet.successful)
        assertTrue(planet.lines.any { it == "Stacks: main" })
        assertTrue(layer.successful)
        assertTrue(layer.lines.any { it == "Above: sky" })
    }

    @Test
    fun `reports an unknown planet without guessing`() {
        assertFalse(query.inspectPlanet("dynamicuniverse:sol#mars").successful)
    }

    @Test
    fun `routes positions through registered connection scales`() {
        val result = DimensionRoutePlanner.route(universe, core, space, DimensionPosition(-5, 80, 7))

        assertEquals(DimensionPosition(-80, 80, 112), result)
    }
}
