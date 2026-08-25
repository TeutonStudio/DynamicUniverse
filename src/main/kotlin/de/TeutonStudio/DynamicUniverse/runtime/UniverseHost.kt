package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.CelestialSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.DynamicCosmos
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseSpace

/** Technical runtime host for one semantic R³; Minecraft's All level is only its attachment point. */
class UniverseHost(
    val id: String,
    val space: UniverseSpace,
    private val cosmos: DynamicCosmos = DynamicCosmos(),
    var layout: UniverseHostLayout = UniverseHostLayout(),
) {
    init { require(id.matches(Regex("[a-z0-9_.-]+:[a-z0-9_./-]+"))) { "Universe-host id must be namespaced." } }
    fun register(object_: CelestialSpatialObject) = cosmos.register(object_)
    fun objects(): List<CelestialSpatialObject> = cosmos.spatialSnapshot()
    fun objectById(id: String) = cosmos.spatialObject(id)
    fun tick(seconds: Double) = cosmos.tick(seconds)
}
