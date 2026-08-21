package de.TeutonStudio.GalactiCraft.cosmos

import kotlin.math.sqrt

data class Vector3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(factor: Double) = Vector3(x * factor, y * factor, z * factor)
    fun dot(other: Vector3): Double = x * other.x + y * other.y + z * other.z
    fun lengthSquared(): Double = dot(this)
    fun length(): Double = sqrt(lengthSquared())
    fun normalized(): Vector3 {
        val length = length()
        require(length > 0.0) { "Zero vector has no direction." }
        return this * (1.0 / length)
    }

    companion object {
        val ZERO = Vector3(0.0, 0.0, 0.0)
    }
}
