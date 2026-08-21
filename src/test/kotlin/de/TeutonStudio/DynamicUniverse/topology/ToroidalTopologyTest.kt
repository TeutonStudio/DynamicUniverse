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

    @Test
    fun `canonicalization remains safe for arbitrary long coordinates`() {
        val period = HorizontalPeriod(1024)

        assertEquals(-1, period.canonical(Long.MAX_VALUE))
        assertEquals(0, period.canonical(Long.MIN_VALUE))
    }

    @Test
    fun `seam is centered and spans exactly one period`() {
        val seam = ToroidalSeamSpec.centered(HorizontalPeriod(1024))

        assertEquals(-512, seam.minX)
        assertEquals(512, seam.maxX)
        assertEquals(1024, seam.maxZ - seam.minZ)
    }
}
