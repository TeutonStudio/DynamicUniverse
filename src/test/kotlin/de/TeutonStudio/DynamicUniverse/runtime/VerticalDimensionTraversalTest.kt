package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.UniverseFrame
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity
import de.TeutonStudio.DynamicUniverse.worldtype.IsolatedUniverseDefinition
import de.TeutonStudio.DynamicUniverse.worldtype.VerticalDimensionSeam
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VerticalDimensionTraversalTest {
    private val end = DimensionId("minecraft:the_end")
    private val overworld = DimensionId("minecraft:overworld")
    private val aether = DimensionId("aether:the_aether")
    private val universe = DimensionId("dynamicuniverse:test/universe")
    private val endBounds = VerticalDimensionBounds(0.0, 256.0)
    private val overworldBounds = VerticalDimensionBounds(-64.0, 320.0)
    private val aetherBounds = VerticalDimensionBounds(0.0, 256.0)

    private val planner = VerticalDimensionTraversalPlanner(
        UniverseGeometryManifest(
            universe = UniverseFrame(universe.value), layers = emptyList(), airBuffers = emptyList(), links = emptyList(),
            planetSpaces = emptyList(), isolatedUniverses = listOf(IsolatedUniverseDefinition.end()),
            verticalSeams = listOf(VerticalDimensionSeam.overworldToAether()),
        ),
    )

    @Test fun `End re-enters at its opposite height before void fall`() {
        val result = requireNotNull(planner.traverse(end, endBounds, state(8.0, 256.0, -3.0)) { endBounds })
        assertEquals(end, result.target)
        assertEquals(SpatialPosition(8.0, 0.0, -3.0), result.state.position)
        assertEquals(SpatialVelocity(1.0, -2.0, 3.0), result.state.velocity)
    }

    @Test fun `Overworld top leads into Aether bottom and reverse fall returns to Overworld top`() {
        val upward = requireNotNull(planner.traverse(overworld, overworldBounds, state(12.0, 320.0, -4.0)) { boundsFor(it) })
        assertEquals(aether, upward.target)
        assertEquals(SpatialPosition(12.0, 0.0, -4.0), upward.state.position)

        val downward = requireNotNull(planner.traverse(aether, aetherBounds, state(12.0, -1.0, -4.0)) { boundsFor(it) })
        assertEquals(overworld, downward.target)
        assertEquals(SpatialPosition(12.0, 319.0, -4.0), downward.state.position)
    }

    @Test fun `unloaded Aether leaves the optional seam inactive`() {
        assertNull(planner.traverse(overworld, overworldBounds, state(0.0, 320.0, 0.0)) { null })
    }

    @Test fun `universe host does not inherit the Ends vertical loop`() {
        assertNull(planner.traverse(universe, endBounds, state(5.0, -1.0, 7.0)) { endBounds })
    }

    private fun state(x: Double, y: Double, z: Double) = TraversalState(SpatialPosition(x, y, z), SpatialVelocity(1.0, -2.0, 3.0))
    private fun boundsFor(dimension: DimensionId) = when (dimension) {
        overworld -> overworldBounds
        aether -> aetherBounds
        else -> endBounds
    }
}
