package de.TeutonStudio.DynamicUniverse.cosmos

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class UniverseSpaceTest {
    @Test
    fun `planet binding round-trips position and velocity through an arbitrarily oriented local frame`() {
        val binding = PlanetSpaceBinding(
            planetId = "earth",
            localSpaceId = "dynamicuniverse:earth/surface",
            universeSpace = UniverseSpace("dynamicuniverse:sol"),
            kinematics = UniverseKinematicState(
                position = Vector3(1_000.0, -200.0, 50.0),
                velocity = Vector3(4.0, 5.0, 6.0),
                orientation = SpatialRotation.of(0.0, 0.0, sqrt(0.5), sqrt(0.5)),
            ),
            localUnitsPerUniverseUnit = 4.0,
        )

        val localPosition = Vector3(8.0, 12.0, -4.0)
        val localVelocity = Vector3(4.0, -8.0, 0.0)

        assertVectorEquals(localPosition, binding.localPosition(binding.universePosition(localPosition)))
        assertVectorEquals(localVelocity, binding.localVelocity(binding.universeVelocity(localVelocity)))
    }

    @Test
    fun `spatial objects retain full kinematic state in the cosmos runtime`() {
        val cosmos = DynamicCosmos(gravityConstant = 0.0)
        val state = UniverseKinematicState(Vector3(1.0, 2.0, 3.0), Vector3(4.0, 0.0, -2.0))
        cosmos.register(CosmicSpatialObject("station", 10.0, 2.0, state))

        cosmos.tick(0.5)

        assertVectorEquals(Vector3(3.0, 2.0, 2.0), cosmos.spatialObject("station")!!.kinematics.position)
    }

    private fun assertVectorEquals(expected: Vector3, actual: Vector3) {
        assertEquals(expected.x, actual.x, absoluteTolerance = 1e-10)
        assertEquals(expected.y, actual.y, absoluteTolerance = 1e-10)
        assertEquals(expected.z, actual.z, absoluteTolerance = 1e-10)
    }
}
