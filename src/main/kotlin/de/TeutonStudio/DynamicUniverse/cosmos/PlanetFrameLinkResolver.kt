package de.TeutonStudio.DynamicUniverse.cosmos

import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionTransform
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity

/**
 * Resolves the moving outer link without ever moving a planet's local chunks. Only the
 * Universe-side anchor and inertial velocity are updated as its celestial body moves.
 */
object PlanetFrameLinkResolver {
    fun bindOuterLink(connection: DimensionConnection, frame: PlanetFrame): DimensionConnection = connection.copy(
        transform = DimensionTransform(
            coordinateScale = connection.scale,
            physicalScale = connection.physicalScale,
            sourceAnchor = connection.transform.sourceAnchor,
            targetAnchor = frame.anchor,
            rotation = frame.rotation,
        ),
    )

    fun toUniverse(connection: DimensionConnection, frame: PlanetFrame, position: SpatialPosition, velocity: SpatialVelocity): PlanetFrameTraversal =
        PlanetFrameTraversal(
            position = bindOuterLink(connection, frame).targetPosition(position),
            velocity = bindOuterLink(connection, frame).targetVelocity(velocity) + frame.velocity,
        )
}

data class PlanetFrameTraversal(val position: SpatialPosition, val velocity: SpatialVelocity)
