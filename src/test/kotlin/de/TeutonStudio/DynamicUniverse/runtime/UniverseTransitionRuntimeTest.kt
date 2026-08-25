package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import de.TeutonStudio.DynamicUniverse.worldtype.IsolatedUniverseDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UniverseTransitionRuntimeTest {
    private val layer = DimensionId("dynamicuniverse:test/layer")
    private val target = DimensionId("dynamicuniverse:test/outer")
    private val planner = UniverseTransitionPlanner(
        UniverseGeometryManifest(
            universe = de.TeutonStudio.DynamicUniverse.cosmos.UniverseFrame("dynamicuniverse:test/universe"),
            layers = listOf(LayerGeometry(layer, HorizontalPeriod(1024))),
            airBuffers = emptyList(), links = emptyList(), planetFrames = emptyList(),
            isolatedUniverses = listOf(IsolatedUniverseDefinition.end()),
        ),
    )

    @Test fun `horizontal wrap preserves fractional position velocity and passengers`() {
        val state = TraversalState(SpatialPosition(513.25, 70.0, -513.75), SpatialVelocity(8.0, 0.0, -2.0))
        val wrapped = requireNotNull(planner.horizontal(layer, state))
        assertEquals(SpatialPosition(-510.75, 70.0, 510.25), wrapped.position)
        assertEquals(state.velocity, wrapped.velocity)
        assertNull(planner.horizontal(layer, wrapped))
    }

    @Test fun `vertical transition uses shared coordinate and velocity transform`() {
        val connection = DimensionConnection("dynamicuniverse:test/link", layer, target, DimensionScale(8))
        val result = planner.vertical(connection, TraversalState(SpatialPosition(2.0, 10.0, -3.0), SpatialVelocity(1.0, 0.0, -1.0)))
        assertEquals(target, result.target)
        assertEquals(SpatialPosition(16.0, 10.0, -24.0), result.state.position)
        assertEquals(SpatialVelocity(8.0, 0.0, -8.0), result.state.velocity)
    }
}
