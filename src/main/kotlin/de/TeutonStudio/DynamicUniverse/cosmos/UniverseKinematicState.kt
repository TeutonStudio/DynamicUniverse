package de.TeutonStudio.DynamicUniverse.cosmos

/**
 * The pose and linear velocity of an object in a [UniverseSpace].
 *
 * Rotation belongs to the object/local frame only; UniverseSpace itself deliberately has no
 * preferred forward, up, orbital plane, or radial direction.
 */
data class UniverseKinematicState(
    val position: Vector3,
    val velocity: Vector3 = Vector3.ZERO,
    val orientation: SpatialRotation = SpatialRotation.IDENTITY,
) {
    fun advanced(seconds: Double): UniverseKinematicState {
        require(seconds >= 0.0 && seconds.isFinite()) { "Elapsed simulation time must be finite and non-negative." }
        return copy(position = position + velocity * seconds)
    }
}

/** Unit quaternion used only to relate a local object's coordinates to UniverseSpace. */
@ConsistentCopyVisibility
data class SpatialRotation private constructor(
    val x: Double,
    val y: Double,
    val z: Double,
    val w: Double,
) {
    fun rotate(vector: Vector3): Vector3 {
        val qVector = Vector3(x, y, z)
        val twiceCross = qVector.cross(vector) * 2.0
        return vector + twiceCross * w + qVector.cross(twiceCross)
    }

    fun inverse(): SpatialRotation = SpatialRotation(-x, -y, -z, w)

    companion object {
        val IDENTITY = SpatialRotation(0.0, 0.0, 0.0, 1.0)

        fun of(x: Double, y: Double, z: Double, w: Double): SpatialRotation {
            require(x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()) { "Rotation components must be finite." }
            val magnitudeSquared = x * x + y * y + z * z + w * w
            require(magnitudeSquared > 0.0) { "A zero quaternion cannot define a rotation." }
            val inverseMagnitude = 1.0 / kotlin.math.sqrt(magnitudeSquared)
            return SpatialRotation(x * inverseMagnitude, y * inverseMagnitude, z * inverseMagnitude, w * inverseMagnitude)
        }
    }
}

private fun Vector3.cross(other: Vector3) = Vector3(
    y * other.z - z * other.y,
    z * other.x - x * other.z,
    x * other.y - y * other.x,
)
