package de.TeutonStudio.DynamicUniverse.runtime

import com.mojang.logging.LogUtils
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import net.minecraft.server.MinecraftServer
import net.neoforged.fml.ModList

/** Optional visible materialization of vertical boundaries; the entity listener is the non-IP fallback. */
interface VerticalBoundaryPortalAdapter {
    fun install(server: MinecraftServer, manifest: UniverseGeometryManifest): Set<DimensionId>
    fun clear(server: MinecraftServer?)
}

object VerticalBoundaryPortalRuntime {
    private val logger = LogUtils.getLogger()
    @Volatile private var adapter: VerticalBoundaryPortalAdapter? = null
    @Volatile private var portalHandledDimensions: Set<DimensionId> = emptySet()

    fun install(server: MinecraftServer, manifest: UniverseGeometryManifest) {
        portalHandledDimensions = runCatching { adapter()?.install(server, manifest).orEmpty() }
            .onFailure { logger.error("DynamicUniverse could not install vertical Immersive Portals boundaries.", it) }
            .getOrDefault(emptySet())
    }

    fun usesPortal(dimension: DimensionId): Boolean = dimension in portalHandledDimensions

    fun clear(server: MinecraftServer? = null) {
        runCatching { adapter?.clear(server) }
            .onFailure { logger.error("DynamicUniverse could not remove vertical Immersive Portals boundaries.", it) }
        adapter = null
        portalHandledDimensions = emptySet()
    }

    private fun adapter(): VerticalBoundaryPortalAdapter? {
        adapter?.let { return it }
        if (!ModList.get().isLoaded("immersive_portals_core")) return null
        return runCatching {
            val type = Class.forName("de.TeutonStudio.DynamicUniverse.compat.immersiveportals.ImmersivePortalVerticalBoundaryAdapter")
            (type.getDeclaredConstructor().newInstance() as VerticalBoundaryPortalAdapter).also { adapter = it }
        }.getOrNull()
    }
}
