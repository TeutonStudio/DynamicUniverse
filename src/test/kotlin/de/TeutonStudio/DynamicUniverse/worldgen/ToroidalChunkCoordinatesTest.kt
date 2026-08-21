package de.TeutonStudio.DynamicUniverse.worldgen

import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import kotlin.test.Test
import kotlin.test.assertEquals

class ToroidalChunkCoordinatesTest {
    @Test
    fun `opposite seam chunks share canonical worldgen coordinates`() {
        val coordinates = ToroidalChunkCoordinates(HorizontalPeriod(1024))

        assertEquals(ToroidalChunkPosition(-32, 7), coordinates.canonical(32, 7))
        assertEquals(ToroidalChunkPosition(31, -32), coordinates.canonical(31, 32))
    }
}
