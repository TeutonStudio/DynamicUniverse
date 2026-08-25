package de.TeutonStudio.DynamicUniverse.cosmos

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

/** Normalized quaternion relating an object's local coordinates to UniverseSpace. */
@ConsistentCopyVisibility
data class SpatialRotation private constructor(val x: Double, val y: Double, val z: Double, val w: Double) {
    fun rotate(vector: Vector3): Vector3 {
        val q = Vector3(x, y, z)
        val twiceCross = q.cross(vector) * 2.0
        return vector + twiceCross * w + q.cross(twiceCross)
    }
    fun inverse() = SpatialRotation(-x, -y, -z, w)

    companion object {
        val IDENTITY = SpatialRotation(0.0, 0.0, 0.0, 1.0)
        fun of(x: Double, y: Double, z: Double, w: Double): SpatialRotation {
            require(x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()) { "Rotation components must be finite." }
            val squared = x * x + y * y + z * z + w * w
            require(squared > 0.0) { "A zero quaternion cannot define a rotation." }
            val inverse = 1.0 / kotlin.math.sqrt(squared)
            return SpatialRotation(x * inverse, y * inverse, z * inverse, w * inverse)
        }
    }
}

private fun Vector3.cross(other: Vector3) = Vector3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)
