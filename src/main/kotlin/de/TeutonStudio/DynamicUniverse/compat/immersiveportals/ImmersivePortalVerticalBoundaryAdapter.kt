package de.TeutonStudio.DynamicUniverse.compat.immersiveportals

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.runtime.UniverseGeometryManifest
import de.TeutonStudio.DynamicUniverse.runtime.VerticalBoundaryPortalAdapter
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal

/**
 * One global horizontal portal covers each practical world border. It keeps the vertical
 * dimension stack visible and traversable without the vanilla dimension-change screen.
 */
class ImmersivePortalVerticalBoundaryAdapter : VerticalBoundaryPortalAdapter {
    override fun install(server: MinecraftServer, manifest: UniverseGeometryManifest): Set<DimensionId> {
        val handled = linkedSetOf<DimensionId>()
        val host = DimensionId(manifest.universe.id)
        server.level(host)?.let { level ->
            val tag = "$TAG_PREFIX/host"
            removeTagged(level, tag)
            addLoop(level, tag)
            handled += host
        }
        manifest.isolatedUniverses.forEach { isolated ->
            server.level(isolated.dimension)?.let { level ->
                val tag = "$TAG_PREFIX/isolated/${isolated.id}"
                removeTagged(level, tag)
                addLoop(level, tag)
                handled += isolated.dimension
            }
        }
        manifest.verticalSeams.forEach { seam ->
            val lower = server.level(seam.lowerDimension) ?: return@forEach
            val upper = server.level(seam.upperDimension) ?: return@forEach
            // Portal scaling changes physical entity scale; vertical seams intentionally keep it 1:1.
            if (seam.coordinateScale != de.TeutonStudio.DynamicUniverse.dimension.DimensionScale.ONE) return@forEach
            val tag = "$TAG_PREFIX/seam/${seam.id}"
            removeTagged(lower, tag)
            removeTagged(upper, tag)
            addPortal(lower, VerticalConnectingPortal.ConnectorType.ceil, upper, "$tag/up")
            addPortal(upper, VerticalConnectingPortal.ConnectorType.floor, lower, "$tag/down")
            handled += seam.lowerDimension
            handled += seam.upperDimension
        }
        return handled
    }

    override fun clear(server: MinecraftServer?) {
        server?.allLevels?.forEach(::removeTagged)
    }

    private fun addLoop(level: ServerLevel, tag: String) {
        addPortal(level, VerticalConnectingPortal.ConnectorType.ceil, level, "$tag/up")
        addPortal(level, VerticalConnectingPortal.ConnectorType.floor, level, "$tag/down")
    }

    private fun addPortal(
        source: ServerLevel,
        connector: VerticalConnectingPortal.ConnectorType,
        target: ServerLevel,
        tag: String,
    ) {
        val portal = VerticalConnectingPortal.createConnectingPortal(
            source,
            connector,
            target,
            1.0,
            false,
            0.0,
            source.minBuildHeight,
            source.maxBuildHeight,
            target.minBuildHeight,
            target.maxBuildHeight,
        )
        portal.portalTag = tag
        GlobalPortalStorage.get(source).addPortal(portal)
    }

    private fun removeTagged(level: ServerLevel, tagPrefix: String = TAG_PREFIX) {
        val storage = GlobalPortalStorage.get(level)
        storage.data.filter { it.portalTag?.startsWith(tagPrefix) == true }.toList().forEach(storage::removePortal)
    }

    private fun MinecraftServer.level(id: DimensionId): ServerLevel? {
        val location = ResourceLocation.tryParse(id.value) ?: return null
        return getLevel(ResourceKey.create(Registries.DIMENSION, location))
    }

    companion object {
        private const val TAG_PREFIX = "dynamicuniverse:vertical-boundary"
    }
}
