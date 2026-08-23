package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity
import kotlin.math.floor

/**
 * Immutable transition authority shared by the lightweight server fallback and optional
 * render adapters. It never creates chunks, entities, or portals: those are adapters.
 */
class UniverseTransitionPlanner(private val manifest: UniverseGeometryManifest) {
    fun horizontal(dimension: DimensionId, state: TraversalState): TraversalState? {
        val layer = manifest.layers.singleOrNull { it.dimension == dimension } ?: return null
        val x = canonical(state.position.x, layer.period.blocks)
        val z = canonical(state.position.z, layer.period.blocks)
        return if (x == state.position.x && z == state.position.z) null
        else state.copy(position = state.position.copy(x = x, z = z))
    }

    fun vertical(connection: DimensionConnection, state: TraversalState): DimensionTraversal = DimensionTraversal(
        connection.target,
        PortalTraversalReconciler.reconcile(connection, state),
    )

    private fun canonical(value: Double, period: Long): Double {
        val whole = floor(value).toLong()
        val fraction = value - whole
        val remainder = Math.floorMod(whole, period)
        val canonical = if (remainder >= period / 2) remainder - period else remainder
        return canonical + fraction
    }
}

data class DimensionTraversal(val target: DimensionId, val state: TraversalState)

/** Server lifecycle owner; adapters may query it but cannot mutate its manifest. */
object UniverseTransitionRuntime {
    @Volatile private var planner: UniverseTransitionPlanner? = null

    fun install(manifest: UniverseGeometryManifest) { planner = UniverseTransitionPlanner(manifest) }
    fun clear() { planner = null }
    fun horizontal(dimension: DimensionId, state: TraversalState): TraversalState? = planner?.horizontal(dimension, state)
    fun vertical(connection: DimensionConnection, state: TraversalState): DimensionTraversal? = planner?.vertical(connection, state)
}
