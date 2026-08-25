package de.TeutonStudio.DynamicUniverse.cosmos

import kotlin.math.sqrt

data class Vector3(val x: Double, val y: Double, val z: Double) {
    init { require(x.isFinite() && y.isFinite() && z.isFinite()) { "Vector coordinates must be finite." } }

    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double) = Vector3(x * scalar, y * scalar, z * scalar)
    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z
    fun lengthSquared() = dot(this)
    fun distanceSquaredTo(other: Vector3) = (other - this).lengthSquared()
    fun normalized(): Vector3 {
        val length = sqrt(lengthSquared())
        require(length > 0.0) { "A zero vector cannot define a collision normal." }
        return this * (1.0 / length)
    }

    companion object { val ZERO = Vector3(0.0, 0.0, 0.0) }
}
