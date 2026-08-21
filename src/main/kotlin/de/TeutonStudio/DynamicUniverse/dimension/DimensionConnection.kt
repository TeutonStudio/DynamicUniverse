package de.TeutonStudio.DynamicUniverse.dimension

import java.math.BigInteger

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

/** One directed radial route. Portal and teleport adapters consume this instead of inventing links. */
data class DimensionConnection(
    val id: String,
    val source: DimensionId,
    val target: DimensionId,
    val scale: DimensionScale,
) {
    init {
        require(id.matches(Regex("[a-z0-9_./:-]+"))) { "Invalid connection id: $id" }
        require(source != target) { "A dimension connection needs two different endpoints." }
    }

    fun targetPosition(position: DimensionPosition): DimensionPosition = position.copy(
        x = scale.map(position.x),
        z = scale.map(position.z),
    )

    fun inverse(): DimensionConnection = DimensionConnection(
        id = "$id:reverse",
        source = target,
        target = source,
        scale = scale.inverse(),
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

    fun transition(source: DimensionId, connectionId: String, position: DimensionPosition): DimensionTransition? =
        routesFrom(source).firstOrNull { it.id == connectionId }?.let { route ->
            DimensionTransition(route.target, route.targetPosition(position))
        }
}
