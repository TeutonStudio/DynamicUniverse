package de.TeutonStudio.DynamicUniverse.cosmos

/** Common spatial identity for bodies, ships, stations and future portal shells. */
open class UniverseSpatialObject(
    val id: String,
    open val kinematics: UniverseKinematicState,
    val collisionRadius: Double,
) {
    init {
        require(id.isNotBlank()) { "A spatial object needs an id." }
        require(collisionRadius > 0.0 && collisionRadius.isFinite()) { "Collision radius must be finite and positive." }
    }
}

/** A gravitational spatial object. Rendering and dimension-stack details stay outside this type. */
data class CelestialSpatialObject(
    val objectId: String,
    val mass: Double,
    override val kinematics: UniverseKinematicState,
    val radius: Double,
    val restitution: Double = 0.65,
) : UniverseSpatialObject(objectId, kinematics, radius) {
    init {
        require(mass > 0.0 && mass.isFinite()) { "Celestial mass must be finite and positive." }
        require(restitution in 0.0..1.0) { "Restitution must be in [0, 1]." }
    }
}
