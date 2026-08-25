package de.TeutonStudio.DynamicUniverse.cosmos

import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlobeProjectionTest {
    private val period = HorizontalPeriod(96)

    @Test fun `six-chart atlas samples a canonical torus position without pole singularities`() {
        val sample = GlobeProjection.sample(Vector3(1.0, 0.0, 0.0), period, GlobeVisualConfiguration(10.0))
        assertEquals(GlobeChart.POSITIVE_X, sample.chart)
        assertEquals(16L, sample.position.x)
        assertEquals(24L, sample.position.z)
        assertFalse(sample.polarSingularity)
    }

    @Test fun `six charts keep every cardinal direction inside the source torus`() {
        listOf(
            Vector3(1.0, 0.0, 0.0), Vector3(-1.0, 0.0, 0.0), Vector3(0.0, 1.0, 0.0),
            Vector3(0.0, -1.0, 0.0), Vector3(0.0, 0.0, 1.0), Vector3(0.0, 0.0, -1.0),
        ).forEach { direction ->
            val sample = GlobeProjection.sample(direction, period, GlobeVisualConfiguration(10.0))
            assertTrue(sample.position.x in -period.halfBlocks until period.halfBlocks)
            assertTrue(sample.position.z in -period.halfBlocks until period.halfBlocks)
        }
    }

    @Test fun `equirectangular debug projection exposes its polar singularity`() {
        val sample = GlobeProjection.sample(
            Vector3(0.0, 1.0, 0.0), period,
            GlobeVisualConfiguration(10.0, atlas = GlobeAtlas.EQUIRECTANGULAR_DEBUG),
        )
        assertTrue(sample.polarSingularity)
    }
}
