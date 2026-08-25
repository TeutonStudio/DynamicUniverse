package de.TeutonStudio.DynamicUniverse.compat.distanthorizons

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PortalLodRenderPolicyTest {
    private val overworld = DimensionId("minecraft:overworld")
    private val aether = DimensionId("aether:the_aether")

    @Test
    fun `outer world permits only the player's own dimension`() {
        assertEquals(
            PortalLodRenderDecision.OUTER_WORLD,
            PortalLodRenderPolicy.decide(overworld, overworld, null, nativeFramebufferAvailable = true),
        )
        assertEquals(
            PortalLodRenderDecision.VANILLA_TARGET_CHUNKS,
            PortalLodRenderPolicy.decide(aether, overworld, null, nativeFramebufferAvailable = true),
        )
    }

    @Test
    fun `portal target is accepted even though the player remains in the source world`() {
        val scope = PortalLodRenderScope(
            sourceDimension = aether,
            targetDimension = overworld,
            recursionDepth = 1,
            frameBufferId = 7,
            hasStencil = true,
        )

        assertEquals(
            PortalLodRenderDecision.NATIVE_PORTAL_LOD,
            PortalLodRenderPolicy.decide(overworld, aether, scope, nativeFramebufferAvailable = true),
        )
    }

    @Test
    fun `wrong target or unavailable stencil uses vanilla target chunks`() {
        val scope = PortalLodRenderScope(
            sourceDimension = overworld,
            targetDimension = aether,
            recursionDepth = 1,
            frameBufferId = 7,
            hasStencil = true,
        )

        assertEquals(
            PortalLodRenderDecision.VANILLA_TARGET_CHUNKS,
            PortalLodRenderPolicy.decide(overworld, overworld, scope, nativeFramebufferAvailable = true),
        )
        assertEquals(
            PortalLodRenderDecision.VANILLA_TARGET_CHUNKS,
            PortalLodRenderPolicy.decide(aether, overworld, scope.copy(hasStencil = false), nativeFramebufferAvailable = true),
        )
        assertEquals(
            PortalLodRenderDecision.VANILLA_TARGET_CHUNKS,
            PortalLodRenderPolicy.decide(aether, overworld, scope, nativeFramebufferAvailable = false),
        )
    }
}
