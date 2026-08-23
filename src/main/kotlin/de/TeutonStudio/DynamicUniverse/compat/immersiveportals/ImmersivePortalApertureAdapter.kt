package de.TeutonStudio.DynamicUniverse.compat.immersiveportals

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.runtime.ApertureCell
import de.TeutonStudio.DynamicUniverse.runtime.AperturePortalAdapter
import de.TeutonStudio.DynamicUniverse.runtime.BedrockBoundaryPlane
import de.TeutonStudio.DynamicUniverse.runtime.CoreApertureProjection
import de.TeutonStudio.DynamicUniverse.runtime.CoreBoundaryAperture
import de.TeutonStudio.DynamicUniverse.runtime.CoreShellFace
import de.TeutonStudio.DynamicUniverse.runtime.PairedBoundaryAperture
import de.TeutonStudio.DynamicUniverse.runtime.PlanetCoreGeometry
import de.TeutonStudio.DynamicUniverse.runtime.PlanetCoreProjectionResolver
import de.TeutonStudio.DynamicUniverse.runtime.UniverseGeometryManifest
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPosition
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import qouteall.imm_ptl.core.api.PortalAPI
import qouteall.imm_ptl.core.portal.Portal
import qouteall.q_misc_util.my_util.DQuaternion
import kotlin.math.cos
import kotlin.math.sin

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
        val sourcePeriod = manifest.period(connection.source) ?: return
        val targetPeriod = manifest.period(connection.target) ?: return
        val sourcePlane = planes.singleOrNull { it.dimension == connection.source && it.face == connection.sourceBoundaryFace } ?: return
        val targetPlane = planes.singleOrNull { it.dimension == connection.target && it.face == connection.targetBoundaryFace } ?: return
        val sourceOrientation = boundaryOrientation(connection.sourceBoundaryFace)
        val targetOrientation = boundaryOrientation(connection.targetBoundaryFace)

        aperture.shape.cells.forEach { cell ->
            val source = worldCell(sourcePeriod, aperture.sourceAnchor, cell)
            val target = worldCell(targetPeriod, aperture.targetAnchor, cell)
            val sourceCenter = boundaryCenter(source, sourcePlane)
            val targetCenter = boundaryCenter(target, targetPlane)
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
        val deepPeriod = manifest.period(aperture.deepDimension) ?: return
        val deepPlane = planes.singleOrNull { it.dimension == aperture.deepDimension && it.face == aperture.deepFace } ?: return
        val sourceOrientation = boundaryOrientation(aperture.deepFace)

        projection.mapping.forEach { (cell, coreCell) ->
            val targetBlock = coreResolver.blockPosition(geometry, coreCell) ?: return@forEach
            val source = worldCell(deepPeriod, aperture.deepAnchor, cell)
            val sourceCenter = boundaryCenter(source, deepPlane)
            val targetCenter = coreCenter(targetBlock, coreCell.face)
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

    override fun remove(server: MinecraftServer, apertureIds: Collection<String>) {
        apertureIds.forEach { apertureId -> removeExisting(server, apertureId) }
    }

    override fun prune(server: MinecraftServer, validApertureIds: Set<String>) {
        server.allLevels.forEach { level ->
            level.getAllEntities()
                .filterIsInstance<Portal>()
                .filter { portal ->
                    val apertureId = portal.portalTag?.apertureIdOrNull()
                    apertureId != null && apertureId !in validApertureIds
                }
                .forEach { portal -> portal.discard() }
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
        val forwardRotation = multiply(targetOrientation, conjugate(sourceOrientation))
        val reverseRotation = multiply(sourceOrientation, conjugate(targetOrientation))
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
                .forEach { portal -> portal.discard() }
        }
    }

    private fun boundaryOrientation(face: DimensionBoundaryFace): DQuaternion = when (face) {
        // A lower boundary must be visible from inside the level, i.e. from above. The
        // previous signs exposed the back face, so crossing upwards entered on top of the
        // counterpart Bedrock rather than through its opening.
        DimensionBoundaryFace.LOWER -> axisAngle(1.0, 0.0, 0.0, -90.0)
        DimensionBoundaryFace.UPPER -> axisAngle(1.0, 0.0, 0.0, 90.0)
    }

    /** The portal lies on the exposed face, never in the middle of the Bedrock block. */
    private fun boundaryCenter(position: HorizontalPosition, plane: BedrockBoundaryPlane): Vec3 = Vec3(
        position.x + 0.5,
        plane.y + when (plane.face) {
            DimensionBoundaryFace.LOWER -> 0.5
            DimensionBoundaryFace.UPPER -> -0.5
        },
        position.z + 0.5,
    )

    /** A core aperture exits on the *inside* of the materialized shell, not in exterior void. */
    private fun coreCenter(position: net.minecraft.core.BlockPos, face: CoreShellFace): Vec3 {
        val inward = when (face) {
            CoreShellFace.POSITIVE_X -> Vec3(-0.5, 0.0, 0.0)
            CoreShellFace.NEGATIVE_X -> Vec3(0.5, 0.0, 0.0)
            CoreShellFace.POSITIVE_Y -> Vec3(0.0, -0.5, 0.0)
            CoreShellFace.NEGATIVE_Y -> Vec3(0.0, 0.5, 0.0)
            CoreShellFace.POSITIVE_Z -> Vec3(0.0, 0.0, -0.5)
            CoreShellFace.NEGATIVE_Z -> Vec3(0.0, 0.0, 0.5)
        }
        return Vec3(position.x + 0.5, position.y + 0.5, position.z + 0.5).add(inward)
    }

    private fun coreOrientation(face: CoreShellFace, quarterTurns: Int): DQuaternion {
        val faceRotation = when (face) {
            CoreShellFace.POSITIVE_Z -> axisAngle(0.0, 1.0, 0.0, 180.0)
            CoreShellFace.NEGATIVE_Z -> DQuaternion.identity
            CoreShellFace.POSITIVE_X -> axisAngle(0.0, 1.0, 0.0, -90.0)
            CoreShellFace.NEGATIVE_X -> axisAngle(0.0, 1.0, 0.0, 90.0)
            CoreShellFace.POSITIVE_Y -> axisAngle(1.0, 0.0, 0.0, 90.0)
            CoreShellFace.NEGATIVE_Y -> axisAngle(1.0, 0.0, 0.0, -90.0)
        }
        val inPlane = axisAngle(0.0, 0.0, 1.0, quarterTurns * 90.0)
        return multiply(faceRotation, inPlane)
    }
}

private fun String.apertureIdOrNull(): String? {
    val prefix = "dynamicuniverse:aperture:"
    if (!startsWith(prefix)) return null
    return removePrefix(prefix).substringBefore(':').takeIf { it.isNotBlank() }
}

private fun UniverseGeometryManifest.period(dimension: DimensionId): HorizontalPeriod? =
    layers.singleOrNull { it.dimension == dimension }?.period

private fun worldCell(period: HorizontalPeriod, anchor: HorizontalPosition, cell: ApertureCell): HorizontalPosition =
    HorizontalPosition(period.canonical(anchor.x + cell.dx), period.canonical(anchor.z + cell.dz))

private fun axisAngle(x: Double, y: Double, z: Double, degrees: Double): DQuaternion {
    val half = Math.toRadians(degrees) / 2.0
    val sine = sin(half)
    return DQuaternion(x * sine, y * sine, z * sine, cos(half))
}

private fun conjugate(q: DQuaternion): DQuaternion = DQuaternion(-q.x, -q.y, -q.z, q.w)

private fun multiply(a: DQuaternion, b: DQuaternion): DQuaternion = DQuaternion(
    a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y,
    a.w * b.y - a.x * b.z + a.y * b.w + a.z * b.x,
    a.w * b.z + a.x * b.y - a.y * b.x + a.z * b.w,
    a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z,
)

private fun MinecraftServer.level(dimension: DimensionId): ServerLevel? {
    val location = ResourceLocation.tryParse(dimension.value) ?: return null
    return getLevel(ResourceKey.create(Registries.DIMENSION, location))
}
