package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface

/** Client-side draft used while the Create World screen is open. */
data class UniverseWorldCreationDraft(
    val universe: EditableUniverse = EditableUniverse.default(),
) {
    fun validation(): UniverseDraftValidation = UniverseDraftValidation(
        universe.galaxies.flatMapIndexed { galaxyIndex, galaxy ->
            galaxy.entries.flatMapIndexed { entryIndex, entry ->
                (entry as? EditableSolarSystem)?.planets.orEmpty().mapIndexedNotNull { planetIndex, planet ->
                    if (planet.dimensionValidation.isValid) null else PlanetDraftValidationError(galaxyIndex, entryIndex, planetIndex, planet.dimensionValidation)
                }
            }
        },
    )
}

data class UniverseDraftValidation(val invalidPlanets: List<PlanetDraftValidationError>) {
    val isValid: Boolean get() = invalidPlanets.isEmpty()
}

data class PlanetDraftValidationError(
    val galaxyIndex: Int,
    val entryIndex: Int,
    val planetIndex: Int,
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
data class EditableSolarSystem(override val name: String, val star: EditableStar, val planets: List<EditablePlanet>) : EditableGalaxyEntry {
    init { require(planets.isNotEmpty()) { "A solar system needs at least one planet" } }
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
    val coreSize: Int,
    val profile: PlanetProfileProvenance,
) {
    init { require(coreSize in MIN_CORE_SIZE..MAX_CORE_SIZE) }

    val dimensions: List<EditableDimension> get() = dimensionStack.layers
    val dimensionValidation: EditableStackValidation get() = dimensionStack.validation()
    val radialScale: Int get() = bodyKind.radialScale
    val profileLabel: String get() = "$name · ${profile.visibleName}"

    fun withBodyKind(kind: CelestialBodyKind) = if (kind == bodyKind) this else copy(bodyKind = kind).asLocalProfile()
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

    private fun withStack(stack: EditableDimensionStack): EditablePlanet =
        if (stack == dimensionStack) this else copy(dimensionStack = stack).asLocalProfile()

    private fun asLocalProfile(): EditablePlanet = if (profile.isLocal) this else copy(
        profile = profile.copy(isLocal = true, localProfileName = "${profile.templateName} Custom"),
    )

    companion object {
        const val MIN_CORE_SIZE = 8
        const val MAX_CORE_SIZE = 2048
        const val CORE_SIZE_STEP = 8

        fun default() = EditablePlanet(
            name = "Terra",
            dimensionStack = EditableDimensionStack.planetDefault(),
            bodyKind = CelestialBodyKind.PLANET,
            coreSize = 32,
            profile = PlanetProfileProvenance("dynamicuniverse:earth", "Erde"),
        )
    }
}

/** An ordered radial stack. It uses template descriptors, never hand-written boundary toggles. */
data class EditableDimensionStack(val layers: List<EditableDimension>) {
    init {
        require(layers.size >= 3) { "A planet stack needs at least a core, surface, and sky layer." }
        require(layers.first().role == EditableDimensionRole.CORE) { "The first layer must be the core." }
        require(layers.last().role == EditableDimensionRole.SKY) { "The final layer must be sky-facing." }
    }

    companion object {
        fun planetDefault() = EditableDimensionStack(listOf(
            EditableDimension("core", PlanetDimensionCatalog.CORE_ID),
            EditableDimension("deep_nether", PlanetDimensionCatalog.DEEP_NETHER_ID),
            EditableDimension("nether", PlanetDimensionCatalog.NETHER_ID),
            EditableDimension("surface", PlanetDimensionCatalog.OVERWORLD_ID),
            EditableDimension("sky", PlanetDimensionCatalog.SKY_ID),
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
        if (index !in 1 until layers.lastIndex) return this
        val target = (index + delta).coerceIn(1, layers.lastIndex - 1)
        if (target == index) return this
        val mutable = layers.toMutableList()
        mutable.add(target, mutable.removeAt(index))
        return copy(layers = mutable)
    }

    fun remove(index: Int): EditableDimensionStack {
        if (index !in 1 until layers.lastIndex) return this
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
