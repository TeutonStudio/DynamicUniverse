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

/** Source compatibility name retained while callers move to [CelestialSpatialObject]. */
typealias CosmicSpatialObject = CelestialSpatialObject

fun CelestialBody.asCosmicSpatialObject() = CelestialSpatialObject(
    objectId = id, mass = mass, radius = radius,
    kinematics = UniverseKinematicState(position, velocity), restitution = restitution,
)

fun CelestialSpatialObject.asCelestialBody() = CelestialBody(id, mass, radius, kinematics.position, kinematics.velocity, restitution)

class DynamicCosmos(private val gravityConstant: Double = 6.67430e-11) {
    private val bodies = linkedMapOf<String, CelestialSpatialObject>()

    fun register(body: CelestialBody) {
        register(body.asCosmicSpatialObject())
    }

    fun register(body: CelestialSpatialObject) {
        require(body.id !in bodies) { "Duplicate celestial body: ${body.id}" }
        bodies[body.id] = body
    }

    fun snapshot(): List<CelestialBody> = bodies.values.map(CelestialSpatialObject::asCelestialBody)
    fun spatialSnapshot(): List<CelestialSpatialObject> = bodies.values.toList()
    fun spatialObject(id: String): CelestialSpatialObject? = bodies[id]

    fun tick(seconds: Double) {
        require(seconds > 0.0 && seconds.isFinite())
        val before = spatialSnapshot()
        before.forEach { body ->
            val acceleration = before.filter { it.id != body.id }.fold(Vector3.ZERO) { total, other ->
                val offset = other.kinematics.position - body.kinematics.position
                val distanceSquared = max(offset.lengthSquared(), 1.0)
                total + offset.normalized() * (gravityConstant * other.mass / distanceSquared)
            }
            val velocity = body.kinematics.velocity + acceleration * seconds
            bodies[body.id] = body.copy(kinematics = body.kinematics.copy(position = body.kinematics.position + velocity * seconds, velocity = velocity))
        }
        resolveBilliardCollisions()
    }

    private fun resolveBilliardCollisions() {
        val candidates = spatialSnapshot()
        candidates.indices.forEach { firstIndex ->
            for (secondIndex in firstIndex + 1 until candidates.size) {
                val first = bodies.getValue(candidates[firstIndex].id)
                val second = bodies.getValue(candidates[secondIndex].id)
                val delta = second.kinematics.position - first.kinematics.position
                if (delta.lengthSquared() > (first.radius + second.radius) * (first.radius + second.radius)) continue
                val normal = if (delta.lengthSquared() > 0.0) delta.normalized() else Vector3(1.0, 0.0, 0.0)
                val normalSpeed = (second.kinematics.velocity - first.kinematics.velocity).dot(normal)
                if (normalSpeed >= 0.0) continue
                val restitution = minOf(first.restitution, second.restitution)
                val impulseSize = -(1.0 + restitution) * normalSpeed / (1.0 / first.mass + 1.0 / second.mass)
                val impulse = normal * impulseSize
                bodies[first.id] = first.copy(kinematics = first.kinematics.copy(velocity = first.kinematics.velocity - impulse * (1.0 / first.mass)))
                bodies[second.id] = second.copy(kinematics = second.kinematics.copy(velocity = second.kinematics.velocity + impulse * (1.0 / second.mass)))
            }
        }
    }
}
