package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.CelestialSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.PlanetSpaceBinding
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseKinematicState
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseSpace

/** Versioned authoritative state: geometry names local spaces; this owns their moving state. */
data class UniverseRuntimeState(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val hostId: String,
    val universeSpaceId: String,
    val objects: List<CelestialSpatialObject> = emptyList(),
    val planetBindings: List<PlanetSpaceBinding> = emptyList(),
    val planetKinematics: List<PlanetKinematicState> = emptyList(),
    val hostLayout: UniverseHostLayout = UniverseHostLayout(),
) {
    init {
        require(formatVersion == CURRENT_FORMAT_VERSION) { "Unsupported universe runtime format: $formatVersion" }
        require(objects.map { it.id }.distinct().size == objects.size) { "Spatial object ids must be unique." }
        require(planetBindings.map { it.planetId }.distinct().size == planetBindings.size) { "Planet bindings must be unique." }
        require(planetKinematics.map { it.planetId }.distinct().size == planetKinematics.size) { "Planet kinematics must be unique." }
        require(planetBindings.all { it.universeSpace.id == universeSpaceId }) { "Bindings must belong to the host space." }
    }
    fun kinematicsFor(planetId: String) = planetKinematics.singleOrNull { it.planetId == planetId }?.kinematics
    fun restoreHost(): UniverseHost = UniverseHost(hostId, UniverseSpace(universeSpaceId), layout = hostLayout).also { host -> objects.forEach(host::register) }
    companion object { const val CURRENT_FORMAT_VERSION = 1 }
}

data class PlanetKinematicState(val planetId: String, val kinematics: UniverseKinematicState) { init { require(planetId.isNotBlank()) } }

/** Kept for callers from the earlier branch; now snapshots the complete multi-bubble state. */
typealias UniverseRuntimeSnapshot = UniverseRuntimeState

class UniverseRuntimePersistence {
    fun snapshot(host: UniverseHost, planetBindings: Collection<PlanetSpaceBinding>, planetKinematics: Collection<PlanetKinematicState> = emptyList()) =
        UniverseRuntimeState(hostId = host.id, universeSpaceId = host.space.id, objects = host.objects(), planetBindings = planetBindings.toList(), planetKinematics = planetKinematics.toList(), hostLayout = host.layout)
    fun restore(snapshot: UniverseRuntimeState) = RestoredUniverseRuntime(snapshot.restoreHost(), snapshot.planetBindings, snapshot.planetKinematics)
}

data class RestoredUniverseRuntime(val host: UniverseHost, val planetBindings: List<PlanetSpaceBinding>, val planetKinematics: List<PlanetKinematicState>)
