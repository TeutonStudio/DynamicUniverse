package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.worldtype.Planet
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionLayer
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionStack
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldType

/**
 * Server-side source for the Universe definitions that are active in a save.
 *
 * The future world-creation persistence bridge owns registration. Commands and
 * portal adapters only query this registry, so neither subsystem needs to
 * recreate or mutate universe topology.
 */
interface UniverseRuntimeApi {
    fun register(universe: UniverseWorldType)
    fun clear()
    fun universe(id: String): UniverseWorldType?
    fun universes(): Collection<UniverseWorldType>
}

class UniverseRuntimeRegistry : UniverseRuntimeApi {
    private val universes = linkedMapOf<String, UniverseWorldType>()

    override fun register(universe: UniverseWorldType) {
        require(universes.putIfAbsent(universe.id, universe) == null) {
            "Universe ${universe.id} is already registered."
        }
    }

    override fun clear() {
        universes.clear()
    }

    override fun universe(id: String): UniverseWorldType? = universes[id]
    override fun universes(): Collection<UniverseWorldType> = universes.values.toList()
}

object UniverseRuntime {
    private val registry = UniverseRuntimeRegistry()

    fun api(): UniverseRuntimeApi = registry

    /** Called by the server lifecycle before a save is restored or discarded. */
    fun clear() = registry.clear()
}

data class RegisteredPlanet(
    val universe: UniverseWorldType,
    val planet: Planet,
)

data class RegisteredStack(
    val planet: RegisteredPlanet,
    val stack: PlanetDimensionStack,
)

data class RegisteredLayer(
    val stack: RegisteredStack,
    val layer: PlanetDimensionLayer,
    val index: Int,
)

/** Shared read-only topology lookup used by suggestions, diagnostics and navigation. */
object UniverseTopology {
    const val PLANET_REFERENCE_SEPARATOR: Char = '#'

    fun planets(runtime: UniverseRuntimeApi): List<RegisteredPlanet> = runtime.universes().flatMap { universe ->
        universe.galaxies.flatMap { galaxy -> galaxy.groups.flatMap { group ->
            group.allPlanets().map { planet -> RegisteredPlanet(universe, planet) }
        } }
    }

    fun planetReference(planet: RegisteredPlanet): String = "${planet.universe.id}$PLANET_REFERENCE_SEPARATOR${planet.planet.id}"

    fun resolvePlanet(runtime: UniverseRuntimeApi, reference: String): RegisteredPlanet? {
        val separator = reference.indexOf(PLANET_REFERENCE_SEPARATOR)
        if (separator >= 0) {
            val universe = runtime.universe(reference.substring(0, separator)) ?: return null
            val planetId = reference.substring(separator + 1)
            return planetsFor(universe).firstOrNull { it.id == planetId }?.let { RegisteredPlanet(universe, it) }
        }
        return planets(runtime).singleOrNull { it.planet.id == reference }
    }

    fun resolveStack(runtime: UniverseRuntimeApi, planetReference: String, stackId: String): RegisteredStack? =
        resolvePlanet(runtime, planetReference)?.let { planet ->
            planet.planet.stacks.firstOrNull { it.id == stackId }?.let { RegisteredStack(planet, it) }
        }

    fun resolveLayer(
        runtime: UniverseRuntimeApi,
        planetReference: String,
        stackId: String,
        layerId: String,
    ): RegisteredLayer? = resolveStack(runtime, planetReference, stackId)?.let { stack ->
        stack.stack.layersInnerToOuter.indexOfFirst { it.id == layerId }
            .takeIf { it >= 0 }
            ?.let { index -> RegisteredLayer(stack, stack.stack.layersInnerToOuter[index], index) }
    }

    fun layersForDimension(runtime: UniverseRuntimeApi, dimension: DimensionId): List<RegisteredLayer> =
        planets(runtime).flatMap { planet -> planet.planet.stacks.flatMap { stack ->
            stack.layersInnerToOuter.mapIndexedNotNull { index, layer ->
                if (layer.dimension == dimension) RegisteredLayer(RegisteredStack(planet, stack), layer, index) else null
            }
        } }

    private fun planetsFor(universe: UniverseWorldType): List<Planet> = universe.galaxies.flatMap { galaxy ->
        galaxy.groups.flatMap { it.allPlanets() }
    }
}
