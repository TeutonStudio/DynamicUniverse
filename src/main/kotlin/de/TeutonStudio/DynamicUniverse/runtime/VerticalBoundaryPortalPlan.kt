package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId

/** Inclusive lower and exclusive upper Minecraft build-height limits. */
data class VerticalPortalLevelBounds(val minY: Int, val maxYExclusive: Int) {
    init { require(maxYExclusive > minY) }
}

/** The visible side of a horizontal portal from within its source level. */
enum class VerticalPortalSurface(val expectedNormalY: Int) {
    /** A ceiling portal is visible from below and therefore faces downward. */
    CEILING(-1),
    /** A floor portal is visible from above and therefore faces upward. */
    FLOOR(1),
}

/**
 * Materialization-neutral portal definition. The destination is deliberately inside the
 * target's legal build range: some dimensions (notably Aether) treat their exact lower bound
 * as a falling-out transition before an arriving player can take their next upward tick.
 */
data class VerticalBoundaryPortalPlan(
    val source: DimensionId,
    val target: DimensionId,
    val sourceBounds: VerticalPortalLevelBounds,
    val targetBounds: VerticalPortalLevelBounds,
    val sourceSurface: VerticalPortalSurface,
    val entryClearance: Int = DEFAULT_ENTRY_CLEARANCE,
) {
    init {
        require(entryClearance > 0)
        require(targetBounds.maxYExclusive - targetBounds.minY > entryClearance * 2) {
            "Target level is too short for a safe vertical portal entry."
        }
    }

    /** Bounds passed to Immersive Portals' native vertical connector. */
    fun nativeTargetBounds(): VerticalPortalLevelBounds = when (sourceSurface) {
        VerticalPortalSurface.CEILING -> targetBounds.copy(minY = targetBounds.minY + entryClearance)
        VerticalPortalSurface.FLOOR -> targetBounds.copy(maxYExclusive = targetBounds.maxYExclusive - entryClearance)
    }

    companion object {
        const val DEFAULT_ENTRY_CLEARANCE = 4
    }
}

object VerticalBoundaryPortalPlans {
    fun seam(
        lower: DimensionId,
        lowerBounds: VerticalPortalLevelBounds,
        upper: DimensionId,
        upperBounds: VerticalPortalLevelBounds,
    ): List<VerticalBoundaryPortalPlan> = listOf(
        VerticalBoundaryPortalPlan(lower, upper, lowerBounds, upperBounds, VerticalPortalSurface.CEILING),
        VerticalBoundaryPortalPlan(upper, lower, upperBounds, lowerBounds, VerticalPortalSurface.FLOOR),
    )

    fun loop(dimension: DimensionId, bounds: VerticalPortalLevelBounds): List<VerticalBoundaryPortalPlan> = listOf(
        VerticalBoundaryPortalPlan(dimension, dimension, bounds, bounds, VerticalPortalSurface.CEILING),
        VerticalBoundaryPortalPlan(dimension, dimension, bounds, bounds, VerticalPortalSurface.FLOOR),
    )
}
