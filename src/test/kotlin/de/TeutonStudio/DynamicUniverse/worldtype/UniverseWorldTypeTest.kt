package de.TeutonStudio.DynamicUniverse.worldtype

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionPosition
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UniverseWorldTypeTest {
    @Test
    fun `Universe creates reversible routes from core to space`() {
        val core = DimensionId("dynamicuniverse:earth/core")
        val surface = DimensionId("dynamicuniverse:earth/surface")
        val universe = DimensionId("dynamicuniverse:sol")
        val worldType = UniverseWorldType(
            id = "dynamicuniverse:sol",
            universeDimension = universe,
            galaxies = listOf(
                Galaxy(
                    id = "milky_way",
                    groups = listOf(
                        CelestialGroup(
                            id = "sol",
                            kind = CelestialGroupKind.SOLAR_SYSTEM,
                            star = Star(
                                "sun",
                                listOf(
                                    PlanetDimensionStack(
                                        "main",
                                        listOf(
                                            PlanetDimensionLayer("core", PlanetDimensionRole.PLANET_CORE, DimensionId("dynamicuniverse:sun/core"), DimensionScale(1)),
                                            PlanetDimensionLayer("corona", PlanetDimensionRole.SKY, DimensionId("dynamicuniverse:sun/corona")),
                                        ),
                                    ),
                                ),
                            ),
                            planets = listOf(
                                Planet(
                                    id = "earth",
                                    planetCoreSize = 1_200_000.0,
                                    stacks = listOf(
                                        PlanetDimensionStack(
                                            id = "main",
                                            layersInnerToOuter = listOf(
                                                PlanetDimensionLayer("core", PlanetDimensionRole.PLANET_CORE, core, DimensionScale(8)),
                                                PlanetDimensionLayer("surface", PlanetDimensionRole.SKY, surface),
                                            ),
                                            outerToUniverseScale = DimensionScale(2),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val graph = worldType.connectionGraph()
        val coreToSurface = graph.routesFrom(core).single()
        val source = DimensionPosition(-5, 80, 7)
        val scaled = coreToSurface.targetPosition(source)

        assertEquals(DimensionPosition(-40, 80, 56), scaled)
        assertEquals(source, graph.transition(surface, "${coreToSurface.id}:reverse", scaled)?.position)
        assertEquals(universe, graph.routesFrom(surface).single { it.target == universe }.target)
    }
}
