package de.TeutonStudio.DynamicUniverse.cosmos

import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialQuaternion
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity

/** The inertial reference frame shared by all Universe-host connections. */
data class UniverseFrame(val id: String = "dynamicuniverse:universe")

/**
 * A planet keeps local chunks stable while this binding moves its outer portal anchor through
 * Universe space. The server updates anchor and velocity from DynamicCosmos each tick.
 */
data class PlanetFrame(
    val id: String,
    val universeFrame: UniverseFrame,
    val anchor: SpatialPosition,
    val velocity: SpatialVelocity,
    val rotation: SpatialQuaternion = SpatialQuaternion.IDENTITY,
)
