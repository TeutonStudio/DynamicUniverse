package de.TeutonStudio.DynamicUniverse.client.render

import de.TeutonStudio.DynamicUniverse.cosmos.GlobeVisualConfiguration
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.runtime.CelestialGlobeKind
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod

/** Client copy of the server-authoritative globe presentation list. */
data class GlobeRenderBody(
    val bodyId: String,
    val kind: CelestialGlobeKind,
    val sourceDimension: DimensionId,
    val period: HorizontalPeriod,
    val visual: GlobeVisualConfiguration,
    val universePosition: Vector3,
)

/**
 * Deliberately contains presentation state only. Source chunks and terrain tiles are acquired
 * through an authorizing provider; a globe list alone must never expose remote world data.
 */
object GlobeRenderContext {
    @Volatile private var bodiesById: Map<String, GlobeRenderBody> = emptyMap()

    fun install(bodies: Collection<GlobeRenderBody>) {
        require(bodies.map(GlobeRenderBody::bodyId).distinct().size == bodies.size) { "Globe ids must be unique." }
        bodiesById = bodies.associateBy(GlobeRenderBody::bodyId)
    }

    fun clear() { bodiesById = emptyMap() }
    fun body(id: String): GlobeRenderBody? = bodiesById[id]
    fun bodies(): List<GlobeRenderBody> = bodiesById.values.toList()
}
