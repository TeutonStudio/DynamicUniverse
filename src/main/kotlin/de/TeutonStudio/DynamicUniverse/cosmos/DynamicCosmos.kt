package de.TeutonStudio.DynamicUniverse.cosmos

import kotlin.math.max

data class CelestialBody(
    val id: String,
    val mass: Double,
    val radius: Double,
    val position: Vector3,
    val velocity: Vector3,
    val restitution: Double = 0.65,
) {
    init {
        require(mass > 0.0 && mass.isFinite())
        require(radius > 0.0 && radius.isFinite())
        require(restitution in 0.0..1.0)
    }
}

class DynamicCosmos(private val gravityConstant: Double = 6.67430e-11) {
    private val bodies = linkedMapOf<String, CelestialBody>()

    fun register(body: CelestialBody) {
        require(body.id !in bodies) { "Duplicate celestial body: ${body.id}" }
        bodies[body.id] = body
    }

    fun snapshot(): List<CelestialBody> = bodies.values.toList()

    fun tick(seconds: Double) {
        require(seconds > 0.0 && seconds.isFinite())
        val before = snapshot()
        before.forEach { body ->
            val acceleration = before.filter { it.id != body.id }.fold(Vector3.ZERO) { total, other ->
                val offset = other.position - body.position
                val distanceSquared = max(offset.lengthSquared(), 1.0)
                total + offset.normalized() * (gravityConstant * other.mass / distanceSquared)
            }
            val velocity = body.velocity + acceleration * seconds
            bodies[body.id] = body.copy(position = body.position + velocity * seconds, velocity = velocity)
        }
        resolveBilliardCollisions()
    }

    private fun resolveBilliardCollisions() {
        val candidates = snapshot()
        candidates.indices.forEach { firstIndex ->
            for (secondIndex in firstIndex + 1 until candidates.size) {
                val first = bodies.getValue(candidates[firstIndex].id)
                val second = bodies.getValue(candidates[secondIndex].id)
                val delta = second.position - first.position
                if (delta.lengthSquared() > (first.radius + second.radius) * (first.radius + second.radius)) continue
                val normal = if (delta.lengthSquared() > 0.0) delta.normalized() else Vector3(1.0, 0.0, 0.0)
                val normalSpeed = (second.velocity - first.velocity).dot(normal)
                if (normalSpeed >= 0.0) continue
                val restitution = minOf(first.restitution, second.restitution)
                val impulseSize = -(1.0 + restitution) * normalSpeed / (1.0 / first.mass + 1.0 / second.mass)
                val impulse = normal * impulseSize
                bodies[first.id] = first.copy(velocity = first.velocity - impulse * (1.0 / first.mass))
                bodies[second.id] = second.copy(velocity = second.velocity + impulse * (1.0 / second.mass))
            }
        }
    }
}
