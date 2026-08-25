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

    /**
     * Rebase at the exact focus point when the finite Minecraft host is close to one of its
     * native build limits. Unlike [rebaseFor], this does not snap: it must be able to recover
     * from any Y coordinate without turning R³ into a periodic vertical world.
     */
    fun rebaseAt(focus: Vector3): BubbleRebase = BubbleRebase(this, copy(origin = focus))
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

/** Safe local interval inside the invisible finite Minecraft attachment of one R³ bubble. */
data class UniverseHostLocalBounds(
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
    val minZ: Double,
    val maxZ: Double,
) {
    init {
        require(minX < maxX && minY < maxY && minZ < maxZ) { "Universe host bounds must be non-empty." }
    }

    fun contains(local: Vector3) = local.x in minX..maxX && local.y in minY..maxY && local.z in minZ..maxZ

    companion object {
        /** Conservative interior of a standard 1.21 dimension; adapters may choose tighter limits. */
        fun aroundOrigin(horizontal: Double = 8_192.0, lowerY: Double = -32.0, upperY: Double = 288.0) =
            UniverseHostLocalBounds(-horizontal, horizontal, lowerY, upperY, -horizontal, horizontal)
    }
}

/**
 * Converts an imminent finite-host overflow into a translation of the local attachment frame.
 * The returned local coordinate is normally zero, while `globalPosition` is bit-for-bit the
 * position before rebasing. No modulo operation exists on any axis.
 */
data class UniverseHostRebasePlan(
    val rebase: BubbleRebase,
    val globalPosition: Vector3,
    val rebasedLocalPosition: Vector3,
)

object UniverseHostRebasePlanner {
    fun plan(bubble: SimulationBubble, localPosition: Vector3, bounds: UniverseHostLocalBounds): UniverseHostRebasePlan? {
        if (bounds.contains(localPosition)) return null
        val global = bubble.origin + localPosition
        val rebase = bubble.rebaseAt(global)
        return UniverseHostRebasePlan(rebase, global, rebase.next.localPosition(global))
    }
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
