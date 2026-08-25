package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnectionKind
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity
import kotlin.math.floor

/**
 * Server-authoritative coordinate conversion for the final sky ↔ UniverseSpace hand-off.
 *
 * It deliberately does not teleport entities or allocate chunks. Platform adapters must first
 * select/rebase an All-host bubble, then materialize these coordinates there. Keeping that split
 * prevents a client globe mesh from ever becoming traversal authority.
 */
class UniverseSpaceTraversalPlanner(
    private val manifest: UniverseGeometryManifest,
    private val state: UniverseRuntimeState,
) {
    fun exit(connection: DimensionConnection, local: TraversalState): UniverseSpaceExit? {
        if (connection.kind != DimensionConnectionKind.UNIVERSE_TRANSITION) return null
        val globe = manifest.celestialGlobes.singleOrNull { it.sourceDimension == connection.source } ?: return null
        val binding = state.planetBindings.singleOrNull { it.bodyId == globe.bodyId } ?: return null
        val kinematics = state.kinematicsFor(globe.bodyId) ?: return null
        val canonical = canonical(local.position, globe.period.blocks)
        val scaled = connection.targetPosition(canonical)
        val universePosition = binding.universePosition(Vector3(scaled.x, scaled.y, scaled.z), kinematics)
        val localVelocity = connection.targetVelocity(local.velocity)
        val universeVelocity = kinematics.orientation.rotate(Vector3(localVelocity.x, localVelocity.y, localVelocity.z)) + kinematics.velocity
        return UniverseSpaceExit(globe.bodyId, globe.sourceDimension, universePosition, universeVelocity, canonical)
    }

    fun enter(bodyId: String, universePosition: Vector3, universeVelocity: Vector3, arrivalY: Double): UniverseSpaceEntry? {
        val globe = manifest.celestialGlobes.singleOrNull { it.bodyId == bodyId } ?: return null
        val binding = state.planetBindings.singleOrNull { it.bodyId == bodyId } ?: return null
        val kinematics = state.kinematicsFor(bodyId) ?: return null
        val outerTarget = binding.localPosition(universePosition, kinematics)
        val local = SpatialPosition(outerTarget.x, arrivalY, outerTarget.z)
        val route = manifest.links.singleOrNull {
            it.kind == DimensionConnectionKind.UNIVERSE_TRANSITION && it.source == globe.sourceDimension
        } ?: return null
        val restored = route.inverse().targetPosition(local)
        val localVelocity = kinematics.orientation.inverse().rotate(universeVelocity - kinematics.velocity) * binding.localUnitsPerUniverseUnit
        val inverseVelocity = route.inverse().targetVelocity(SpatialVelocity(localVelocity.x, localVelocity.y, localVelocity.z))
        return UniverseSpaceEntry(
            bodyId = bodyId,
            target = globe.sourceDimension,
            position = canonical(restored, globe.period.blocks),
            velocity = inverseVelocity,
        )
    }

    private fun canonical(position: SpatialPosition, period: Long): SpatialPosition = position.copy(
        x = canonical(position.x, period),
        z = canonical(position.z, period),
    )

    private fun canonical(value: Double, period: Long): Double {
        val whole = floor(value).toLong()
        val fraction = value - whole
        val remainder = Math.floorMod(whole, period)
        return (if (remainder >= period / 2) remainder - period else remainder) + fraction
    }
}

data class UniverseSpaceExit(
    val bodyId: String,
    val source: DimensionId,
    val universePosition: Vector3,
    val universeVelocity: Vector3,
    /** Canonical source point retained for diagnostics and deterministic return corridors. */
    val canonicalSourcePosition: SpatialPosition,
)

data class UniverseSpaceEntry(
    val bodyId: String,
    val target: DimensionId,
    val position: SpatialPosition,
    val velocity: SpatialVelocity,
)
