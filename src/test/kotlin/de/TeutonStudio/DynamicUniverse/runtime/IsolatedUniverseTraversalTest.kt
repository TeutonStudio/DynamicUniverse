package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.worldtype.VerticalLoop
import kotlin.test.Test
import kotlin.test.assertEquals

class IsolatedUniverseTraversalTest {
    @Test
    fun `the End re-enters at the opposite vertical side without changing horizontal position`() {
        val bounds = VerticalLoopBounds(-64.0, 320.0)

        assertEquals(SpatialPosition(12.0, 319.0, -4.0), IsolatedUniverseTraversal.reenter(SpatialPosition(12.0, -65.0, -4.0), bounds, VerticalLoop.BOTH_DIRECTIONS))
        assertEquals(SpatialPosition(12.0, -63.0, -4.0), IsolatedUniverseTraversal.reenter(SpatialPosition(12.0, 321.0, -4.0), bounds, VerticalLoop.BOTH_DIRECTIONS))
        assertEquals(SpatialPosition(12.0, -64.0, -4.0), IsolatedUniverseTraversal.reenter(SpatialPosition(12.0, 320.0, -4.0), bounds, VerticalLoop.BOTH_DIRECTIONS))
    }
}
