package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.worldtype.VerticalLoop

/**
 * Server-neutral End re-entry rule. A platform adapter detects a vertical overflow and
 * applies the returned position without displaying a portal or world-selection hint.
 */
data class VerticalLoopBounds(val lowerY: Double, val upperY: Double) {
    init { require(lowerY.isFinite() && upperY.isFinite() && upperY > lowerY) }
}

object IsolatedUniverseTraversal {
    fun reenter(position: SpatialPosition, bounds: VerticalLoopBounds, loop: VerticalLoop): SpatialPosition? {
        if (loop != VerticalLoop.BOTH_DIRECTIONS) return null
        val height = bounds.upperY - bounds.lowerY
        val wrappedY = when {
            position.y < bounds.lowerY -> bounds.upperY - ((bounds.lowerY - position.y) % height)
            position.y >= bounds.upperY -> bounds.lowerY + ((position.y - bounds.upperY) % height)
            else -> return null
        }
        return position.copy(y = wrappedY)
    }
}
