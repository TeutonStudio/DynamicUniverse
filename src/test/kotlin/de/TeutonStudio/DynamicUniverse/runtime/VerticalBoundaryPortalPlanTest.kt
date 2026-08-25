package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import kotlin.test.Test
import kotlin.test.assertEquals

class VerticalBoundaryPortalPlanTest {
    private val overworld = DimensionId("minecraft:overworld")
    private val aether = DimensionId("aether:the_aether")
    private val overworldBounds = VerticalPortalLevelBounds(-64, 320)
    private val aetherBounds = VerticalPortalLevelBounds(0, 256)

    @Test fun `Overworld ceiling is visible from below and enters safely above Aether floor`() {
        val upward = VerticalBoundaryPortalPlans.seam(overworld, overworldBounds, aether, aetherBounds).first()
        assertEquals(VerticalPortalSurface.CEILING, upward.sourceSurface)
        assertEquals(-1, upward.sourceSurface.expectedNormalY)
        assertEquals(4, upward.nativeTargetBounds().minY)
        assertEquals(256, upward.nativeTargetBounds().maxYExclusive)
    }

    @Test fun `Aether floor is visible from above and enters safely below Overworld ceiling`() {
        val downward = VerticalBoundaryPortalPlans.seam(overworld, overworldBounds, aether, aetherBounds).last()
        assertEquals(VerticalPortalSurface.FLOOR, downward.sourceSurface)
        assertEquals(1, downward.sourceSurface.expectedNormalY)
        assertEquals(-64, downward.nativeTargetBounds().minY)
        assertEquals(316, downward.nativeTargetBounds().maxYExclusive)
    }
}
