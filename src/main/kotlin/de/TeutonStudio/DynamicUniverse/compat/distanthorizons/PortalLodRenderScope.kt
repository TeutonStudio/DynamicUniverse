package de.TeutonStudio.DynamicUniverse.compat.distanthorizons

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING
import org.lwjgl.opengl.GL30.glGetInteger
import java.lang.reflect.Method

/**
 * Snapshot of an Immersive Portals world render which is safe to hand to a client-only
 * Distant-Horizons adapter. The snapshot deliberately contains no IP types: DH must still be
 * usable in installations that do not provide IP.
 */
internal data class PortalLodRenderScope(
    val sourceDimension: DimensionId?,
    val targetDimension: DimensionId,
    val recursionDepth: Int,
    val frameBufferId: Int,
    val hasStencil: Boolean,
)

internal enum class PortalLodRenderDecision {
    OUTER_WORLD,
    NATIVE_PORTAL_LOD,
    VANILLA_TARGET_CHUNKS,
}

/** Keeps the policy testable without loading either optional renderer. */
internal object PortalLodRenderPolicy {
    fun decide(
        renderedDimension: DimensionId,
        viewerDimension: DimensionId?,
        scope: PortalLodRenderScope?,
        nativeFramebufferAvailable: Boolean,
    ): PortalLodRenderDecision {
        if (scope == null) {
            return if (renderedDimension == viewerDimension) {
                PortalLodRenderDecision.OUTER_WORLD
            } else {
                PortalLodRenderDecision.VANILLA_TARGET_CHUNKS
            }
        }

        // IP temporarily renders a different ClientLevel while the real player remains in the
        // source level. Comparing with player.level() here would reject every legitimate target
        // LOD render, which was the source of the one-sided-looking Aether/Overworld seam.
        if (renderedDimension != scope.targetDimension) return PortalLodRenderDecision.VANILLA_TARGET_CHUNKS
        if (!nativeFramebufferAvailable || !scope.hasStencil || scope.frameBufferId <= 0) {
            return PortalLodRenderDecision.VANILLA_TARGET_CHUNKS
        }
        return PortalLodRenderDecision.NATIVE_PORTAL_LOD
    }
}

/**
 * Reflective IP boundary. Keep this in the DH compat source-set so neither common code nor a
 * normal client has a linkage requirement on Immersive Portals.
 */
internal object ImmersivePortalRenderScopeReader {
    private val api: ImmersivePortalRenderApi? by lazy {
        runCatching {
            val type = Class.forName(
                "qouteall.imm_ptl.core.render.context_management.PortalRendering",
                false,
                javaClass.classLoader,
            )
            ImmersivePortalRenderApi(
                isRendering = type.getMethod("isRendering"),
                getRenderingPortal = type.getMethod("getRenderingPortal"),
                getPortalLayer = type.getMethod("getPortalLayer"),
            )
        }.getOrNull()
    }

    fun current(): PortalLodRenderScope? = runCatching {
        val portalApi = api ?: return null
        if (portalApi.isRendering.invoke(null) != true) return null

        val portal = portalApi.getRenderingPortal.invoke(null) ?: return null
        val targetDimension = dimensionId(portal.javaClass.getMethod("getDestDim").invoke(portal)) ?: return null
        val sourceWorld = portal.javaClass.getMethod("getOriginWorld").invoke(portal)
        val sourceDimension = dimensionId(sourceWorld?.javaClass?.getMethod("dimension")?.invoke(sourceWorld))
        val depth = portalApi.getPortalLayer.invoke(null) as Int
        val renderTarget = Minecraft.getInstance().mainRenderTarget
        val activeFrameBuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)
        PortalLodRenderScope(
            sourceDimension = sourceDimension,
            targetDimension = targetDimension,
            recursionDepth = depth,
            frameBufferId = activeFrameBuffer,
            // A foreign shader FBO cannot be assumed to have the main target's combined
            // depth/stencil attachment. Fallback until it exposes an explicit compatible path.
            hasStencil = renderTarget.isStencilEnabled && activeFrameBuffer == renderTarget.frameBufferId,
        )
    }.getOrNull()

    private fun dimensionId(key: Any?): DimensionId? = runCatching {
        val location = key?.javaClass?.getMethod("location")?.invoke(key)?.toString() ?: return null
        DimensionId(location)
    }.getOrNull()

    private data class ImmersivePortalRenderApi(
        val isRendering: Method,
        val getRenderingPortal: Method,
        val getPortalLayer: Method,
    )
}
