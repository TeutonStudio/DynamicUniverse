package de.TeutonStudio.DynamicUniverse.compat.distanthorizons

import com.mojang.logging.LogUtils
import com.seibel.distanthorizons.api.DhApi
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeDeferredRenderEvent
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam
import com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiFramebuffer
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL30.glBindFramebuffer

/**
 * DH's normal full-screen composite cannot be allowed in an IP sub-render. On the native
 * OpenGL renderer we instead hand DH IP's current color/depth/stencil target. That keeps the
 * existing IP stencil mask authoritative, so target LOD geometry cannot escape the aperture.
 * Other engines deliberately retain vanilla target chunks rather than risking a full-screen LOD
 * composite.
 */
object DistantHorizonsStackRenderAdapter {
    private val logger = LogUtils.getLogger()
    @Volatile private var installed = false

    @JvmStatic
    fun install() {
        if (installed) return
        DhApiEventRegister.on(DhApiBeforeRenderEvent::class.java, ForeignWorldLodSuppressor())
        DhApiEventRegister.on(DhApiBeforeDeferredRenderEvent::class.java, ForeignWorldDeferredLodSuppressor())
        runCatching {
            DhApi.overrides.bind(IDhApiFramebuffer::class.java, ImmersivePortalDhFramebuffer)
        }.onFailure { error ->
            logger.warn("DynamicUniverse could not install the native DH portal framebuffer; portal LODs stay disabled.", error)
        }
        installed = true
        logger.info("DynamicUniverse installed the Distant Horizons portal-aware render adapter.")
    }
}

private class ForeignWorldLodSuppressor : DhApiBeforeRenderEvent() {
    override fun beforeRender(event: DhApiCancelableEventParam<DhApiRenderParam>) {
        if (!mayRenderLods(event.value)) event.cancelEvent()
    }
}

private class ForeignWorldDeferredLodSuppressor : DhApiBeforeDeferredRenderEvent() {
    override fun beforeRender(event: DhApiCancelableEventParam<DhApiRenderParam>) {
        if (!mayRenderLods(event.value)) event.cancelEvent()
    }
}

private fun mayRenderLods(render: DhApiRenderParam): Boolean {
    val rendered = runCatching { DimensionId(render.clientLevelWrapper.dimensionName) }.getOrNull() ?: return false
    val viewer = Minecraft.getInstance().player?.let { player ->
        DimensionId(player.level().dimension().location().toString())
    }
    return PortalLodRenderPolicy.decide(
        renderedDimension = rendered,
        viewerDimension = viewer,
        scope = ImmersivePortalRenderScopeReader.current(),
        nativeFramebufferAvailable = NativePortalFramebufferCapability.isAvailable(),
    ) != PortalLodRenderDecision.VANILLA_TARGET_CHUNKS
}

/**
 * DH 3.2's framebuffer override is only honoured by its native OpenGL renderer. This probe is
 * intentionally reflective because the public facade changed between DH beta lines.
 */
private object NativePortalFramebufferCapability {
    @Volatile private var knownNativeAvailability = false

    fun isAvailable(): Boolean {
        if (knownNativeAvailability) return true
        return runCatching {
            val proxyType = Class.forName("com.seibel.distanthorizons.core.render.DhApiRenderProxy")
            val proxy = proxyType.getField("INSTANCE").get(null)
            val api = proxyType.getMethod("getRenderingApi").invoke(proxy).toString()
            (api == "OPEN_GL" && proxyType.getMethod("isNativeRenderer").invoke(proxy) == true)
                .also { available -> knownNativeAvailability = available }
        }.getOrDefault(false)
    }
}

/**
 * A transient bridge to Minecraft's active portal render target. DH calls attachment methods
 * while constructing its normal framebuffer; no-op implementations intentionally preserve IP's
 * already-created color/depth/stencil attachments.
 */
private object ImmersivePortalDhFramebuffer : IDhApiFramebuffer {
    override fun overrideThisFrame(): Boolean {
        val scope = ImmersivePortalRenderScopeReader.current() ?: return false
        return scope.hasStencil && scope.frameBufferId > 0 && NativePortalFramebufferCapability.isAvailable()
    }

    override fun bind() {
        ImmersivePortalRenderScopeReader.current()?.let { scope ->
            glBindFramebuffer(GL_FRAMEBUFFER, scope.frameBufferId)
        }
    }

    override fun addDepthAttachment(textureId: Int, isCombinedStencil: Boolean) = Unit

    override fun addColorAttachment(textureIndex: Int, textureId: Int) = Unit

    override fun destroy() = Unit

    override fun getId(): Int = ImmersivePortalRenderScopeReader.current()?.frameBufferId ?: 0

    override fun getStatus(): Int = GL_FRAMEBUFFER_COMPLETE

    override fun getPriority(): Int = 100
}
