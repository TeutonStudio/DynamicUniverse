package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.UniverseFrame
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import de.TeutonStudio.DynamicUniverse.topology.ToroidalSeamSpec
import de.TeutonStudio.DynamicUniverse.worldtype.IsolatedUniverseDefinition
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionRole
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionStack
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldType
import de.TeutonStudio.DynamicUniverse.worldtype.VerticalDimensionSeam
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI

/** Persist this logical graph, not incidental Immersive Portals entity UUIDs. */
data class UniverseGeometryManifest(
    val version: Int = CURRENT_VERSION,
    val universe: UniverseFrame,
    val layers: List<LayerGeometry>,
    val airBuffers: List<AirBoundaryBuffer>,
    val links: List<DimensionConnection>,
    /** Immutable local-space identities. Their anchors/velocities belong to UniverseRuntimeState. */
    val planetSpaces: List<PlanetSpaceGeometry>,
    val isolatedUniverses: List<IsolatedUniverseDefinition>,
    val verticalSeams: List<VerticalDimensionSeam> = emptyList(),
    val planetCores: List<PlanetCoreGeometry> = emptyList(),
) {
    init {
        require(layers.map(LayerGeometry::dimension).distinct().size == layers.size) { "A level may only bind one layer." }
        require(links.map(DimensionConnection::id).distinct().size == links.size) { "Logical link ids must be unique." }
        require(planetSpaces.map(PlanetSpaceGeometry::planetId).distinct().size == planetSpaces.size) { "A planet may only bind one local space." }
        require(verticalSeams.map(VerticalDimensionSeam::id).distinct().size == verticalSeams.size) { "Vertical seam ids must be unique." }
        require(planetCores.map(PlanetCoreGeometry::coreDimension).distinct().size == planetCores.size) {
            "A planet-core dimension may only belong to one core geometry."
        }
    }

    companion object {
        const val CURRENT_VERSION = 4
        /** Keeps rendered stack layers well outside the tallest generated dimension. */
        const val RENDER_LAYER_SPACING_BLOCKS = 4096
    }
}

data class PlanetSpaceGeometry(val planetId: String, val localSpaceId: String) {
    init { require(planetId.isNotBlank() && localSpaceId.isNotBlank()) }
}

data class LayerGeometry(
    val dimension: DimensionId,
    val period: HorizontalPeriod,
    val seam: ToroidalSeamSpec = ToroidalSeamSpec.centered(period),
    val ownerId: String? = null,
    val role: PlanetDimensionRole = PlanetDimensionRole.CUSTOM,
    /** The stack and position are render metadata; they never move local chunks. */
    val renderStackId: String? = null,
    val renderStackIndex: Int = 0,
) {
    /** Render-only pseudo radius; cosmic collision radius remains a separate body property. */
    val projectionRadiusBlocks: Double get() = period.blocks.toDouble() / (2.0 * PI)
}

data class PlanetCoreGeometry(
    val planetId: String,
    val connectionId: String,
    val coreDimension: DimensionId,
    val deepDimension: DimensionId,
    val edgeBlocks: Long,
    val edgeMarginBlocks: Int = 2,
) {
    init {
        require(edgeBlocks >= 16L) { "Planet-core edge must be at least one chunk." }
        require(edgeMarginBlocks >= 1) { "Planet-core apertures need a positive edge margin." }
        require(edgeBlocks > edgeMarginBlocks * 2L) { "Planet-core edge margin consumes the whole face." }
    }
}

/** Every AIR-to-AIR stack boundary gets a symmetric ten-block render/transfer buffer. */
data class AirBoundaryBuffer(
    val lowerDimension: DimensionId,
    val upperDimension: DimensionId,
    val lowerBlocks: Int = 5,
    val upperBlocks: Int = 5,
) {
    init { require(lowerBlocks + upperBlocks == 10) { "An alpha0 air buffer is exactly ten blocks." } }
}

/** Turns the validated world type into the immutable runtime geometry contract. */
object UniverseGeometryCompiler {
    fun compile(worldType: UniverseWorldType): UniverseGeometryManifest {
        val layers = mutableListOf<LayerGeometry>()
        val airBuffers = mutableListOf<AirBoundaryBuffer>()
        val spaces = mutableListOf<PlanetSpaceGeometry>()
        val cores = mutableListOf<PlanetCoreGeometry>()
        worldType.galaxies.forEach { galaxy ->
            galaxy.groups.forEach { group ->
                group.star?.stacks?.forEach { stack ->
                    layers += stack.layers(defaultCorePeriod = 16L, ownerId = group.star.id)
                    airBuffers += stack.airBuffers()
                }
                group.allPlanets().forEach { planet ->
                    val basePeriod = corePeriod(planet.planetCoreSize)
                    planet.stacks.forEach { stack ->
                        layers += stack.layers(basePeriod, planet.id)
                        airBuffers += stack.airBuffers()
                        val core = stack.layersInnerToOuter.first()
                        val deep = stack.layersInnerToOuter.getOrNull(1)
                        if (deep != null) {
                            cores += PlanetCoreGeometry(
                                planetId = planet.id,
                                connectionId = "${planet.id}/${stack.id}/${core.id}-to-${deep.id}",
                                coreDimension = core.dimension,
                                deepDimension = deep.dimension,
                                edgeBlocks = basePeriod,
                            )
                        }
                    }
                    spaces += PlanetSpaceGeometry(planet.id, "${planet.id}:local")
                }
            }
        }
        return UniverseGeometryManifest(
            universe = UniverseFrame(worldType.universeDimension.value),
            layers = layers,
            airBuffers = airBuffers,
            links = worldType.connectionGraph().allRoutes().filterNot { it.id.endsWith(":reverse") },
            planetSpaces = spaces,
            isolatedUniverses = worldType.isolatedUniverses,
            verticalSeams = worldType.verticalSeams,
            planetCores = cores,
        )
    }

    private fun PlanetDimensionStack.layers(defaultCorePeriod: Long, ownerId: String): List<LayerGeometry> {
        var period = HorizontalPeriod(defaultCorePeriod)
        return layersInnerToOuter.mapIndexed { index, layer ->
            if (index > 0) period = scalePeriod(period, requireNotNull(layersInnerToOuter[index - 1].toOuterScale))
            LayerGeometry(
                dimension = layer.dimension,
                period = period,
                ownerId = ownerId,
                role = layer.role,
                renderStackId = "$ownerId/$id",
                renderStackIndex = index,
            )
        }
    }

    private fun PlanetDimensionStack.airBuffers(): List<AirBoundaryBuffer> = layersInnerToOuter.windowed(2)
        .filter { (lower, upper) ->
            lower.outerBoundarySurface == de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface.AIR &&
                upper.innerBoundarySurface == de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface.AIR
        }
        .map { (lower, upper) -> AirBoundaryBuffer(lower.dimension, upper.dimension) }

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
