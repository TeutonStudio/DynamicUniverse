package de.TeutonStudio.DynamicUniverse.worldtype

import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnectionGraph
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface
import de.TeutonStudio.DynamicUniverse.dimension.LocalEuclideanPortal
import de.TeutonStudio.DynamicUniverse.dimension.LocalEuclideanPortalGraph
import de.TeutonStudio.DynamicUniverse.dimension.LocalPortalEndpoint
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnectionKind

/** The configurable Universe world type. It has no client or portal-mod dependency. */
data class UniverseWorldType(
    val id: String,
    val universeDimension: DimensionId,
    val galaxies: List<Galaxy>,
    val isolatedUniverses: List<IsolatedUniverseDefinition> = listOf(IsolatedUniverseDefinition.end()),
) {
    init {
        require(id.matches(Regex("[a-z0-9_.-]+:[a-z0-9_./-]+"))) { "Universe id must be namespaced." }
        require(galaxies.isNotEmpty()) { "A Universe needs at least one galaxy." }
        require(galaxies.map(Galaxy::id).distinct().size == galaxies.size) { "Galaxy ids must be unique." }
        require(isolatedUniverses.map(IsolatedUniverseDefinition::id).distinct().size == isolatedUniverses.size) { "Isolated universe ids must be unique." }

        val planets = galaxies.flatMap { galaxy -> galaxy.groups.flatMap { it.allPlanets() } }
        require(planets.map(Planet::id).distinct().size == planets.size) { "Planet ids must be unique per Universe." }
    }

    fun connectionGraph(): DimensionConnectionGraph = DimensionConnectionGraph(
        galaxies.flatMap { galaxy -> galaxy.groups }
            .flatMap { group -> group.connectionsTo(universeDimension) },
    )

    /**
     * One directed, horizontal portal specification for each physical stack boundary.
     * The seamless sky-to-Universe transition deliberately has no local portal surface.
     */
    fun localEuclideanPortalGraph(): LocalEuclideanPortalGraph = LocalEuclideanPortalGraph(
        connectionGraph().allRoutes()
            .filter { it.kind == DimensionConnectionKind.RADIAL_BOUNDARY }
            .map { route ->
                LocalEuclideanPortal(
                    id = route.id,
                    source = LocalPortalEndpoint(route.source, route.sourceBoundaryFace),
                    target = LocalPortalEndpoint(route.target, route.targetBoundaryFace),
                    boundary = route.boundarySurface,
                    scale = route.scale,
                )
            },
    )
}

/** A dimension outside every planet stack. The End loops vertically inside its own universe. */
data class IsolatedUniverseDefinition(
    val id: String,
    val dimension: DimensionId,
    val verticalLoop: VerticalLoop = VerticalLoop.BOTH_DIRECTIONS,
) {
    init {
        require(id.matches(Regex("[a-z0-9_.-]+"))) { "Invalid isolated universe id." }
    }

    companion object {
        fun end() = IsolatedUniverseDefinition("end", DimensionId("minecraft:the_end"))
    }
}

enum class VerticalLoop { NONE, BOTH_DIRECTIONS }

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

    internal fun connectionsTo(universeDimension: DimensionId): List<DimensionConnection> =
        buildList {
            star?.let { addAll(it.connectionsTo(universeDimension)) }
            allPlanets().forEach { addAll(it.connectionsTo(universeDimension)) }
        }

    fun allPlanets(): List<Planet> = planets.flatMap(Planet::includingMoons)
}

data class Star(
    val id: String,
    val stacks: List<PlanetDimensionStack>,
) {
    init {
        requireNodeId(id, "Star")
        require(stacks.isNotEmpty()) { "A star needs at least one vertical dimension stack." }
        require(stacks.map(PlanetDimensionStack::id).distinct().size == stacks.size) { "Stack ids must be unique per star." }
    }

    internal fun connectionsTo(universeDimension: DimensionId): List<DimensionConnection> =
        stacks.flatMap { stack -> stack.connectionsTo(id, universeDimension) }
}

data class Planet(
    val id: String,
    val planetCoreSize: Double,
    val stacks: List<PlanetDimensionStack>,
    val moons: List<Planet> = emptyList(),
) {
    init {
        requireNodeId(id, "Planet")
        require(planetCoreSize > 0.0 && planetCoreSize.isFinite()) { "Planet-core size must be finite and positive." }
        require(stacks.isNotEmpty()) { "A planet needs at least one dimension stack." }
        require(stacks.map(PlanetDimensionStack::id).distinct().size == stacks.size) { "Stack ids must be unique per planet." }
        require(moons.map(Planet::id).distinct().size == moons.size) { "Moon ids must be unique per planet." }
    }

    internal fun connectionsTo(universeDimension: DimensionId): List<DimensionConnection> = stacks.flatMap { stack ->
        stack.connectionsTo(id, universeDimension)
    }

    fun includingMoons(): List<Planet> = listOf(this) + moons.flatMap(Planet::includingMoons)
}

private fun PlanetDimensionStack.connectionsTo(ownerId: String, universeDimension: DimensionId): List<DimensionConnection> {
    val radial = layersInnerToOuter.windowed(2).map { (inner, outer) ->
        DimensionConnection(
            id = "$ownerId/$id/${inner.id}-to-${outer.id}",
            source = inner.dimension,
            target = outer.dimension,
            scale = requireNotNull(inner.toOuterScale),
            boundarySurface = requireNotNull(inner.outerBoundarySurface),
        )
    }
    return radial + DimensionConnection(
        id = "$ownerId/$id/${layersInnerToOuter.last().id}-to-universe",
        source = layersInnerToOuter.last().dimension,
        target = universeDimension,
        scale = outerToUniverseScale,
        boundarySurface = BoundarySurface.AIR,
        kind = DimensionConnectionKind.UNIVERSE_TRANSITION,
    )
}

data class PlanetDimensionStack(
    val id: String,
    val layersInnerToOuter: List<PlanetDimensionLayer>,
    val outerToUniverseScale: DimensionScale = DimensionScale.ONE,
) {
    init {
        requireNodeId(id, "Stack")
        require(layersInnerToOuter.size >= 2) { "A stack needs a planet core and a surface layer." }
        require(layersInnerToOuter.first().role == PlanetDimensionRole.PLANET_CORE) { "The innermost layer must be the planet core." }
        require(layersInnerToOuter.last().role == PlanetDimensionRole.SKY || layersInnerToOuter.last().role == PlanetDimensionRole.SURFACE) {
            "The outermost layer must be the surface or an optional sky layer."
        }
        require(layersInnerToOuter.map(PlanetDimensionLayer::id).distinct().size == layersInnerToOuter.size) {
            "Layer ids must be unique per stack."
        }
        layersInnerToOuter.dropLast(1).forEach { layer ->
            requireNotNull(layer.toOuterScale) { "Every inner boundary needs an explicit dimension scale." }
        }
        require(layersInnerToOuter.last().toOuterScale == null) { "The outermost layer's next boundary is the Universe transition." }
        layersInnerToOuter.windowed(2).forEach { (inner, outer) ->
            require(inner.outerBoundarySurface == outer.innerBoundarySurface) {
                "Adjacent dimensions must connect bedrock-to-bedrock or air-to-air."
            }
        }
    }
}

enum class PlanetDimensionRole { PLANET_CORE, INNER, SURFACE, SKY, CUSTOM }

data class PlanetDimensionLayer(
    val id: String,
    val role: PlanetDimensionRole,
    val dimension: DimensionId,
    val toOuterScale: DimensionScale? = null,
    val innerBoundarySurface: BoundarySurface? = defaultInnerSurface(role),
    val outerBoundarySurface: BoundarySurface? = defaultOuterSurface(role),
) {
    init {
        requireNodeId(id, "Dimension layer")
        if (role == PlanetDimensionRole.PLANET_CORE) require(innerBoundarySurface == null)
        if (role == PlanetDimensionRole.SKY) require(outerBoundarySurface == BoundarySurface.AIR)
        if (role != PlanetDimensionRole.PLANET_CORE) requireNotNull(innerBoundarySurface)
        requireNotNull(outerBoundarySurface)
    }
}

private fun defaultInnerSurface(role: PlanetDimensionRole): BoundarySurface? = when (role) {
    PlanetDimensionRole.PLANET_CORE -> null
    PlanetDimensionRole.SKY -> BoundarySurface.AIR
    else -> BoundarySurface.BEDROCK
}

private fun defaultOuterSurface(role: PlanetDimensionRole): BoundarySurface? = when (role) {
    PlanetDimensionRole.SKY -> BoundarySurface.AIR
    PlanetDimensionRole.PLANET_CORE -> BoundarySurface.BEDROCK
    else -> BoundarySurface.AIR
}

private fun requireNodeId(id: String, type: String) {
    require(id.matches(Regex("[a-z0-9_.-]+"))) { "Invalid $type id: $id" }
}
