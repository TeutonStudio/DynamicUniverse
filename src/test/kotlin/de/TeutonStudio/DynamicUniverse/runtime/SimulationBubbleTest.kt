package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SimulationBubbleTest {
    @Test
    fun `bubble rebase preserves the represented UniverseSpace position`() {
        val bubble = SimulationBubble(Vector3.ZERO, radius = 1_000.0, rebaseThreshold = 400.0, rebaseGridSize = 100.0)
        val objectUniversePosition = Vector3(1_250.0, -20.0, 55.0)
        val oldLocalPosition = bubble.localPosition(objectUniversePosition)

        val rebase = bubble.rebaseFor(Vector3(1_120.0, 0.0, 0.0))!!
        val newLocalPosition = rebase.rebaseLocalPosition(oldLocalPosition)

        assertEquals(rebase.next.localPosition(objectUniversePosition), newLocalPosition)
        assertEquals(Vector3(1_100.0, 0.0, 0.0), rebase.next.origin)
    }

    @Test
    fun `bubble does not rebase while focus remains below its threshold`() {
        val bubble = SimulationBubble(Vector3.ZERO, radius = 1_000.0, rebaseThreshold = 400.0)

        assertNull(bubble.rebaseFor(Vector3(399.0, 0.0, 0.0)))
    }
}
