package de.TeutonStudio.DynamicUniverse.runtime

import com.mojang.logging.LogUtils
import net.minecraft.server.MinecraftServer
import net.neoforged.fml.ModList

/** Optional portal rendering/materialization stays behind a common-code interface. */
interface AperturePortalAdapter {
    fun rebuildPaired(
        server: MinecraftServer,
        manifest: UniverseGeometryManifest,
        planes: Collection<BedrockBoundaryPlane>,
        aperture: PairedBoundaryAperture,
    )

    fun rebuildCore(
        server: MinecraftServer,
        manifest: UniverseGeometryManifest,
        planes: Collection<BedrockBoundaryPlane>,
        geometry: PlanetCoreGeometry,
        aperture: CoreBoundaryAperture,
        projection: CoreApertureProjection,
    )

    fun remove(server: MinecraftServer, apertureIds: Collection<String>)

    fun prune(server: MinecraftServer, validApertureIds: Set<String>)
}

object AperturePortalRuntime {
    private val logger = LogUtils.getLogger()
    @Volatile
    private var adapter: AperturePortalAdapter? = null

    @Volatile
    private var planes: List<BedrockBoundaryPlane> = emptyList()

    fun install(planes: Collection<BedrockBoundaryPlane>) {
        this.planes = planes.toList()
    }

    private fun adapter(): AperturePortalAdapter? {
        adapter?.let { return it }
        if (!ModList.get().isLoaded("immersive_portals_core")) return null
        return runCatching {
            val type = Class.forName(
                "de.TeutonStudio.DynamicUniverse.compat.immersiveportals.ImmersivePortalApertureAdapter",
            )
            (type.getDeclaredConstructor().newInstance() as AperturePortalAdapter).also { adapter = it }
        }.onFailure { error ->
            logger.error("DynamicUniverse could not initialize the Immersive Portals aperture adapter.", error)
        }.getOrNull()
    }

    fun clear() {
        adapter = null
        planes = emptyList()
    }

    fun remove(server: MinecraftServer, apertureIds: Collection<String>) {
        if (apertureIds.isEmpty()) return
        attempt("remove ${apertureIds.size} aperture portal(s)") { adapter()?.remove(server, apertureIds) }
    }

    fun prune(server: MinecraftServer, validApertureIds: Set<String>) {
        attempt("prune aperture portals") { adapter()?.prune(server, validApertureIds) }
    }

    fun rebuildPaired(
        server: MinecraftServer,
        manifest: UniverseGeometryManifest,
        aperture: PairedBoundaryAperture,
    ) {
        attempt("rebuild paired aperture ${aperture.id}") { adapter()?.rebuildPaired(server, manifest, planes, aperture) }
    }

    fun rebuildCore(
        server: MinecraftServer,
        manifest: UniverseGeometryManifest,
        geometry: PlanetCoreGeometry,
        aperture: CoreBoundaryAperture,
        projection: CoreApertureProjection,
    ) {
        attempt("rebuild core aperture ${aperture.id}") {
            adapter()?.rebuildCore(server, manifest, planes, geometry, aperture, projection)
        }
    }

    private inline fun attempt(operation: String, action: () -> Unit) {
        runCatching(action).onFailure { error ->
            logger.error("DynamicUniverse failed to $operation.", error)
        }
    }
}
