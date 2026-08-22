package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.CosmicSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.PlanetSpaceBinding
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseSpace

/**
 * Versioned, Minecraft-independent persistence boundary for a UniverseHost.
 * A NeoForge SavedData adapter may serialize this immutable value without changing runtime rules.
 */
data class UniverseRuntimeSnapshot(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val hostId: String,
    val universeSpaceId: String,
    val objects: List<CosmicSpatialObject>,
    val planetBindings: List<PlanetSpaceBinding>,
) {
    init {
        require(formatVersion == CURRENT_FORMAT_VERSION) { "Unsupported universe runtime snapshot version: $formatVersion" }
        require(objects.map(CosmicSpatialObject::id).distinct().size == objects.size) { "Snapshot object ids must be unique." }
        require(planetBindings.map(PlanetSpaceBinding::planetId).distinct().size == planetBindings.size) {
            "Snapshot planet bindings must be unique."
        }
        require(planetBindings.all { it.universeSpace.id == universeSpaceId }) {
            "Every planet binding must belong to the snapshot's UniverseSpace."
        }
    }

    companion object { const val CURRENT_FORMAT_VERSION = 1 }
}

/** In-memory seam shared by the host and a future SavedData/NBT adapter. */
class UniverseRuntimePersistence {
    fun snapshot(host: UniverseHost, planetBindings: Collection<PlanetSpaceBinding>): UniverseRuntimeSnapshot =
        UniverseRuntimeSnapshot(
            hostId = host.id,
            universeSpaceId = host.space.id,
            objects = host.objects(),
            planetBindings = planetBindings.toList(),
        )

    fun restore(snapshot: UniverseRuntimeSnapshot): RestoredUniverseRuntime {
        val host = UniverseHost(snapshot.hostId, UniverseSpace(snapshot.universeSpaceId))
        snapshot.objects.forEach(host::register)
        return RestoredUniverseRuntime(host, snapshot.planetBindings)
    }
}

data class RestoredUniverseRuntime(val host: UniverseHost, val planetBindings: List<PlanetSpaceBinding>)
