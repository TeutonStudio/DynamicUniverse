package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity

/**
 * Common, server-authoritative transport calculation. Portal adapters apply this result to
 * a complete passenger tree in one transaction; no rider or vehicle is transferred alone.
 */
object PortalTraversalReconciler {
    fun reconcile(connection: DimensionConnection, state: TraversalState): TraversalState = state.copy(
        position = connection.targetPosition(state.position),
        velocity = connection.targetVelocity(state.velocity),
    )
}

data class TraversalState(
    val position: SpatialPosition,
    val velocity: SpatialVelocity,
    val passengerEntityIds: List<java.util.UUID> = emptyList(),
)
