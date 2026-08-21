package de.TeutonStudio.DynamicUniverse.worldtype

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaries
import de.TeutonStudio.DynamicUniverse.dimension.DimensionPosition
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UniverseWorldTypeTest {
    @Test
    fun `dimension stacks accept matching shared boundaries`() {
        assertDoesNotThrow {
            PlanetDimensionStack(
                id = "main",
                layersInnerToOuter = listOf(
                    PlanetDimensionLayer("core", PlanetDimensionRole.PLANET_CORE, DimensionId("dynamicuniverse:earth/core"), DimensionScale.ONE, DimensionBoundaries.BEDROCK_TO_BEDROCK),
                    PlanetDimensionLayer("sky", PlanetDimensionRole.SKY, DimensionId("dynamicuniverse:earth/sky"), boundaries = DimensionBoundaries.BEDROCK_TO_BEDROCK),
                ),
            )
        }
        assertDoesNotThrow {
            PlanetDimensionStack(
                id = "air_stack",
                layersInnerToOuter = listOf(
                    PlanetDimensionLayer("core", PlanetDimensionRole.PLANET_CORE, DimensionId("dynamicuniverse:earth/core_air"), DimensionScale.ONE, DimensionBoundaries.AIR_TO_AIR),
                    PlanetDimensionLayer("sky", PlanetDimensionRole.SKY, DimensionId("dynamicuniverse:earth/sky_air"), boundaries = DimensionBoundaries.AIR_TO_AIR),
                ),
            )
        }
    }

    @Test
    fun `dimension stacks reject incompatible shared boundaries`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            PlanetDimensionStack(
                id = "main",
                layersInnerToOuter = listOf(
                    PlanetDimensionLayer("core", PlanetDimensionRole.PLANET_CORE, DimensionId("dynamicuniverse:earth/core"), DimensionScale.ONE, DimensionBoundaries.BEDROCK_TO_BEDROCK),
                    PlanetDimensionLayer("sky", PlanetDimensionRole.SKY, DimensionId("dynamicuniverse:earth/sky"), boundaries = DimensionBoundaries.AIR_TO_AIR),
                ),
            )
        }

        assertEquals("Adjacent dimension boundaries must match (AIR-to-AIR or BEDROCK-to-BEDROCK).", error.message)
    }

    @Test
    fun `surface dimensions require one bedrock and one air edge`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlanetDimensionLayer(
                "surface",
                PlanetDimensionRole.SURFACE,
                DimensionId("dynamicuniverse:earth/surface"),
                boundaries = DimensionBoundaries.AIR_TO_AIR,
            )
        }
    }

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
                            star = Star("sun"),
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
