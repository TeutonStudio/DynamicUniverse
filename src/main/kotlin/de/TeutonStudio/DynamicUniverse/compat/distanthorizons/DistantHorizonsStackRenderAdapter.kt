package de.TeutonStudio.DynamicUniverse.compat.distanthorizons

import com.mojang.logging.LogUtils
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeDeferredRenderEvent
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import net.minecraft.client.Minecraft

/**
 * Distant Horizons owns a full-screen color/depth composite. Immersive Portals renders a
 * destination level recursively, but DH's public events do not expose that portal's stencil,
 * framebuffer, or frustum. Rendering a target LOD there would therefore affect pixels outside
 * the aperture. Until a stencil-aware integration exists, portal sub-renders intentionally use
 * vanilla target chunks only.
 */
object DistantHorizonsStackRenderAdapter {
    private val logger = LogUtils.getLogger()
    @Volatile private var installed = false

    @JvmStatic
    fun install() {
        if (installed) return
        DhApiEventRegister.on(DhApiBeforeRenderEvent::class.java, ForeignWorldLodSuppressor())
        DhApiEventRegister.on(DhApiBeforeDeferredRenderEvent::class.java, ForeignWorldDeferredLodSuppressor())
        installed = true
        logger.info("DynamicUniverse installed the Distant Horizons portal-safe render adapter.")
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
    if (isPortalSubRender()) return false
    val rendered = runCatching { DimensionId(render.clientLevelWrapper.dimensionName) }.getOrNull() ?: return false
    val viewer = Minecraft.getInstance().player?.let { player ->
        DimensionId(player.level().dimension().location().toString())
    } ?: return false
    return rendered == viewer
}

/** Reflection keeps DH usable when Immersive Portals is not installed. */
private fun isPortalSubRender(): Boolean = runCatching {
    val portalRendering = Class.forName("qouteall.imm_ptl.core.render.context_management.PortalRendering")
    portalRendering.getMethod("isRendering").invoke(null) == true
}.getOrDefault(false)
