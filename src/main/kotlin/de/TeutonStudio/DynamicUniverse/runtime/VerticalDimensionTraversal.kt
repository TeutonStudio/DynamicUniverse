package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity
import de.TeutonStudio.DynamicUniverse.worldtype.VerticalDimensionSeam

/** Actual build-height interval: lower is inclusive, upper is exclusive. */
data class VerticalDimensionBounds(val lowerY: Double, val upperY: Double) {
    init { require(lowerY.isFinite() && upperY.isFinite() && upperY > lowerY) }
    fun clampInside(y: Double) = y.coerceIn(lowerY, upperY - 0.001)
}

data class VerticalDimensionTraversal(val target: DimensionId, val state: TraversalState)

/** Pure transition authority shared by End re-entry and optional cross-dimension air seams. */
class VerticalDimensionTraversalPlanner(private val manifest: UniverseGeometryManifest) {
    fun traverse(
        dimension: DimensionId,
        bounds: VerticalDimensionBounds,
        state: TraversalState,
        boundsFor: (DimensionId) -> VerticalDimensionBounds?,
    ): VerticalDimensionTraversal? {
        // UniverseSpace is not an isolated Minecraft-style dimension. Its finite host level is
        // rebased by the R³ host adapter before a native build-height boundary can be reached.
        // In particular, it must never inherit the End's vertical loop.
        if (dimension.value == manifest.universe.id) return null
        manifest.isolatedUniverses.singleOrNull { it.dimension == dimension }?.let { isolated ->
            val position = IsolatedUniverseTraversal.reenter(
                state.position,
                VerticalLoopBounds(bounds.lowerY, bounds.upperY),
                isolated.verticalLoop,
            ) ?: return@let
            return VerticalDimensionTraversal(dimension, state.copy(position = position))
        }

        val seam = manifest.verticalSeams.singleOrNull { seam ->
            (seam.lowerDimension == dimension && state.position.y >= bounds.upperY) ||
                (seam.upperDimension == dimension && state.position.y < bounds.lowerY)
        } ?: return null
        val ascending = seam.lowerDimension == dimension
        val target = if (ascending) seam.upperDimension else seam.lowerDimension
        val targetBounds = boundsFor(target) ?: return null
        return VerticalDimensionTraversal(target, seam.traverse(ascending, bounds, targetBounds, state))
    }
}

private fun VerticalDimensionSeam.traverse(
    ascending: Boolean,
    sourceBounds: VerticalDimensionBounds,
    targetBounds: VerticalDimensionBounds,
    state: TraversalState,
): TraversalState {
    val scale = if (ascending) coordinateScale.asDouble() else coordinateScale.inverse().asDouble()
    val position = state.position
    val mappedY = if (ascending) {
        targetBounds.lowerY + (position.y - sourceBounds.upperY)
    } else {
        targetBounds.upperY - (sourceBounds.lowerY - position.y)
    }
    return state.copy(
        position = SpatialPosition(position.x * scale, targetBounds.clampInside(mappedY), position.z * scale),
        velocity = SpatialVelocity(state.velocity.x * scale, state.velocity.y, state.velocity.z * scale),
    )
}

object VerticalDimensionTransitionRuntime {
    @Volatile private var planner: VerticalDimensionTraversalPlanner? = null
    fun install(manifest: UniverseGeometryManifest) { planner = VerticalDimensionTraversalPlanner(manifest) }
    fun clear() { planner = null }
    fun traverse(
        dimension: DimensionId,
        bounds: VerticalDimensionBounds,
        state: TraversalState,
        boundsFor: (DimensionId) -> VerticalDimensionBounds?,
    ): VerticalDimensionTraversal? = planner?.traverse(dimension, bounds, state, boundsFor)
}
