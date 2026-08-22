package de.TeutonStudio.DynamicUniverse.compat.sable

import de.TeutonStudio.DynamicUniverse.cosmos.CosmicSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseKinematicState
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class SableSublevelBindingTest {
    @Test
    fun `sublevel positions are projected through the owning cosmic object`() {
        val binding = SableSublevelBinding(SableSublevelId("sable:ship-42"), "ship-42")
        val ship = CosmicSpatialObject(
            id = "ship-42",
            mass = 100.0,
            radius = 10.0,
            kinematics = UniverseKinematicState(Vector3(1_000.0, 500.0, -30.0)),
        )

        val position = binding.universePosition(
            sublevelLocalPosition = Vector3(1.0, 2.0, 3.0),
            sublevelPose = SableSublevelPose(Vector3(10.0, 0.0, 0.0)),
            cosmicObject = ship,
        )

        assertEquals(Vector3(1_011.0, 502.0, -27.0), position)
    }
}
