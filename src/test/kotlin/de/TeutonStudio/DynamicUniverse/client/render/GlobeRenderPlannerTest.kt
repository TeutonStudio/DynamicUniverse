package de.TeutonStudio.DynamicUniverse.client.render

import de.TeutonStudio.DynamicUniverse.cosmos.GlobeVisualConfiguration
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.runtime.CelestialGlobeKind
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlobeRenderPlannerTest {
    @Test fun `nearer globes are planned first with finer lod`() {
        val period = HorizontalPeriod(256)
        fun body(id: String, distance: Double) = GlobeRenderBody(
            id, CelestialGlobeKind.PLANET, DimensionId("dynamicuniverse:$id"), period,
            GlobeVisualConfiguration(100.0), Vector3(distance, 0.0, 0.0),
        )
        val plan = GlobeRenderPlanner.plan(listOf(body("far", 10_000.0), body("near", 500.0)), Vector3.ZERO, 1_080, Math.PI / 2.0, 4)
        assertEquals("near", plan.first().body.bodyId)
        assertTrue(plan.first().lod <= plan.last().lod)
    }

    @Test fun `tile cache evicts least recently used tile`() {
        val cache = GlobeTileCache(2)
        fun key(x: Int) = GlobeTileKey("terra", de.TeutonStudio.DynamicUniverse.cosmos.GlobeAtlas.SIX_CHART, 0, x, 0, 0)
        val tile = GlobeTile(intArrayOf(0), 1, 1)
        cache.put(key(1), tile); cache.put(key(2), tile); cache.get(key(1)); cache.put(key(3), tile)
        assertTrue(cache.get(key(1)) != null)
        assertEquals(null, cache.get(key(2)))
    }
}
