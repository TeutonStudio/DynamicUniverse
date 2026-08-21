package de.TeutonStudio.GalactiCraft.dimension

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DimensionStackTest {
    @Test
    fun `scale ratios map outer coordinates without a hard-coded eight`() {
        assertEquals(21, ScaleRatio(3, 1).mapOuter(7))
        assertEquals(-21, ScaleRatio(3, 1).mapOuter(-7))
    }

    @Test
    fun `stack requires core and sky endpoints`() {
        assertFailsWith<IllegalArgumentException> {
            DimensionStack("invalid", listOf(DimensionLayer("surface", LayerRole.SURFACE, "minecraft:overworld")))
        }
    }
}
