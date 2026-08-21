package de.TeutonStudio.DynamicUniverse.dimension

import java.math.BigInteger
import kotlin.math.sqrt

data class DimensionScale(
    val numerator: Long,
    val denominator: Long = 1,
) {
    init {
        require(numerator > 0) { "Dimension-scale numerator must be positive." }
        require(denominator > 0) { "Dimension-scale denominator must be positive." }
    }

    fun map(coordinate: Long): Long {
        val scaled = BigInteger.valueOf(coordinate).multiply(BigInteger.valueOf(numerator))
        val divisor = BigInteger.valueOf(denominator)
        val quotient = scaled.divide(divisor)
        val remainder = scaled.remainder(divisor)
        return if (scaled.signum() < 0 && remainder.signum() != 0) quotient.subtract(BigInteger.ONE).longValueExact()
        else quotient.longValueExact()
    }

    fun inverse(): DimensionScale = DimensionScale(denominator, numerator)

    fun asDouble(): Double = numerator.toDouble() / denominator.toDouble()

    companion object {
        val ONE = DimensionScale(1)
    }
}

data class DimensionId(val value: String) {
    init {
        require(value.matches(Regex("[a-z0-9_.-]+:[a-z0-9_./-]+"))) { "Dimension id must be namespaced: $value" }
    }
}

data class DimensionPosition(val x: Long, val y: Long, val z: Long)

/** The material exposed on one side of a vertical boundary. */
enum class BoundarySurface { BEDROCK, AIR }

/**
 * A coordinate change is deliberately separate from physical scale. A factor of eight
 * makes travel cover eight times as much of the outer layer, but does not shrink players,
 * blocks, or Sable sublevels.
 */
data class DimensionTransform(
    val coordinateScale: DimensionScale,
    val physicalScale: Double = 1.0,
    val sourceAnchor: SpatialPosition = SpatialPosition.ZERO,
    val targetAnchor: SpatialPosition = SpatialPosition.ZERO,
    val rotation: SpatialQuaternion = SpatialQuaternion.IDENTITY,
) {
    init {
        require(physicalScale.isFinite() && physicalScale > 0.0) { "Physical scale must be finite and positive." }
    }

    fun map(position: SpatialPosition): SpatialPosition {
        val relative = position - sourceAnchor
        return targetAnchor + rotation.rotate(relative * coordinateScale.asDouble())
    }

    fun mapVelocity(velocity: SpatialVelocity): SpatialVelocity =
        rotation.rotate(velocity * coordinateScale.asDouble())

    fun inverse(): DimensionTransform = DimensionTransform(
        coordinateScale = coordinateScale.inverse(),
        physicalScale = 1.0 / physicalScale,
        sourceAnchor = targetAnchor,
        targetAnchor = sourceAnchor,
        rotation = rotation.inverse(),
    )
}

data class SpatialPosition(val x: Double, val y: Double, val z: Double) {
    operator fun minus(other: SpatialPosition) = SpatialVector(x - other.x, y - other.y, z - other.z)
    operator fun plus(other: SpatialVector) = SpatialPosition(x + other.x, y + other.y, z + other.z)

    companion object { val ZERO = SpatialPosition(0.0, 0.0, 0.0) }
}

data class SpatialVelocity(val x: Double, val y: Double, val z: Double) {
    operator fun times(scale: Double) = SpatialVelocity(x * scale, y * scale, z * scale)
    operator fun plus(other: SpatialVelocity) = SpatialVelocity(x + other.x, y + other.y, z + other.z)

    companion object { val ZERO = SpatialVelocity(0.0, 0.0, 0.0) }
}

data class SpatialVector(val x: Double, val y: Double, val z: Double) {
    operator fun times(scale: Double) = SpatialVector(x * scale, y * scale, z * scale)
}

/** Normalized quaternion, stored instead of lossy yaw/pitch pairs. */
data class SpatialQuaternion(val x: Double, val y: Double, val z: Double, val w: Double) {
    init {
        val length = sqrt(x * x + y * y + z * z + w * w)
        require(length.isFinite() && length > 0.0) { "Rotation must be finite and non-zero." }
    }

    private val normalized: SpatialQuaternion by lazy {
        val length = sqrt(x * x + y * y + z * z + w * w)
        if (length == 1.0) this else SpatialQuaternion(x / length, y / length, z / length, w / length)
    }

    fun inverse(): SpatialQuaternion {
        val q = normalized
        return SpatialQuaternion(-q.x, -q.y, -q.z, q.w)
    }

    fun rotate(vector: SpatialVector): SpatialVector {
        val q = normalized
        val tx = 2.0 * (q.y * vector.z - q.z * vector.y)
        val ty = 2.0 * (q.z * vector.x - q.x * vector.z)
        val tz = 2.0 * (q.x * vector.y - q.y * vector.x)
        return SpatialVector(
            vector.x + q.w * tx + (q.y * tz - q.z * ty),
            vector.y + q.w * ty + (q.z * tx - q.x * tz),
            vector.z + q.w * tz + (q.x * ty - q.y * tx),
        )
    }

    fun rotate(velocity: SpatialVelocity): SpatialVelocity {
        val vector = rotate(SpatialVector(velocity.x, velocity.y, velocity.z))
        return SpatialVelocity(vector.x, vector.y, vector.z)
    }

    companion object { val IDENTITY = SpatialQuaternion(0.0, 0.0, 0.0, 1.0) }
}

/** One directed radial route. Portal and teleport adapters consume this instead of inventing links. */
data class DimensionConnection(
    val id: String,
    val source: DimensionId,
    val target: DimensionId,
    val scale: DimensionScale,
    val physicalScale: Double = 1.0,
    val boundarySurface: BoundarySurface = BoundarySurface.AIR,
    val transform: DimensionTransform = DimensionTransform(scale, physicalScale),
) {
    init {
        require(id.matches(Regex("[a-z0-9_./:-]+"))) { "Invalid connection id: $id" }
        require(source != target) { "A dimension connection needs two different endpoints." }
        require(physicalScale.isFinite() && physicalScale > 0.0) { "Physical scale must be finite and positive." }
        require(transform.coordinateScale == scale) { "Connection scale and transform scale must agree." }
    }

    fun targetPosition(position: DimensionPosition): DimensionPosition = position.copy(
        x = scale.map(position.x),
        z = scale.map(position.z),
    )

    fun targetPosition(position: SpatialPosition): SpatialPosition = transform.map(position)

    fun targetVelocity(velocity: SpatialVelocity): SpatialVelocity = transform.mapVelocity(velocity)

    fun inverse(): DimensionConnection = DimensionConnection(
        id = "$id:reverse",
        source = target,
        target = source,
        scale = scale.inverse(),
        physicalScale = 1.0 / physicalScale,
        boundarySurface = boundarySurface,
        transform = transform.inverse(),
    )
}

data class DimensionTransition(
    val target: DimensionId,
    val position: DimensionPosition,
)

/** Immutable, server-authoritative connection graph with an explicit inverse for each route. */
class DimensionConnectionGraph(connections: Collection<DimensionConnection>) {
    private val outgoing = connections
        .flatMap { listOf(it, it.inverse()) }
        .groupBy(DimensionConnection::source)
        .mapValues { (_, routes) ->
            require(routes.map(DimensionConnection::id).distinct().size == routes.size) {
                "Connection ids must be unique per source dimension."
            }
            routes
        }

    fun routesFrom(source: DimensionId): List<DimensionConnection> = outgoing[source].orEmpty()

    fun allRoutes(): List<DimensionConnection> = outgoing.values.flatten()

    fun transition(source: DimensionId, connectionId: String, position: DimensionPosition): DimensionTransition? =
        routesFrom(source).firstOrNull { it.id == connectionId }?.let { route ->
            DimensionTransition(route.target, route.targetPosition(position))
        }
}
