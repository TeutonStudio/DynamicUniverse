package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import kotlin.math.floor

/**
 * A finite precision-friendly view of a region in continuous UniverseSpace.
 *
 * The bubble is a rendering/simulation locality aid, never a boundary or a new reference frame
 * for UniverseSpace. Global authoritative values remain unchanged while its origin rebases.
 */
data class SimulationBubble(
    val origin: Vector3,
    val radius: Double,
    val rebaseThreshold: Double = radius * 0.5,
    val rebaseGridSize: Double = rebaseThreshold,
) {
    init {
        require(radius > 0.0 && radius.isFinite()) { "Bubble radius must be finite and positive." }
        require(rebaseThreshold > 0.0 && rebaseThreshold <= radius && rebaseThreshold.isFinite()) {
            "The rebase threshold must be finite, positive, and inside the bubble."
        }
        require(rebaseGridSize > 0.0 && rebaseGridSize.isFinite()) { "The rebase grid size must be finite and positive." }
    }

    fun localPosition(universePosition: Vector3): Vector3 = universePosition - origin

    fun contains(universePosition: Vector3): Boolean = origin.distanceSquaredTo(universePosition) <= radius * radius

    fun rebaseFor(focusUniversePosition: Vector3): BubbleRebase? {
        if (origin.distanceSquaredTo(focusUniversePosition) <= rebaseThreshold * rebaseThreshold) return null
        val nextOrigin = Vector3(
            snapToGrid(focusUniversePosition.x),
            snapToGrid(focusUniversePosition.y),
            snapToGrid(focusUniversePosition.z),
        )
        return BubbleRebase(this, copy(origin = nextOrigin))
    }

    private fun snapToGrid(value: Double): Double = floor(value / rebaseGridSize) * rebaseGridSize
}

/** Translation applied to every bubble-local client or physics representation during a rebase. */
data class BubbleRebase(val previous: SimulationBubble, val next: SimulationBubble) {
    init {
        require(previous.radius == next.radius) { "A rebase must not resize the simulation bubble." }
        require(previous.rebaseThreshold == next.rebaseThreshold) { "A rebase must retain its threshold." }
        require(previous.rebaseGridSize == next.rebaseGridSize) { "A rebase must retain its grid." }
    }

    /** Add this vector to old local coordinates to obtain their equivalent new local coordinates. */
    val localTranslation: Vector3 get() = previous.origin - next.origin

    fun rebaseLocalPosition(previousLocalPosition: Vector3): Vector3 = previousLocalPosition + localTranslation
}
