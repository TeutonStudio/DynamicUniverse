package de.TeutonStudio.DynamicUniverse.client.worldcreation

import java.math.BigInteger
import kotlin.math.sqrt

/** Exact number of non-repeating horizontal block positions in one Y layer. */
object DimensionStackMetrics {
    fun periodAt(planet: EditablePlanet, layerIndex: Int): BigInteger {
        require(layerIndex in planet.dimensions.indices)
        return BigInteger.valueOf(chunkAlignedPeriod(planet.coreSize).toLong())
            .multiply(BigInteger.valueOf(planet.transitionFactor.toLong()).pow(layerIndex))
    }

    fun uniqueAreaAt(planet: EditablePlanet, layerIndex: Int): BigInteger = periodAt(planet, layerIndex).pow(2)

    fun equivalentSurfaceRadiusBlocks(planet: EditablePlanet, layerIndex: Int): Double? {
        if (planet.dimensions[layerIndex].role != EditableDimensionRole.SURFACE) return null
        return sqrt(uniqueAreaAt(planet, layerIndex).toDouble() / (4.0 * Math.PI))
    }

    private fun chunkAlignedPeriod(edgeBlocks: Int): Int = ((edgeBlocks + 15) / 16) * 16
}
