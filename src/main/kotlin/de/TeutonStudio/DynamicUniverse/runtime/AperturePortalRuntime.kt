package de.TeutonStudio.DynamicUniverse.runtime

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
}

object AperturePortalRuntime {
    @Volatile
    private var adapter: AperturePortalAdapter? = null

    private fun adapter(): AperturePortalAdapter? {
        adapter?.let { return it }
        if (!ModList.get().isLoaded("immersive_portals_core")) return null
        return runCatching {
            val type = Class.forName(
                "de.TeutonStudio.DynamicUniverse.compat.immersiveportals.ImmersivePortalApertureAdapter",
            )
            (type.getDeclaredConstructor().newInstance() as AperturePortalAdapter).also { adapter = it }
        }.getOrNull()
    }

    fun clear() {
        adapter = null
    }

    fun rebuildPaired(
        server: MinecraftServer,
        manifest: UniverseGeometryManifest,
        planes: Collection<BedrockBoundaryPlane>,
        aperture: PairedBoundaryAperture,
    ) {
        adapter()?.rebuildPaired(server, manifest, planes, aperture)
    }

    fun rebuildCore(
        server: MinecraftServer,
        manifest: UniverseGeometryManifest,
        planes: Collection<BedrockBoundaryPlane>,
        geometry: PlanetCoreGeometry,
        aperture: CoreBoundaryAperture,
        projection: CoreApertureProjection,
    ) {
        adapter()?.rebuildCore(server, manifest, planes, geometry, aperture, projection)
    }
}
