package de.TeutonStudio.DynamicUniverse.runtime

import com.mojang.logging.LogUtils
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import net.minecraft.server.MinecraftServer
import net.neoforged.fml.ModList

/** Optional visible materialization of vertical boundaries; the entity listener is the non-IP fallback. */
interface VerticalBoundaryPortalAdapter {
    fun install(server: MinecraftServer, manifest: UniverseGeometryManifest): VerticalBoundaryPortalInstallation
    fun clear(server: MinecraftServer?)
}

/** Runtime-only result of portal materialization; it is not part of the save format. */
data class VerticalBoundaryPortalInstallation(
    val handledDimensions: Set<DimensionId> = emptySet(),
    val seamlessTransitions: Set<VerticalBoundaryPortalTransition> = emptySet(),
)

data class VerticalBoundaryPortalTransition(val source: DimensionId, val target: DimensionId)

object VerticalBoundaryPortalRuntime {
    private val logger = LogUtils.getLogger()
    @Volatile private var adapter: VerticalBoundaryPortalAdapter? = null
    @Volatile private var installation = VerticalBoundaryPortalInstallation()

    fun install(server: MinecraftServer, manifest: UniverseGeometryManifest) {
        installation = runCatching { adapter()?.install(server, manifest) ?: VerticalBoundaryPortalInstallation() }
            .onFailure { logger.error("DynamicUniverse could not install vertical Immersive Portals boundaries.", it) }
            .getOrDefault(VerticalBoundaryPortalInstallation())
    }

    fun usesPortal(dimension: DimensionId): Boolean = dimension in installation.handledDimensions

    fun usesPortalTransition(source: DimensionId, target: DimensionId): Boolean =
        VerticalBoundaryPortalTransition(source, target) in installation.seamlessTransitions

    fun clear(server: MinecraftServer? = null) {
        runCatching { adapter?.clear(server) }
            .onFailure { logger.error("DynamicUniverse could not remove vertical Immersive Portals boundaries.", it) }
        adapter = null
        installation = VerticalBoundaryPortalInstallation()
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
