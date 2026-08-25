package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import kotlin.math.floor

/** A finite local view of R³. It is a precision aid, never a UniverseSpace boundary. */
data class SimulationBubble(
    val origin: Vector3,
    val radius: Double,
    val rebaseThreshold: Double = radius * 0.5,
    val rebaseGridSize: Double = rebaseThreshold,
) {
    init {
        require(radius > 0.0 && radius.isFinite()) { "Bubble radius must be finite and positive." }
        require(rebaseThreshold > 0.0 && rebaseThreshold <= radius && rebaseThreshold.isFinite()) { "Invalid rebase threshold." }
        require(rebaseGridSize > 0.0 && rebaseGridSize.isFinite()) { "Invalid rebase grid size." }
    }

    fun localPosition(universePosition: Vector3) = universePosition - origin
    fun contains(universePosition: Vector3) = origin.distanceSquaredTo(universePosition) <= radius * radius
    fun rebaseFor(focus: Vector3): BubbleRebase? {
        if (origin.distanceSquaredTo(focus) <= rebaseThreshold * rebaseThreshold) return null
        val next = Vector3(snap(focus.x), snap(focus.y), snap(focus.z))
        return BubbleRebase(this, copy(origin = next))
    }
    private fun snap(value: Double) = floor(value / rebaseGridSize) * rebaseGridSize
}

data class BubbleRebase(val previous: SimulationBubble, val next: SimulationBubble) {
    init {
        require(previous.radius == next.radius && previous.rebaseThreshold == next.rebaseThreshold && previous.rebaseGridSize == next.rebaseGridSize) {
            "A rebase may only change the origin."
        }
    }
    val localTranslation: Vector3 get() = previous.origin - next.origin
    fun rebaseLocalPosition(previousLocal: Vector3) = previousLocal + localTranslation
}

@JvmInline value class UniverseHostSlot(val value: String) { init { require(value.isNotBlank()) } }

data class HostedSimulationBubble(val slot: UniverseHostSlot, val bubble: SimulationBubble) {
    init { require(slot.value.matches(Regex("[a-z0-9_.-]+"))) { "Host slot must be stable and simple." } }
}

/** Object-to-bubble relation. An object may be in more than one overlapping bubble. */
data class BubbleMembership(val objectId: String, val slot: UniverseHostSlot) {
    init { require(objectId.isNotBlank()) }
}

/** Reserved local host area for Sable's Plotyard; it must not overlap active host bubbles. */
data class SablePlotyardReservation(val center: Vector3, val radius: Double) {
    init { require(radius > 0.0 && radius.isFinite()) }
    fun intersects(bubble: SimulationBubble) = center.distanceSquaredTo(bubble.origin) < (radius + bubble.radius) * (radius + bubble.radius)
}

/** Pure layout operations used by future client/physics adapters. */
data class UniverseHostLayout(
    val bubbles: List<HostedSimulationBubble> = emptyList(),
    val memberships: List<BubbleMembership> = emptyList(),
    val sablePlotyard: SablePlotyardReservation? = null,
) {
    init {
        require(bubbles.map { it.slot }.distinct().size == bubbles.size) { "Host slots must be unique." }
        require(memberships.all { membership -> bubbles.any { it.slot == membership.slot } }) { "Membership refers to an unknown slot." }
        require(memberships.distinct().size == memberships.size) { "Bubble membership must be unique." }
        require(sablePlotyard == null || bubbles.none { sablePlotyard.intersects(it.bubble) }) { "A bubble collides with the Sable Plotyard reservation." }
    }

    fun bubble(slot: UniverseHostSlot) = bubbles.singleOrNull { it.slot == slot }?.bubble
    fun membershipsFor(objectId: String) = memberships.filter { it.objectId == objectId }.map { it.slot }
    fun mergeable(first: UniverseHostSlot, second: UniverseHostSlot): Boolean {
        val left = bubble(first) ?: return false
        val right = bubble(second) ?: return false
        return left.origin.distanceSquaredTo(right.origin) <= (left.radius + right.radius) * (left.radius + right.radius)
    }
    fun rebase(slot: UniverseHostSlot, focus: Vector3): Pair<UniverseHostLayout, BubbleRebase>? {
        val previous = bubble(slot) ?: return null
        val rebase = previous.rebaseFor(focus) ?: return null
        return copy(bubbles = bubbles.map { if (it.slot == slot) it.copy(bubble = rebase.next) else it }) to rebase
    }
}
