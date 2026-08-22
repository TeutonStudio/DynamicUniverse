package de.TeutonStudio.DynamicUniverse.worldtype

import de.TeutonStudio.DynamicUniverse.cosmos.UniverseKinematicState
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseSpace
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanetSpaceBindingsTest {
    @Test
    fun `configured planet creates a runtime binding without owning the host`() {
        val planet = Planet(
            id = "earth",
            planetCoreSize = 128.0,
            stacks = listOf(
                PlanetDimensionStack(
                    id = "main",
                    layersInnerToOuter = listOf(
                        PlanetDimensionLayer("core", PlanetDimensionRole.PLANET_CORE, de.TeutonStudio.DynamicUniverse.dimension.DimensionId("dynamicuniverse:earth/core"), de.TeutonStudio.DynamicUniverse.dimension.DimensionScale.ONE),
                        PlanetDimensionLayer("sky", PlanetDimensionRole.SKY, de.TeutonStudio.DynamicUniverse.dimension.DimensionId("dynamicuniverse:earth/sky")),
                    ),
                ),
            ),
        )

        val binding = planet.spaceBinding(
            localSpaceId = "dynamicuniverse:earth/sky",
            universeSpace = UniverseSpace("dynamicuniverse:sol"),
            kinematics = UniverseKinematicState(Vector3(10.0, 0.0, 0.0)),
        )

        assertEquals("earth", binding.planetId)
        assertEquals(Vector3(11.0, 0.0, 0.0), binding.universePosition(Vector3(1.0, 0.0, 0.0)))
    }
}
