package de.TeutonStudio.DynamicUniverse.compat.immersiveportals

import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import qouteall.imm_ptl.core.api.PortalAPI
import qouteall.imm_ptl.core.portal.Portal
import qouteall.q_misc_util.my_util.DQuaternion

/**
 * Creates a physical-scale-one portal pair for one already validated radial connection.
 * The caller owns level creation, chunk tickets, and lifecycle persistence; this class only
 * materializes the pair after both endpoints are ready.
 */
class ImmersivePortalRadialLinkMaterializer {
    fun materialize(
        connection: DimensionConnection,
        sourceLevel: ServerLevel,
        targetLevel: ServerLevel,
        sourceSurface: PortalSurface,
    ): MaterializedPortalPair {
        val targetAnchor = connection.targetPosition(sourceSurface.center)
        val forward = portal(
            sourceLevel = sourceLevel,
            targetLevel = targetLevel,
            source = sourceSurface,
            targetCenter = targetAnchor,
            linkId = connection.id,
        )
        val reverse = portal(
            sourceLevel = targetLevel,
            targetLevel = sourceLevel,
            source = PortalSurface(targetAnchor, sourceSurface.width, sourceSurface.height),
            targetCenter = sourceSurface.center,
            linkId = "${connection.id}:reverse",
        )
        return MaterializedPortalPair(forward.uuid, reverse.uuid)
    }

    private fun portal(
        sourceLevel: ServerLevel,
        targetLevel: ServerLevel,
        source: PortalSurface,
        targetCenter: SpatialPosition,
        linkId: String,
    ): Portal {
        val portal = Portal(Portal.ENTITY_TYPE, sourceLevel)
        val rotation = DQuaternion.identity
        PortalAPI.setPortalPositionOrientationAndSize(portal, source.center.toVec3(), rotation, source.width, source.height)
        // physical scale deliberately remains one; coordinate scale selected the target anchor.
        PortalAPI.setPortalTransformation(portal, targetLevel.dimension(), targetCenter.toVec3(), rotation, 1.0)
        portal.portalTag = linkId
        PortalAPI.spawnServerEntity(portal)
        return portal
    }
}

data class PortalSurface(
    val center: SpatialPosition,
    val width: Double,
    val height: Double,
) {
    init {
        require(width.isFinite() && width > 0.0)
        require(height.isFinite() && height > 0.0)
    }
}

data class MaterializedPortalPair(val forwardPortalId: java.util.UUID, val reversePortalId: java.util.UUID)

private fun SpatialPosition.toVec3(): Vec3 = Vec3(x, y, z)
