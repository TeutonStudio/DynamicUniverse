package de.TeutonStudio.DynamicUniverse.topology

import kotlin.test.Test
import kotlin.test.assertEquals

class ToroidalTopologyTest {
    @Test
    fun `east edge wraps to west with unchanged z`() {
        val topology = ToroidalTopology(HorizontalPeriod(1024))

        val result = requireNotNull(topology.cross(HorizontalPosition(512, 17)))

        assertEquals(HorizontalEdge.EAST, result.entered)
        assertEquals(HorizontalPosition(-512, 17), result.destination)
    }
}
