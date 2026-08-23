package de.TeutonStudio.DynamicUniverse.compat.immersiveportals

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.runtime.AperturePortalAdapter
import de.TeutonStudio.DynamicUniverse.runtime.BedrockBoundaryPlane
import de.TeutonStudio.DynamicUniverse.runtime.CoreApertureProjection
import de.TeutonStudio.DynamicUniverse.runtime.CoreBoundaryAperture
import de.TeutonStudio.DynamicUniverse.runtime.CoreShellFace
import de.TeutonStudio.DynamicUniverse.runtime.PairedBoundaryAperture
import de.TeutonStudio.DynamicUniverse.runtime.PlanetCoreGeometry
import de.TeutonStudio.DynamicUniverse.runtime.PlanetCoreProjectionResolver
import de.TeutonStudio.DynamicUniverse.runtime.UniverseGeometryManifest
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import qouteall.imm_ptl.core.api.PortalAPI
import qouteall.imm_ptl.core.portal.Portal
import qouteall.q_misc_util.my_util.DQuaternion

/**
 * Materializes each aperture cell independently. This deliberately favors exact arbitrary shapes
 * over entity-count optimization; adjacent cells can later be coalesced without changing the
 * persisted aperture model.
 */
class ImmersivePortalApertureAdapter : AperturePortalAdapter {
    private val coreResolver = PlanetCoreProjectionResolver()

    override fun rebuildPaired(
        server: MinecraftServer,
        manifest: UniverseGeometryManifest,
        planes: Collection<BedrockBoundaryPlane>,
        aperture: PairedBoundaryAperture,
    ) {
        removeExisting(server, aperture.id)
        val connection = manifest.links.singleOrNull { it.id == aperture.connectionId } ?: return
        val sourceLevel = server.level(connection.source) ?: return
        val targetLevel = server.level(connection.target) ?: return
        val sourcePlane = planes.singleOrNull { it.dimension == connection.source && it.face == connection.sourceBoundaryFace } ?: return
        val targetPlane = planes.singleOrNull { it.dimension == connection.target && it.face == connection.targetBoundaryFace } ?: return
        val sourceOrientation = boundaryOrientation(connection.sourceBoundaryFace)
        val targetOrientation = boundaryOrientation(connection.targetBoundaryFace)

        aperture.shape.cells.forEach { cell ->
            val sourceCenter = Vec3(
                aperture.sourceAnchor.x + cell.dx + 0.5,
                sourcePlane.y + 0.5,
                aperture.sourceAnchor.z + cell.dz + 0.5,
            )
            val targetCenter = Vec3(
                aperture.targetAnchor.x + cell.dx + 0.5,
                targetPlane.y + 0.5,
                aperture.targetAnchor.z + cell.dz + 0.5,
            )
            materializeCellPair(
                sourceLevel,
                targetLevel,
                sourceCenter,
                targetCenter,
                sourceOrientation,
                targetOrientation,
                aperture.id,
            )
        }
    }

    override fun rebuildCore(
        server: MinecraftServer,
        manifest: UniverseGeometryManifest,
        planes: Collection<BedrockBoundaryPlane>,
        geometry: PlanetCoreGeometry,
        aperture: CoreBoundaryAperture,
        projection: CoreApertureProjection,
    ) {
        removeExisting(server, aperture.id)
        val deepLevel = server.level(aperture.deepDimension) ?: return
        val coreLevel = server.level(geometry.coreDimension) ?: return
        val deepPlane = planes.singleOrNull { it.dimension == aperture.deepDimension && it.face == aperture.deepFace } ?: return
        val sourceOrientation = boundaryOrientation(aperture.deepFace)

        projection.mapping.forEach { (cell, coreCell) ->
            val targetBlock = coreResolver.blockPosition(geometry, coreCell) ?: return@forEach
            val sourceCenter = Vec3(
                aperture.deepAnchor.x + cell.dx + 0.5,
                deepPlane.y + 0.5,
                aperture.deepAnchor.z + cell.dz + 0.5,
            )
            val targetCenter = Vec3(targetBlock.x + 0.5, targetBlock.y + 0.5, targetBlock.z + 0.5)
            val targetOrientation = coreOrientation(coreCell.face, projection.rotationQuarterTurns)
            materializeCellPair(
                deepLevel,
                coreLevel,
                sourceCenter,
                targetCenter,
                sourceOrientation,
                targetOrientation,
                aperture.id,
            )
        }
    }

    private fun materializeCellPair(
        sourceLevel: ServerLevel,
        targetLevel: ServerLevel,
        sourceCenter: Vec3,
        targetCenter: Vec3,
        sourceOrientation: DQuaternion,
        targetOrientation: DQuaternion,
        apertureId: String,
    ) {
        val forwardRotation = targetOrientation.hamiltonProduct(sourceOrientation.getConjugated())
        val reverseRotation = sourceOrientation.hamiltonProduct(targetOrientation.getConjugated())
        val forward = Portal(Portal.ENTITY_TYPE, sourceLevel)
        PortalAPI.setPortalPositionOrientationAndSize(forward, sourceCenter, sourceOrientation, 1.0, 1.0)
        PortalAPI.setPortalTransformation(forward, targetLevel.dimension(), targetCenter, forwardRotation, 1.0)
        forward.portalTag = "dynamicuniverse:aperture:$apertureId"
        PortalAPI.spawnServerEntity(forward)

        val reverse = Portal(Portal.ENTITY_TYPE, targetLevel)
        PortalAPI.setPortalPositionOrientationAndSize(reverse, targetCenter, targetOrientation, 1.0, 1.0)
        PortalAPI.setPortalTransformation(reverse, sourceLevel.dimension(), sourceCenter, reverseRotation, 1.0)
        reverse.portalTag = "dynamicuniverse:aperture:$apertureId:reverse"
        PortalAPI.spawnServerEntity(reverse)
    }

    private fun removeExisting(server: MinecraftServer, apertureId: String) {
        val forwardTag = "dynamicuniverse:aperture:$apertureId"
        val reverseTag = "$forwardTag:reverse"
        server.allLevels.forEach { level ->
            level.getAllEntities()
                .filterIsInstance<Portal>()
                .filter { portal -> portal.portalTag == forwardTag || portal.portalTag == reverseTag }
                .forEach(Portal::discard)
        }
    }

    private fun boundaryOrientation(face: DimensionBoundaryFace): DQuaternion = when (face) {
        DimensionBoundaryFace.LOWER -> DQuaternion.rotateByDegrees(Vec3(1.0, 0.0, 0.0), 90.0)
        DimensionBoundaryFace.UPPER -> DQuaternion.rotateByDegrees(Vec3(1.0, 0.0, 0.0), -90.0)
    }

    private fun coreOrientation(face: CoreShellFace, quarterTurns: Int): DQuaternion {
        val faceRotation = when (face) {
            CoreShellFace.POSITIVE_Z -> DQuaternion.identity
            CoreShellFace.NEGATIVE_Z -> DQuaternion.rotateByDegrees(Vec3(0.0, 1.0, 0.0), 180.0)
            CoreShellFace.POSITIVE_X -> DQuaternion.rotateByDegrees(Vec3(0.0, 1.0, 0.0), 90.0)
            CoreShellFace.NEGATIVE_X -> DQuaternion.rotateByDegrees(Vec3(0.0, 1.0, 0.0), -90.0)
            CoreShellFace.POSITIVE_Y -> DQuaternion.rotateByDegrees(Vec3(1.0, 0.0, 0.0), -90.0)
            CoreShellFace.NEGATIVE_Y -> DQuaternion.rotateByDegrees(Vec3(1.0, 0.0, 0.0), 90.0)
        }
        val inPlane = DQuaternion.rotateByDegrees(Vec3(0.0, 0.0, 1.0), quarterTurns * 90.0)
        return faceRotation.hamiltonProduct(inPlane)
    }
}

private fun MinecraftServer.level(dimension: DimensionId): ServerLevel? {
    val location = ResourceLocation.tryParse(dimension.value) ?: return null
    return getLevel(ResourceKey.create(Registries.DIMENSION, location))
}
