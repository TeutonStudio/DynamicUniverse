package de.TeutonStudio.DynamicUniverse.compat.immersiveportals

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.runtime.UniverseGeometryManifest
import de.TeutonStudio.DynamicUniverse.runtime.VerticalBoundaryPortalAdapter
import de.TeutonStudio.DynamicUniverse.runtime.VerticalBoundaryPortalInstallation
import de.TeutonStudio.DynamicUniverse.runtime.VerticalBoundaryPortalPlan
import de.TeutonStudio.DynamicUniverse.runtime.VerticalBoundaryPortalPlans
import de.TeutonStudio.DynamicUniverse.runtime.VerticalBoundaryPortalTransition
import de.TeutonStudio.DynamicUniverse.runtime.VerticalPortalLevelBounds
import de.TeutonStudio.DynamicUniverse.runtime.VerticalPortalSurface
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
    override fun install(server: MinecraftServer, manifest: UniverseGeometryManifest): VerticalBoundaryPortalInstallation {
        val handled = linkedSetOf<DimensionId>()
        val seamlessTransitions = linkedSetOf<VerticalBoundaryPortalTransition>()
        // Global portal entities are derived state. Remove all legacy/current instances before
        // rebuilding, including the pre-v2 portals that used the exact Aether lower bound.
        server.allLevels.forEach(::removeTagged)
        // The UniverseSpace host is deliberately absent here. Its native Minecraft build range
        // is hidden by bubble rebasing; adding global floor/ceiling portals would turn R³ back
        // into the End-style vertical cycle that UniverseSpace explicitly does not have.
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
            VerticalBoundaryPortalPlans.seam(
                seam.lowerDimension,
                lower.bounds(),
                seam.upperDimension,
                upper.bounds(),
            ).forEachIndexed { index, plan ->
                addPortal(server.requireLevel(plan.source), plan, "$tag/${if (index == 0) "up" else "down"}")
                seamlessTransitions += VerticalBoundaryPortalTransition(plan.source, plan.target)
            }
            handled += seam.lowerDimension
            handled += seam.upperDimension
        }
        return VerticalBoundaryPortalInstallation(handled, seamlessTransitions)
    }

    override fun clear(server: MinecraftServer?) {
        server?.allLevels?.forEach(::removeTagged)
    }

    private fun addLoop(level: ServerLevel, tag: String) {
        VerticalBoundaryPortalPlans.loop(DimensionId(level.dimension().location().toString()), level.bounds())
            .forEachIndexed { index, plan -> addPortal(level, plan, "$tag/${if (index == 0) "up" else "down"}") }
    }

    private fun addPortal(
        source: ServerLevel,
        plan: VerticalBoundaryPortalPlan,
        tag: String,
    ) {
        val target = source.server.requireLevel(plan.target)
        val targetBounds = plan.nativeTargetBounds()
        val portal = VerticalConnectingPortal.createConnectingPortal(
            source,
            plan.sourceSurface.connectorType(),
            target,
            1.0,
            false,
            0.0,
            plan.sourceBounds.minY,
            plan.sourceBounds.maxYExclusive,
            targetBounds.minY,
            targetBounds.maxYExclusive,
        )
        portal.portalTag = tag
        GlobalPortalStorage.get(source).addPortal(portal)
    }

    private fun removeTagged(level: ServerLevel, tagPrefix: String = TAG_ROOT) {
        val storage = GlobalPortalStorage.get(level)
        storage.data.filter { it.portalTag?.startsWith(tagPrefix) == true }.toList().forEach(storage::removePortal)
    }

    private fun MinecraftServer.level(id: DimensionId): ServerLevel? {
        val location = ResourceLocation.tryParse(id.value) ?: return null
        return getLevel(ResourceKey.create(Registries.DIMENSION, location))
    }

    private fun MinecraftServer.requireLevel(id: DimensionId): ServerLevel =
        requireNotNull(level(id)) { "Portal plan references an unavailable level: ${id.value}" }

    private fun ServerLevel.bounds() = VerticalPortalLevelBounds(minBuildHeight, maxBuildHeight)

    private fun VerticalPortalSurface.connectorType() = when (this) {
        VerticalPortalSurface.CEILING -> VerticalConnectingPortal.ConnectorType.ceil
        VerticalPortalSurface.FLOOR -> VerticalConnectingPortal.ConnectorType.floor
    }

    companion object {
        // Removing the root also migrates all pre-v2, incorrectly placed portal instances.
        private const val TAG_ROOT = "dynamicuniverse:vertical-boundary"
        private const val TAG_PREFIX = "$TAG_ROOT/v2"
    }
}
