package de.TeutonStudio.DynamicUniverse.cosmos

/**
 * An unbounded, continuous, directionless three-dimensional coordinate space.
 *
 * It is not a Minecraft dimension, a planet surface, or an orbit plane. Dimensions and moving
 * local spaces bind into it explicitly through [PlanetSpaceBinding] or compatibility adapters.
 */
data class UniverseSpace(val id: String) {
    init {
        require(id.matches(Regex("[a-z0-9_.-]+:[a-z0-9_./-]+"))) { "Universe-space id must be namespaced." }
    }

    fun point(x: Double, y: Double, z: Double): Vector3 = Vector3(x, y, z)
}

/** Logical transform between a planet-local continuous space and its UniverseSpace anchor. */
data class PlanetSpaceBinding(
    val planetId: String,
    val localSpaceId: String,
    val universeSpace: UniverseSpace,
    val kinematics: UniverseKinematicState,
    val localUnitsPerUniverseUnit: Double = 1.0,
) {
    init {
        require(planetId.isNotBlank()) { "A planet binding needs a planet id." }
        require(localSpaceId.isNotBlank()) { "A planet binding needs a local-space id." }
        require(localUnitsPerUniverseUnit > 0.0 && localUnitsPerUniverseUnit.isFinite()) {
            "The planet-local scale must be finite and positive."
        }
    }

    fun universePosition(localPosition: Vector3): Vector3 =
        kinematics.position + kinematics.orientation.rotate(localPosition * (1.0 / localUnitsPerUniverseUnit))

    fun localPosition(universePosition: Vector3): Vector3 =
        kinematics.orientation.inverse().rotate(universePosition - kinematics.position) * localUnitsPerUniverseUnit

    fun universeVelocity(localVelocity: Vector3): Vector3 =
        kinematics.velocity + kinematics.orientation.rotate(localVelocity * (1.0 / localUnitsPerUniverseUnit))

    fun localVelocity(universeVelocity: Vector3): Vector3 =
        kinematics.orientation.inverse().rotate(universeVelocity - kinematics.velocity) * localUnitsPerUniverseUnit
}
