package de.TeutonStudio.DynamicUniverse.cosmos

import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPosition
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.max

/**
 * Client-visible representation of a local, toroidal planet layer.
 *
 * The globe is deliberately presentation only: terrain, entities, saving and traversal keep
 * using [HorizontalPosition] in the source T². A sphere cannot be a globally faithful copy of a
 * torus, so every projection reports its seam state instead of pretending that a unique smooth
 * inverse exists everywhere.
 */
data class GlobeVisualConfiguration(
    val visualRadius: Double,
    val atlas: GlobeAtlas = GlobeAtlas.SIX_CHART,
    val seamBlendFraction: Double = 0.035,
    val curvatureStartHeight: Double = 192.0,
) {
    init {
        require(visualRadius.isFinite() && visualRadius > 0.0) { "Globe radius must be finite and positive." }
        require(seamBlendFraction.isFinite() && seamBlendFraction in 0.0..0.25) { "Invalid globe seam blend." }
        require(curvatureStartHeight.isFinite() && curvatureStartHeight >= 0.0) { "Invalid curvature start height." }
    }
}

/** The six-chart atlas avoids a single pole vertex; atlas edges remain declared visual seams. */
enum class GlobeAtlas { SIX_CHART, EQUIRECTANGULAR_DEBUG }

enum class GlobeChart { POSITIVE_X, NEGATIVE_X, POSITIVE_Y, NEGATIVE_Y, POSITIVE_Z, NEGATIVE_Z }

data class GlobeSample(
    val chart: GlobeChart,
    val position: HorizontalPosition,
    /** 0 at an atlas edge and 1 outside its blend region. */
    val seamWeight: Double,
    /** Equirectangular debugging exposes its unavoidable polar singularity explicitly. */
    val polarSingularity: Boolean = false,
)

/** Pure, deterministic S² → T² sampler shared by globe rendering, ray-picking and tests. */
object GlobeProjection {
    fun sample(direction: Vector3, period: HorizontalPeriod, configuration: GlobeVisualConfiguration): GlobeSample =
        when (configuration.atlas) {
            GlobeAtlas.SIX_CHART -> sixChart(direction, period, configuration.seamBlendFraction)
            GlobeAtlas.EQUIRECTANGULAR_DEBUG -> equirectangular(direction, period, configuration.seamBlendFraction)
        }

    private fun sixChart(direction: Vector3, period: HorizontalPeriod, blend: Double): GlobeSample {
        val unit = direction.normalized()
        val ax = abs(unit.x)
        val ay = abs(unit.y)
        val az = abs(unit.z)
        val chart: GlobeChart
        val u: Double
        val v: Double
        when {
            ax >= ay && ax >= az && unit.x >= 0.0 -> {
                chart = GlobeChart.POSITIVE_X; u = -unit.z / ax; v = unit.y / ax
            }
            ax >= ay && ax >= az -> {
                chart = GlobeChart.NEGATIVE_X; u = unit.z / ax; v = unit.y / ax
            }
            ay >= ax && ay >= az && unit.y >= 0.0 -> {
                chart = GlobeChart.POSITIVE_Y; u = unit.x / ay; v = -unit.z / ay
            }
            ay >= ax && ay >= az -> {
                chart = GlobeChart.NEGATIVE_Y; u = unit.x / ay; v = unit.z / ay
            }
            az >= ax && az >= ay && unit.z >= 0.0 -> {
                chart = GlobeChart.POSITIVE_Z; u = unit.x / az; v = unit.y / az
            }
            else -> {
                chart = GlobeChart.NEGATIVE_Z; u = -unit.x / az; v = unit.y / az
            }
        }
        val localU = (u + 1.0) * 0.5
        val localV = (v + 1.0) * 0.5
        // A 3×2 source layout covers T² exactly once. Its seams are intentionally visual;
        // neighbouring cube faces generally do not denote adjacent torus coordinates.
        val (column, row) = when (chart) {
            GlobeChart.POSITIVE_X -> 0 to 0
            GlobeChart.NEGATIVE_X -> 1 to 0
            GlobeChart.POSITIVE_Y -> 2 to 0
            GlobeChart.NEGATIVE_Y -> 0 to 1
            GlobeChart.POSITIVE_Z -> 1 to 1
            GlobeChart.NEGATIVE_Z -> 2 to 1
        }
        return GlobeSample(
            chart = chart,
            position = sourcePosition(period, (column + localU) / 3.0, (row + localV) / 2.0),
            seamWeight = seamWeight(localU, localV, blend),
        )
    }

    private fun equirectangular(direction: Vector3, period: HorizontalPeriod, blend: Double): GlobeSample {
        val unit = direction.normalized()
        val isPole = abs(unit.x) + abs(unit.z) < 1.0e-10
        val longitude = if (isPole) 0.0 else atan2(unit.z, unit.x) / (2.0 * Math.PI) + 0.5
        val latitude = acos(unit.y.coerceIn(-1.0, 1.0)) / Math.PI
        return GlobeSample(
            chart = if (unit.y >= 0.0) GlobeChart.POSITIVE_Y else GlobeChart.NEGATIVE_Y,
            position = sourcePosition(period, longitude, latitude),
            seamWeight = seamWeight(longitude, latitude, blend),
            polarSingularity = isPole,
        )
    }

    private fun sourcePosition(period: HorizontalPeriod, u: Double, v: Double): HorizontalPosition {
        val x = kotlin.math.floor(u * period.blocks).toLong()
        val z = kotlin.math.floor(v * period.blocks).toLong()
        return HorizontalPosition(period.canonical(x), period.canonical(z))
    }

    private fun seamWeight(u: Double, v: Double, blend: Double): Double {
        if (blend == 0.0) return 1.0
        val nearest = max(0.0, minOf(u, v, 1.0 - u, 1.0 - v))
        return (nearest / blend).coerceIn(0.0, 1.0)
    }
}
