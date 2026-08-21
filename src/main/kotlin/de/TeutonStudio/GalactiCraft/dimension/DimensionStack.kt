package de.TeutonStudio.GalactiCraft.dimension

import java.math.BigInteger

data class ScaleRatio(
    val numerator: Long,
    val denominator: Long = 1,
) {
    init {
        require(numerator > 0) { "Scale numerator must be positive." }
        require(denominator > 0) { "Scale denominator must be positive." }
    }

    fun mapOuter(coordinate: Long): Long {
        val scaled = BigInteger.valueOf(coordinate).multiply(BigInteger.valueOf(numerator))
        val divisor = BigInteger.valueOf(denominator)
        val quotient = scaled.divide(divisor)
        val remainder = scaled.remainder(divisor)
        // Coordinate descent must use floor division, also west/south of origin.
        return if (scaled.signum() < 0 && remainder.signum() != 0) quotient.subtract(BigInteger.ONE).longValueExact()
        else quotient.longValueExact()
    }
}

enum class LayerRole {
    PLANET_CORE,
    DEEP_NETHER,
    NETHER,
    SURFACE,
    SKY,
    CUSTOM,
}

data class DimensionLayer(
    val id: String,
    val role: LayerRole,
    val generationTemplate: String,
    /** Ratio from this layer to the next outer layer. The outermost layer has no value. */
    val toOuterScale: ScaleRatio? = null,
) {
    init {
        require(id.matches(Regex("[a-z0-9_./-]+"))) { "Invalid layer id: $id" }
        require(':' in generationTemplate) { "Generation template must be namespaced." }
    }
}

sealed interface StackEndpoint {
    data object PlanetCore : StackEndpoint
    data object Cosmos : StackEndpoint
}

data class DimensionStack(
    val id: String,
    val layersInnerToOuter: List<DimensionLayer>,
) {
    init {
        require(id.matches(Regex("[a-z0-9_./-]+"))) { "Invalid stack id: $id" }
        require(layersInnerToOuter.size >= 2) { "A stack needs at least a core and an outer layer." }
        require(layersInnerToOuter.first().role == LayerRole.PLANET_CORE) {
            "The innermost layer must be the planet core."
        }
        require(layersInnerToOuter.last().role == LayerRole.SKY) {
            "The outermost layer must be a sky layer connected to the cosmos."
        }
        layersInnerToOuter.dropLast(1).forEach {
            requireNotNull(it.toOuterScale) { "Every inner boundary needs an explicit scale ratio." }
        }
        require(layersInnerToOuter.last().toOuterScale == null) {
            "The outermost layer has no further dimension scale."
        }
        require(layersInnerToOuter.map { it.id }.distinct().size == layersInnerToOuter.size) {
            "Layer ids must be unique per stack."
        }
    }

    val innerEndpoint: StackEndpoint = StackEndpoint.PlanetCore
    val outerEndpoint: StackEndpoint = StackEndpoint.Cosmos

    fun runtimeDimensionPath(planetId: String, layer: DimensionLayer): String =
        "planets/${planetId.substringAfter(':')}/$id/${layer.id}"
}
