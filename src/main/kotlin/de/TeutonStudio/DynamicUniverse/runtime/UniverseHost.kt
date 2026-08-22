package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.CosmicSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.DynamicCosmos
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseSpace

/**
 * Technical owner of one runtime simulation. It deliberately contains no galaxy, planet-stack,
 * portal, or world-creation semantics; those configurations are clients of this host.
 */
class UniverseHost(
    val id: String,
    val space: UniverseSpace,
    private val cosmos: DynamicCosmos = DynamicCosmos(),
) {
    init {
        require(id.matches(Regex("[a-z0-9_.-]+:[a-z0-9_./-]+"))) { "Universe-host id must be namespaced." }
    }

    fun register(spatialObject: CosmicSpatialObject) = cosmos.register(spatialObject)

    fun objects(): List<CosmicSpatialObject> = cosmos.spatialSnapshot()

    fun objectById(id: String): CosmicSpatialObject? = cosmos.spatialObject(id)

    fun tick(seconds: Double) = cosmos.tick(seconds)
}
