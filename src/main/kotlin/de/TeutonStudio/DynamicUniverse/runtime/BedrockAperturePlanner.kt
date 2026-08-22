package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnectionGraph
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnectionKind
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionPosition

/** A generated Bedrock plane. The generator adapter, not a hard-coded world height, supplies it. */
data class BedrockBoundaryPlane(
    val dimension: DimensionId,
    val face: DimensionBoundaryFace,
    val y: Int,
)

/** The two blocks which become one radial opening after a permitted Bedrock break. */
data class BedrockAperture(
    val connection: DimensionConnection,
    val source: DimensionPosition,
    val target: DimensionPosition,
)

/**
 * Server-authoritative selection of a Bedrock aperture. It deliberately knows no Minecraft
 * classes, so both world generation and the NeoForge event adapter use precisely the same rule.
 */
class BedrockAperturePlanner(
    connections: Collection<DimensionConnection>,
    planes: Collection<BedrockBoundaryPlane>,
) {
    private val graph = DimensionConnectionGraph(connections)
    private val planeByEndpoint = planes.associateBy { it.dimension to it.face }

    init {
        require(planeByEndpoint.size == planes.size) {
            "A dimension boundary may only declare one Bedrock plane."
        }
    }

    fun apertureFor(sourceDimension: DimensionId, source: DimensionPosition): BedrockAperture? {
        val candidates = graph.routesFrom(sourceDimension).filter { route ->
            route.kind == DimensionConnectionKind.RADIAL_BOUNDARY &&
                route.boundarySurface == BoundarySurface.BEDROCK &&
                planeByEndpoint[sourceDimension to route.sourceBoundaryFace]?.y?.toLong() == source.y &&
                planeByEndpoint[route.target to route.targetBoundaryFace] != null
        }
        val connection = candidates.singleOrNull() ?: return null
        val targetPlane = requireNotNull(planeByEndpoint[connection.target to connection.targetBoundaryFace])
        val mapped = connection.targetPosition(source)
        return BedrockAperture(connection, source, mapped.copy(y = targetPlane.y.toLong()))
    }
}
