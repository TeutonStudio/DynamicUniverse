package de.TeutonStudio.GalactiCraft.cosmos

import kotlin.math.max

data class CollisionMaterial(
    val id: String,
    val restitution: Double = 0.65,
) {
    init {
        require(restitution in 0.0..1.0) { "Restitution must be in [0, 1]." }
    }
}

data class CelestialBody(
    val id: String,
    val mass: Double,
    val radius: Double,
    val material: CollisionMaterial = CollisionMaterial("rock"),
    val position: Vector3 = Vector3.ZERO,
    val velocity: Vector3 = Vector3.ZERO,
) {
    init {
        require(mass > 0.0 && mass.isFinite()) { "Mass must be finite and positive." }
        require(radius > 0.0 && radius.isFinite()) { "Radius must be finite and positive." }
    }

    companion object {
        fun earth(id: String) = CelestialBody(id, 5.972e24, 6_371_000.0)
    }
}

/** Server-authoritative, pairwise N-body state. Units are SI-like but intentionally configurable. */
class DynamicCosmos(private val gravitationalConstant: Double = 6.67430e-11) {
    private val bodies = linkedMapOf<String, CelestialBody>()

    fun register(body: CelestialBody) {
        require(body.id !in bodies) { "Celestial body already exists: ${body.id}" }
        bodies.values.forEach { existing ->
            require((body.position - existing.position).lengthSquared() > (body.radius + existing.radius) * (body.radius + existing.radius)) {
                "New body ${body.id} overlaps ${existing.id}; choose a non-colliding administrative spawn position."
            }
        }
        bodies[body.id] = body
    }

    fun bodies(): Collection<CelestialBody> = bodies.values.toList()

    fun tick(seconds: Double) {
        require(seconds > 0.0 && seconds.isFinite()) { "Simulation delta must be positive." }
        val snapshot = bodies.values.toList()
        val accelerations = snapshot.associate { body -> body.id to accelerationOf(body, snapshot) }
        snapshot.forEach { body ->
            val velocity = body.velocity + accelerations.getValue(body.id) * seconds
            bodies[body.id] = body.copy(position = body.position + velocity * seconds, velocity = velocity)
        }
        resolveCollisions()
    }

    private fun accelerationOf(subject: CelestialBody, allBodies: List<CelestialBody>): Vector3 =
        allBodies.filter { it.id != subject.id }.fold(Vector3.ZERO) { total, other ->
            val displacement = other.position - subject.position
            val distanceSquared = max(displacement.lengthSquared(), 1.0)
            total + displacement.normalized() * (gravitationalConstant * other.mass / distanceSquared)
        }

    private fun resolveCollisions() {
        val snapshot = bodies.values.toList()
        snapshot.indices.forEach { i ->
            for (j in i + 1 until snapshot.size) {
                val resolved = ElasticCollisionResolver.resolve(snapshot[i], snapshot[j]) ?: continue
                bodies[resolved.first.id] = resolved.first
                bodies[resolved.second.id] = resolved.second
            }
        }
    }
}

/** Sphere collision response: the same normal impulse model as billiard balls. */
object ElasticCollisionResolver {
    fun resolve(first: CelestialBody, second: CelestialBody): Pair<CelestialBody, CelestialBody>? {
        val separation = second.position - first.position
        if (separation.lengthSquared() > (first.radius + second.radius) * (first.radius + second.radius)) return null
        val relativeVelocity = second.velocity - first.velocity
        val normal = when {
            separation.lengthSquared() > 0.0 -> separation.normalized()
            relativeVelocity.lengthSquared() > 0.0 -> relativeVelocity.normalized()
            else -> Vector3(1.0, 0.0, 0.0)
        }
        val closingSpeed = relativeVelocity.dot(normal)
        if (closingSpeed >= 0.0) return null

        val restitution = minOf(first.material.restitution, second.material.restitution)
        val impulseMagnitude = -(1.0 + restitution) * closingSpeed / (1.0 / first.mass + 1.0 / second.mass)
        val impulse = normal * impulseMagnitude
        return first.copy(velocity = first.velocity - impulse * (1.0 / first.mass)) to
            second.copy(velocity = second.velocity + impulse * (1.0 / second.mass))
    }
}
