package de.TeutonStudio.DynamicUniverse.command.service

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.runtime.UniverseRuntime
import de.TeutonStudio.DynamicUniverse.runtime.UniverseRuntimeApi
import de.TeutonStudio.DynamicUniverse.runtime.UniverseTopology

data class CommandFeedback(val lines: List<String>, val successful: Boolean = true)

interface UniverseQueryService {
    fun inspectUniverse(universeId: String? = null): CommandFeedback
    fun inspectPlanet(planetReference: String): CommandFeedback
    fun inspectStack(planetReference: String, stackId: String): CommandFeedback
    fun inspectLayer(planetReference: String, stackId: String, layerId: String): CommandFeedback
    fun inspectPosition(dimensionId: String, x: Double, y: Double, z: Double): CommandFeedback
}

class RegistryUniverseQueryService(
    private val runtime: UniverseRuntimeApi = UniverseRuntime.api(),
) : UniverseQueryService {
    override fun inspectUniverse(universeId: String?): CommandFeedback {
        if (universeId == null) {
            val universes = runtime.universes()
            return CommandFeedback(
                if (universes.isEmpty()) listOf("No active Universe is registered for this save.")
                else listOf("Active Universes: ${universes.size}") + universes.map { "- ${it.id}" },
            )
        }
        val universe = runtime.universe(universeId) ?: return unknownUniverse(universeId)
        val planets = UniverseTopology.planets(runtime).filter { it.universe.id == universe.id }
        return CommandFeedback(listOf(
            "Universe: ${universe.id}",
            "Universe dimension: ${universe.universeDimension.value}",
            "Galaxies: ${universe.galaxies.size}",
            "Planets and moons: ${planets.size}",
        ))
    }

    override fun inspectPlanet(planetReference: String): CommandFeedback {
        val registered = UniverseTopology.resolvePlanet(runtime, planetReference) ?: return unknownPlanet(planetReference)
        return CommandFeedback(listOf(
            "Planet: ${registered.planet.id}",
            "Universe: ${registered.universe.id}",
            "Core size: ${registered.planet.planetCoreSize}",
            "Stacks: ${registered.planet.stacks.joinToString { it.id }}",
        ))
    }

    override fun inspectStack(planetReference: String, stackId: String): CommandFeedback {
        val registered = UniverseTopology.resolveStack(runtime, planetReference, stackId)
            ?: return unknownStack(planetReference, stackId)
        return CommandFeedback(listOf(
            "Stack: ${registered.stack.id}",
            "Planet: ${registered.planet.planet.id}",
            "Outer-to-Universe scale: ${registered.stack.outerToUniverseScale.numerator}/${registered.stack.outerToUniverseScale.denominator}",
        ) + registered.stack.layersInnerToOuter.mapIndexed { index, layer ->
            "[$index] ${layer.id}: ${layer.dimension.value} (${layer.role})"
        })
    }

    override fun inspectLayer(planetReference: String, stackId: String, layerId: String): CommandFeedback {
        val registered = UniverseTopology.resolveLayer(runtime, planetReference, stackId, layerId)
            ?: return unknownLayer(planetReference, stackId, layerId)
        val layers = registered.stack.stack.layersInnerToOuter
        val lower = layers.getOrNull(registered.index - 1)?.id ?: "planet core"
        val upper = layers.getOrNull(registered.index + 1)?.id ?: "universe (${registered.stack.planet.universe.universeDimension.value})"
        val scale = registered.layer.toOuterScale?.let { "${it.numerator}/${it.denominator}" } ?: "stack outer scale"
        return CommandFeedback(listOf(
            "Layer: ${registered.layer.id}",
            "Dimension: ${registered.layer.dimension.value}",
            "Role: ${registered.layer.role}",
            "Below: $lower",
            "Above: $upper",
            "Scale to outer neighbour: $scale",
            "Boundaries: ${registered.layer.boundaries.inner} -> ${registered.layer.boundaries.outer}",
        ))
    }

    override fun inspectPosition(dimensionId: String, x: Double, y: Double, z: Double): CommandFeedback {
        val dimension = runCatching { DimensionId(dimensionId) }.getOrNull()
            ?: return CommandFeedback(listOf("Invalid dimension id: $dimensionId"), false)
        val layers = UniverseTopology.layersForDimension(runtime, dimension)
        val universe = runtime.universes().firstOrNull { it.universeDimension == dimension }
        val context = when {
            layers.size == 1 -> "Planet: ${layers.single().stack.planet.planet.id}; stack: ${layers.single().stack.stack.id}; layer: ${layers.single().layer.id}"
            layers.size > 1 -> "Dimension belongs to multiple active layers; runtime configuration is invalid."
            universe != null -> "Universe space: ${universe.id}"
            else -> "Dimension is not part of an active Universe topology."
        }
        return CommandFeedback(listOf("Minecraft dimension: $dimensionId", "Position: x=$x, y=$y, z=$z", context), layers.size <= 1)
    }

    private fun unknownUniverse(id: String) = CommandFeedback(listOf("Universe $id is not registered."), false)
    private fun unknownPlanet(id: String) = CommandFeedback(listOf("Planet $id is not registered or is ambiguous; use <universe>#<planet>."), false)
    private fun unknownStack(planet: String, stack: String) = CommandFeedback(listOf("Stack $stack is not registered for $planet."), false)
    private fun unknownLayer(planet: String, stack: String, layer: String) = CommandFeedback(listOf("Layer $layer is not registered for $planet/$stack."), false)
}
