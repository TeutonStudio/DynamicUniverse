package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.PlanetFrame
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseFrame
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import de.TeutonStudio.DynamicUniverse.topology.ToroidalSeamSpec
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionStack
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldType
import java.math.BigDecimal
import java.math.RoundingMode

/** Persist this logical graph, not incidental Immersive Portals entity UUIDs. */
data class UniverseGeometryManifest(
    val version: Int = CURRENT_VERSION,
    val universe: UniverseFrame,
    val layers: List<LayerGeometry>,
    val links: List<DimensionConnection>,
    val planetFrames: List<PlanetFrame>,
) {
    init {
        require(layers.map(LayerGeometry::dimension).distinct().size == layers.size) { "A level may only bind one layer." }
        require(links.map(DimensionConnection::id).distinct().size == links.size) { "Logical link ids must be unique." }
    }

    companion object { const val CURRENT_VERSION = 1 }
}

data class LayerGeometry(
    val dimension: DimensionId,
    val period: HorizontalPeriod,
    val seam: ToroidalSeamSpec = ToroidalSeamSpec.centered(period),
)

/** Turns the validated world type into the immutable runtime geometry contract. */
object UniverseGeometryCompiler {
    fun compile(worldType: UniverseWorldType): UniverseGeometryManifest {
        val layers = mutableListOf<LayerGeometry>()
        val frames = mutableListOf<PlanetFrame>()
        worldType.galaxies.forEach { galaxy ->
            galaxy.groups.forEach { group ->
                group.star?.stacks?.forEach { stack -> layers += stack.layers(defaultCorePeriod = 16L) }
                group.planets.forEach { planet ->
                    val basePeriod = corePeriod(planet.planetCoreSize)
                    planet.stacks.forEach { stack -> layers += stack.layers(basePeriod) }
                    frames += PlanetFrame(
                        id = "${planet.id}:frame",
                        universeFrame = UniverseFrame(worldType.universeDimension.value),
                        anchor = SpatialPosition.ZERO,
                        velocity = SpatialVelocity(0.0, 0.0, 0.0),
                    )
                }
            }
        }
        return UniverseGeometryManifest(
            universe = UniverseFrame(worldType.universeDimension.value),
            layers = layers,
            links = worldType.connectionGraph().allRoutes().filterNot { it.id.endsWith(":reverse") },
            planetFrames = frames,
        )
    }

    private fun PlanetDimensionStack.layers(defaultCorePeriod: Long): List<LayerGeometry> {
        var period = HorizontalPeriod(defaultCorePeriod)
        return layersInnerToOuter.mapIndexed { index, layer ->
            if (index > 0) period = scalePeriod(period, requireNotNull(layersInnerToOuter[index - 1].toOuterScale))
            LayerGeometry(layer.dimension, period)
        }
    }

    private fun corePeriod(coreSize: Double): Long {
        val rounded = BigDecimal.valueOf(coreSize).setScale(0, RoundingMode.CEILING).longValueExact()
        return Math.multiplyExact(rounded, 16L)
    }

    private fun scalePeriod(period: HorizontalPeriod, scale: DimensionScale): HorizontalPeriod {
        val scaled = java.math.BigInteger.valueOf(period.blocks).multiply(java.math.BigInteger.valueOf(scale.numerator))
        val divisor = java.math.BigInteger.valueOf(scale.denominator)
        val division = scaled.divideAndRemainder(divisor)
        require(division[1] == java.math.BigInteger.ZERO) { "Layer scale must preserve chunk-aligned horizontal periods." }
        return HorizontalPeriod(division[0].longValueExact())
    }
}
