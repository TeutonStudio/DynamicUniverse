package de.TeutonStudio.DynamicUniverse.worldtype

import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnectionGraph
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaries
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.dimension.PlanetDimensionStackValidator

/** The configurable Universe world type. It has no client or portal-mod dependency. */
data class UniverseWorldType(
    val id: String,
    val universeDimension: DimensionId,
    val galaxies: List<Galaxy>,
) {
    init {
        require(id.matches(Regex("[a-z0-9_.-]+:[a-z0-9_./-]+"))) { "Universe id must be namespaced." }
        require(galaxies.isNotEmpty()) { "A Universe needs at least one galaxy." }
        require(galaxies.map(Galaxy::id).distinct().size == galaxies.size) { "Galaxy ids must be unique." }

        val planets = galaxies.flatMap { galaxy -> galaxy.groups.flatMap(CelestialGroup::planets) }
        require(planets.map(Planet::id).distinct().size == planets.size) { "Planet ids must be unique per Universe." }
    }

    fun connectionGraph(): DimensionConnectionGraph = DimensionConnectionGraph(
        galaxies.flatMap { galaxy -> galaxy.groups.flatMap(CelestialGroup::planets) }
            .flatMap { planet -> planet.connectionsTo(universeDimension) },
    )
}

data class Galaxy(
    val id: String,
    val groups: List<CelestialGroup>,
) {
    init {
        requireNodeId(id, "Galaxy")
        require(groups.isNotEmpty()) { "A galaxy needs at least one celestial group." }
        require(groups.map(CelestialGroup::id).distinct().size == groups.size) { "Celestial group ids must be unique per galaxy." }
    }
}

enum class CelestialGroupKind { SOLAR_SYSTEM, CLOUD }

data class CelestialGroup(
    val id: String,
    val kind: CelestialGroupKind,
    val star: Star? = null,
    val planets: List<Planet>,
) {
    init {
        requireNodeId(id, "Celestial group")
        require(planets.map(Planet::id).distinct().size == planets.size) { "Planet ids must be unique per celestial group." }
        when (kind) {
            CelestialGroupKind.SOLAR_SYSTEM -> requireNotNull(star) { "A solar system needs exactly one star." }
            CelestialGroupKind.CLOUD -> require(star == null) { "A cloud cannot own a star." }
        }
    }
}

data class Star(val id: String) {
    init {
        requireNodeId(id, "Star")
    }
}

data class Planet(
    val id: String,
    val planetCoreSize: Double,
    val stacks: List<PlanetDimensionStack>,
) {
    init {
        requireNodeId(id, "Planet")
        require(planetCoreSize > 0.0 && planetCoreSize.isFinite()) { "Planet-core size must be finite and positive." }
        require(stacks.isNotEmpty()) { "A planet needs at least one dimension stack." }
        require(stacks.map(PlanetDimensionStack::id).distinct().size == stacks.size) { "Stack ids must be unique per planet." }
    }

    internal fun connectionsTo(universeDimension: DimensionId): List<DimensionConnection> = stacks.flatMap { stack ->
        val radial = stack.layersInnerToOuter.windowed(2).map { (inner, outer) ->
            DimensionConnection(
                id = "$id/${stack.id}/${inner.id}-to-${outer.id}",
                source = inner.dimension,
                target = outer.dimension,
                scale = requireNotNull(inner.toOuterScale),
            )
        }
        radial + DimensionConnection(
            id = "$id/${stack.id}/${stack.layersInnerToOuter.last().id}-to-universe",
            source = stack.layersInnerToOuter.last().dimension,
            target = universeDimension,
            scale = stack.outerToUniverseScale,
        )
    }
}

data class PlanetDimensionStack(
    val id: String,
    val layersInnerToOuter: List<PlanetDimensionLayer>,
    val outerToUniverseScale: DimensionScale = DimensionScale.ONE,
) {
    init {
        requireNodeId(id, "Stack")
        require(layersInnerToOuter.size >= 2) { "A stack needs a planet core and a sky layer." }
        require(layersInnerToOuter.first().role == PlanetDimensionRole.PLANET_CORE) { "The innermost layer must be the planet core." }
        require(layersInnerToOuter.last().role == PlanetDimensionRole.SKY) { "The outermost layer must be the sky layer." }
        require(layersInnerToOuter.map(PlanetDimensionLayer::id).distinct().size == layersInnerToOuter.size) {
            "Layer ids must be unique per stack."
        }
        layersInnerToOuter.dropLast(1).forEach { layer ->
            requireNotNull(layer.toOuterScale) { "Every inner boundary needs an explicit dimension scale." }
        }
        require(layersInnerToOuter.last().toOuterScale == null) { "The sky layer's next boundary is the stack scale." }
        require(PlanetDimensionStackValidator.incompatibleTransitions(layersInnerToOuter.map(PlanetDimensionLayer::boundaries)).isEmpty()) {
            "Adjacent dimension boundaries must match (AIR-to-AIR or BEDROCK-to-BEDROCK)."
        }
    }
}

enum class PlanetDimensionRole { PLANET_CORE, INNER, SURFACE, SKY, CUSTOM }

data class PlanetDimensionLayer(
    val id: String,
    val role: PlanetDimensionRole,
    val dimension: DimensionId,
    val toOuterScale: DimensionScale? = null,
    val boundaries: DimensionBoundaries = DimensionBoundaries.AIR_TO_AIR,
) {
    init {
        requireNodeId(id, "Dimension layer")
        val isSurfaceBoundary = boundaries.inner != boundaries.outer
        require((role == PlanetDimensionRole.SURFACE) == isSurfaceBoundary) {
            "Only a surface may have BEDROCK on one edge and AIR on the other."
        }
    }
}

private fun requireNodeId(id: String, type: String) {
    require(id.matches(Regex("[a-z0-9_.-]+"))) { "Invalid $type id: $id" }
}
