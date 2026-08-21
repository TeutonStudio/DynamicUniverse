package de.TeutonStudio.DynamicUniverse.worldtype

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaries
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryType
import de.TeutonStudio.DynamicUniverse.dimension.DimensionPosition
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.dimension.VerticalDimensionFace
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

    @Test
    fun `bedrock at the overworld bottom resolves to the nether top`() {
        val nether = DimensionId("minecraft:the_nether")
        val overworld = DimensionId("minecraft:overworld")
        val stack = PlanetDimensionStack(
            id = "main",
            layersInnerToOuter = listOf(
                PlanetDimensionLayer("nether", PlanetDimensionRole.PLANET_CORE, nether, DimensionScale(8), DimensionBoundaries.BEDROCK_TO_BEDROCK),
                PlanetDimensionLayer("surface", PlanetDimensionRole.SURFACE, overworld, DimensionScale.ONE, DimensionBoundaries.BEDROCK_TO_AIR),
                PlanetDimensionLayer("sky", PlanetDimensionRole.SKY, DimensionId("dynamicuniverse:earth/sky"), boundaries = DimensionBoundaries.AIR_TO_AIR),
            ),
        )
        val worldType = worldType(stack)

        val portal = requireNotNull(
            worldType.localEuclideanPortalGraph().portalAt(overworld, VerticalDimensionFace.BOTTOM),
        )

        assertEquals(nether, portal.target.dimension)
        assertEquals(VerticalDimensionFace.TOP, portal.target.face)
        assertEquals(DimensionBoundaryType.BEDROCK, portal.boundary)
        assertEquals(DimensionPosition(10, 12, -3), portal.targetPosition(DimensionPosition(80, 12, -24)))
    }

    @Test
    fun `air boundary resolves automatically from lower top to upper bottom`() {
        val surface = DimensionId("minecraft:overworld")
        val sky = DimensionId("dynamicuniverse:earth/sky")
        val stack = PlanetDimensionStack(
            id = "main",
            layersInnerToOuter = listOf(
                PlanetDimensionLayer("nether", PlanetDimensionRole.PLANET_CORE, DimensionId("minecraft:the_nether"), DimensionScale(8), DimensionBoundaries.BEDROCK_TO_BEDROCK),
                PlanetDimensionLayer("surface", PlanetDimensionRole.SURFACE, surface, DimensionScale.ONE, DimensionBoundaries.BEDROCK_TO_AIR),
                PlanetDimensionLayer("sky", PlanetDimensionRole.SKY, sky, boundaries = DimensionBoundaries.AIR_TO_AIR),
            ),
        )

        val portal = requireNotNull(worldType(stack).localEuclideanPortalGraph().portalAt(surface, VerticalDimensionFace.TOP))

        assertEquals(sky, portal.target.dimension)
        assertEquals(VerticalDimensionFace.BOTTOM, portal.target.face)
        assertEquals(DimensionBoundaryType.AIR, portal.boundary)
    }

    @Test
    fun `a dimension cannot be assigned to two vertical stack layers`() {
        val overworld = DimensionId("minecraft:overworld")
        val error = assertThrows(IllegalArgumentException::class.java) {
            PlanetDimensionStack(
                id = "invalid",
                layersInnerToOuter = listOf(
                    PlanetDimensionLayer("core", PlanetDimensionRole.PLANET_CORE, overworld, DimensionScale.ONE, DimensionBoundaries.BEDROCK_TO_BEDROCK),
                    PlanetDimensionLayer("sky", PlanetDimensionRole.SKY, overworld, boundaries = DimensionBoundaries.BEDROCK_TO_BEDROCK),
                ),
            )
        }

        assertEquals("A dimension may occur only once per stack.", error.message)
    }

    private fun worldType(stack: PlanetDimensionStack): UniverseWorldType = UniverseWorldType(
        id = "dynamicuniverse:sol",
        universeDimension = DimensionId("dynamicuniverse:sol"),
        galaxies = listOf(
            Galaxy(
                id = "milky_way",
                groups = listOf(
                    CelestialGroup(
                        id = "sol",
                        kind = CelestialGroupKind.SOLAR_SYSTEM,
                        star = Star("sun"),
                        planets = listOf(Planet("earth", 1_200_000.0, listOf(stack))),
                    ),
                ),
            ),
        ),
    )
}
