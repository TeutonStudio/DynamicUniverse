package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface

/** Client-side draft used while the Create World screen is open. */
data class UniverseWorldCreationDraft(
    val universe: EditableUniverse = EditableUniverse.default(),
) {
    fun validation(): UniverseDraftValidation = UniverseDraftValidation(
        universe.galaxies.flatMapIndexed { galaxyIndex, galaxy ->
            galaxy.entries.flatMapIndexed { entryIndex, entry ->
                (entry as? EditableSolarSystem)?.planets.orEmpty().flatMapIndexed { planetIndex, planet ->
                    planet.validationErrors(PlanetAddress(galaxyIndex, entryIndex, planetIndex))
                }
            }
        },
    )
}

data class UniverseDraftValidation(val invalidPlanets: List<PlanetDraftValidationError>) {
    val isValid: Boolean get() = invalidPlanets.isEmpty()
}

data class PlanetDraftValidationError(
    val address: PlanetAddress,
    val validation: EditableStackValidation,
)

data class EditableUniverse(val galaxies: List<EditableGalaxy>) {
    init { require(galaxies.isNotEmpty()) { "Universe needs at least one galaxy" } }

    companion object {
        fun default() = EditableUniverse(listOf(
            EditableGalaxy("Lokale Gruppe", listOf(
                EditableSolarSystem("Sol", EditableStar("Sol"), listOf(EditablePlanet.default())),
                EditableCloud("Orion-Wolke"),
            )),
        ))
    }
}

data class EditableGalaxy(val name: String, val entries: List<EditableGalaxyEntry>)
sealed interface EditableGalaxyEntry { val name: String }
data class EditableSolarSystem(
    override val name: String,
    val star: EditableStar,
    val planets: List<EditablePlanet>,
    /** Distances are deliberately separate from day length; orbital simulation is future work. */
    val firstPlanetDistanceMilBlocks: Long = DEFAULT_ORBIT_GAP_MILBLOCKS,
    val planetToPlanetDistancesMilBlocks: List<Long> = List((planets.size - 1).coerceAtLeast(0)) { DEFAULT_ORBIT_GAP_MILBLOCKS },
) : EditableGalaxyEntry {
    init {
        require(planets.isNotEmpty()) { "A solar system needs at least one planet" }
        require(firstPlanetDistanceMilBlocks > 0) { "The first orbit distance must be positive." }
        require(planetToPlanetDistancesMilBlocks.size == (planets.size - 1).coerceAtLeast(0)) { "Every pair of planets needs one orbit gap." }
        require(planetToPlanetDistancesMilBlocks.all { it > 0 }) { "Orbit distances must be positive." }
    }

    fun withOrbitDistance(index: Int, distanceMilBlocks: Long): EditableSolarSystem = when (index) {
        0 -> copy(firstPlanetDistanceMilBlocks = distanceMilBlocks.coerceAtLeast(1))
        in 1..planetToPlanetDistancesMilBlocks.size -> copy(
            planetToPlanetDistancesMilBlocks = planetToPlanetDistancesMilBlocks.mapIndexed { gapIndex, gap ->
                if (gapIndex == index - 1) distanceMilBlocks.coerceAtLeast(1) else gap
            },
        )
        else -> this
    }

    companion object { const val DEFAULT_ORBIT_GAP_MILBLOCKS = 1L }
}
data class EditableCloud(override val name: String) : EditableGalaxyEntry
data class EditableStar(val name: String, val dimensionStack: EditableDimensionStack = EditableDimensionStack.starDefault()) {
    val dimensions: List<EditableDimension> get() = dimensionStack.layers
}

enum class CelestialBodyKind(val radialScale: Int) {
    PLANET(8),
    DWARF_PLANET(4),
    MOON(2),
}

data class PlanetProfileProvenance(
    val templateId: String,
    val templateName: String,
    val isLocal: Boolean = false,
    val localProfileName: String? = null,
) {
    val visibleName: String get() = localProfileName ?: templateName
}

/** A body owns one local stack. Templates are copied on first edit, never live-inherited. */
data class EditablePlanet(
    val name: String,
    val dimensionStack: EditableDimensionStack,
    val bodyKind: CelestialBodyKind,
    val transitionFactor: Int,
    val coreSize: Int,
    val profile: PlanetProfileProvenance,
    val moons: List<EditablePlanet> = emptyList(),
    val nebulaRings: List<EditableNebulaRing> = emptyList(),
    /** Used by moons; a direct planet's distance is owned by its solar system. */
    val parentOrbitDistanceMilBlocks: Long = DEFAULT_PARENT_ORBIT_DISTANCE_MILBLOCKS,
) {
    init {
        require(coreSize in MIN_CORE_SIZE..MAX_CORE_SIZE)
        require(parentOrbitDistanceMilBlocks > 0)
    }

    val dimensions: List<EditableDimension> get() = dimensionStack.layers
    val dimensionValidation: EditableStackValidation get() = dimensionStack.validation()
    val radialScale: Int get() = transitionFactor
    val profileLabel: String get() = "$name · ${profile.visibleName}"

    fun withBodyKind(kind: CelestialBodyKind) = if (kind == bodyKind) this else copy(bodyKind = kind, transitionFactor = kind.radialScale).asLocalProfile()
    fun withTransitionFactor(factor: Int) = copy(transitionFactor = factor.coerceIn(MIN_TRANSITION_FACTOR, MAX_TRANSITION_FACTOR)).asLocalProfile()
    fun withCoreSize(size: Int): EditablePlanet {
        val normalized = size.coerceIn(MIN_CORE_SIZE, MAX_CORE_SIZE)
        return if (normalized == coreSize) this else copy(coreSize = normalized).asLocalProfile()
    }
    fun addDimension() = withStack(dimensionStack.addDimension())
    fun moveDimension(index: Int, delta: Int) = withStack(dimensionStack.move(index, delta))
    fun removeDimension(index: Int) = withStack(dimensionStack.remove(index))
    fun replaceDimension(index: Int, descriptorId: String) = withStack(dimensionStack.replace(index, descriptorId))
    fun insertBalancingDimension(lowerIndex: Int, descriptorId: String) =
        withStack(dimensionStack.insert(lowerIndex + 1, descriptorId))

    fun withMoon(index: Int, transform: (EditablePlanet) -> EditablePlanet): EditablePlanet =
        copy(moons = moons.mapIndexed { moonIndex, moon -> if (moonIndex == index) transform(moon) else moon })
    fun withParentOrbitDistance(distanceMilBlocks: Long) = copy(parentOrbitDistanceMilBlocks = distanceMilBlocks.coerceAtLeast(1)).asLocalProfile()

    private fun withStack(stack: EditableDimensionStack): EditablePlanet =
        if (stack == dimensionStack) this else copy(dimensionStack = stack).asLocalProfile()

    private fun asLocalProfile(): EditablePlanet = if (profile.isLocal) this else copy(
        profile = profile.copy(isLocal = true, localProfileName = "${profile.templateName} Custom"),
    )

    companion object {
        const val MIN_CORE_SIZE = 8
        const val MAX_CORE_SIZE = 2048
        const val CORE_SIZE_STEP = 8
        const val MIN_TRANSITION_FACTOR = 1
        const val MAX_TRANSITION_FACTOR = 64
        const val DEFAULT_PARENT_ORBIT_DISTANCE_MILBLOCKS = 1L

        fun default() = EditablePlanet(
            name = "Terra",
            dimensionStack = EditableDimensionStack.planetDefault(),
            bodyKind = CelestialBodyKind.PLANET,
            transitionFactor = CelestialBodyKind.PLANET.radialScale,
            coreSize = 32,
            profile = PlanetProfileProvenance("dynamicuniverse:earth", "Erde"),
        )
    }
}

/** A persisted placeholder for a ring; its geometry and rendering are intentionally not defined in alpha0. */
data class EditableNebulaRing(val name: String)

data class PlanetAddress(
    val galaxyIndex: Int,
    val entryIndex: Int,
    val planetIndex: Int,
    val moonIndexes: List<Int> = emptyList(),
) {
    fun moon(index: Int): PlanetAddress = copy(moonIndexes = moonIndexes + index)
}

fun UniverseWorldCreationDraft.planetAt(address: PlanetAddress): EditablePlanet {
    var planet = (universe.galaxies[address.galaxyIndex].entries[address.entryIndex] as EditableSolarSystem).planets[address.planetIndex]
    address.moonIndexes.forEach { planet = planet.moons[it] }
    return planet
}

fun UniverseWorldCreationDraft.updatePlanet(address: PlanetAddress, transform: (EditablePlanet) -> EditablePlanet): UniverseWorldCreationDraft {
    fun updateNested(planet: EditablePlanet, path: List<Int>): EditablePlanet =
        if (path.isEmpty()) transform(planet) else planet.withMoon(path.first()) { updateNested(it, path.drop(1)) }
    val galaxy = universe.galaxies[address.galaxyIndex]
    val system = galaxy.entries[address.entryIndex] as EditableSolarSystem
    val updatedSystem = system.copy(planets = system.planets.mapIndexed { index, planet ->
        if (index == address.planetIndex) updateNested(planet, address.moonIndexes) else planet
    })
    val updatedGalaxy = galaxy.copy(entries = galaxy.entries.mapIndexed { index, entry -> if (index == address.entryIndex) updatedSystem else entry })
    return copy(universe = universe.copy(galaxies = universe.galaxies.mapIndexed { index, candidate -> if (index == address.galaxyIndex) updatedGalaxy else candidate }))
}

private fun EditablePlanet.validationErrors(address: PlanetAddress): List<PlanetDraftValidationError> = buildList {
    if (!dimensionValidation.isValid) add(PlanetDraftValidationError(address, dimensionValidation))
    moons.forEachIndexed { index, moon -> addAll(moon.validationErrors(address.moon(index))) }
}

/** An ordered radial stack. It uses template descriptors, never hand-written boundary toggles. */
data class EditableDimensionStack(val layers: List<EditableDimension>) {
    init {
        require(layers.size >= 2) { "A planet stack needs at least a core and a surface." }
        require(layers.first().role == EditableDimensionRole.CORE) { "The first layer must be the core." }
        require(layers.last().role in setOf(EditableDimensionRole.SURFACE, EditableDimensionRole.SKY)) {
            "The final layer must be surface-facing or sky-facing."
        }
    }

    companion object {
        fun planetDefault() = EditableDimensionStack(listOf(
            EditableDimension("core", PlanetDimensionCatalog.CORE_ID),
            EditableDimension("nether", PlanetDimensionCatalog.NETHER_ID),
            EditableDimension("underground", PlanetDimensionCatalog.UNDERGROUND_ID),
            EditableDimension("overworld", PlanetDimensionCatalog.OVERWORLD_ID),
            EditableDimension("aether", PlanetDimensionCatalog.AETHER_ID),
        ))

        fun starDefault() = EditableDimensionStack(listOf(
            EditableDimension("core", PlanetDimensionCatalog.CORE_ID),
            EditableDimension("radiative_zone", PlanetDimensionCatalog.OVERWORLD_ID),
            EditableDimension("corona", PlanetDimensionCatalog.SKY_ID),
        ))
    }

    fun validation(catalog: PlanetDimensionCatalog = PlanetDimensionCatalogs.current): EditableStackValidation {
        val mismatches = layers.windowed(2).mapIndexedNotNull { index, (inner, outer) ->
            if (inner.outerBoundarySurface == outer.innerBoundarySurface) null else EditableBoundaryMismatch(index, inner, outer)
        }
        val unresolved = layers.filter { catalog.require(it.descriptorId).blocksWorldCreation }
        val surfaceCount = layers.count { it.role == EditableDimensionRole.SURFACE }
        val shapeErrors = buildList {
            if (surfaceCount != 1) add("A planet stack needs exactly one BEDROCK-to-AIR surface.")
            if (layers.drop(1).dropLast(1).any { it.role == EditableDimensionRole.CORE }) add("The core may only be the lowest layer.")
            val surfaceIndex = layers.indexOfFirst { it.role == EditableDimensionRole.SURFACE }
            if (layers.drop(surfaceIndex + 1).any { it.role != EditableDimensionRole.SKY }) add("Only an optional sky layer may follow the surface.")
            if (layers.count { it.role == EditableDimensionRole.SKY } > 1) add("A stack may have at most one sky layer.")
        }
        return EditableStackValidation(mismatches, unresolved, shapeErrors)
    }

    fun balancingCandidates(lowerIndex: Int, catalog: PlanetDimensionCatalog = PlanetDimensionCatalogs.current): List<RegisteredDimensionDescriptor> {
        if (lowerIndex !in 0 until layers.lastIndex) return emptyList()
        return catalog.insertionCandidates(layers[lowerIndex], layers[lowerIndex + 1])
    }

    fun addDimension(catalog: PlanetDimensionCatalog = PlanetDimensionCatalogs.current): EditableDimensionStack {
        val lowerIndex = layers.lastIndex - 1
        val descriptor = balancingCandidates(lowerIndex, catalog).firstOrNull() ?: return this
        return insert(layers.lastIndex, descriptor.id)
    }

    fun insert(index: Int, descriptorId: String): EditableDimensionStack {
        if (index !in 1..layers.lastIndex) return this
        val descriptor = PlanetDimensionCatalogs.current.require(descriptorId)
        if (!descriptor.isSelectable || descriptor.kind == RegisteredDimensionKind.CORE) return this
        val nextId = uniqueLayerId(descriptorId.substringAfter(':').replace('/', '_'))
        return copy(layers = layers.toMutableList().also { it.add(index, EditableDimension(nextId, descriptorId)) })
    }

    fun replace(index: Int, descriptorId: String): EditableDimensionStack {
        if (index !in layers.indices) return this
        val descriptor = PlanetDimensionCatalogs.current.require(descriptorId)
        if (!descriptor.isSelectable) return this
        val candidates = PlanetDimensionCatalogs.current.selectableFor(index, layers.size)
        if (descriptor !in candidates) return this
        return copy(layers = layers.mapIndexed { candidate, layer -> if (candidate == index) layer.copy(descriptorId = descriptorId) else layer })
    }

    fun move(index: Int, delta: Int): EditableDimensionStack {
        if (index !in 1 until layers.lastIndex || layers[index].role != EditableDimensionRole.SHELL) return this
        val target = (index + delta).coerceIn(1, layers.lastIndex - 1)
        if (target == index || layers[target].role != EditableDimensionRole.SHELL) return this
        val mutable = layers.toMutableList()
        mutable.add(target, mutable.removeAt(index))
        return copy(layers = mutable)
    }

    fun remove(index: Int): EditableDimensionStack {
        if (index !in 1..layers.lastIndex) return this
        if (layers[index].role == EditableDimensionRole.SURFACE) return this
        if (index == layers.lastIndex && layers[index].role != EditableDimensionRole.SKY) return this
        return copy(layers = layers.filterIndexed { candidate, _ -> candidate != index })
    }

    private fun uniqueLayerId(prefix: String): String = generateSequence(1) { it + 1 }
        .map { "${prefix}_$it" }
        .first { candidate -> layers.none { it.id == candidate } }
}

data class EditableStackValidation(
    val mismatches: List<EditableBoundaryMismatch>,
    val unresolvedDimensions: List<EditableDimension>,
    val shapeErrors: List<String>,
) {
    val isValid: Boolean get() = mismatches.isEmpty() && unresolvedDimensions.isEmpty() && shapeErrors.isEmpty()
}

data class EditableBoundaryMismatch(val lowerIndex: Int, val lower: EditableDimension, val upper: EditableDimension) {
    val isForbiddenAirToBedrock: Boolean get() = lower.outerBoundarySurface == BoundarySurface.AIR && upper.innerBoundarySurface == BoundarySurface.BEDROCK
}

data class EditableDimension(val id: String, val descriptorId: String) {
    private val descriptor: RegisteredDimensionDescriptor get() = PlanetDimensionCatalogs.current.require(descriptorId)
    val displayName: String get() = descriptor.displayName
    val role: EditableDimensionRole get() = when (descriptor.kind) {
        RegisteredDimensionKind.CORE -> EditableDimensionRole.CORE
        RegisteredDimensionKind.SHELL -> EditableDimensionRole.SHELL
        RegisteredDimensionKind.SURFACE -> EditableDimensionRole.SURFACE
        RegisteredDimensionKind.SKY -> EditableDimensionRole.SKY
        RegisteredDimensionKind.ISOLATED -> error("Isolated dimensions cannot enter a planet stack.")
    }
    val innerBoundarySurface: BoundarySurface? get() = descriptor.inspection.lower
    val outerBoundarySurface: BoundarySurface? get() = descriptor.inspection.upper
    val status: DimensionCatalogStatus get() = descriptor.catalogStatus
}

enum class EditableDimensionRole { CORE, SHELL, SURFACE, SKY }
