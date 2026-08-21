package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface

/**
 * Client-side draft used while the Create World screen is open.
 *
 * It is deliberately not written to a world yet. A later world-creation bridge can
 * consume this data after the server-side generator and persistence format exist.
 */
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

data class EditableUniverse(
    val galaxies: List<EditableGalaxy>,
) {
    init {
        require(galaxies.isNotEmpty()) { "Universe needs at least one galaxy" }
    }

    companion object {
        fun default() = EditableUniverse(
            galaxies = listOf(
                EditableGalaxy(
                    name = "Lokale Gruppe",
                    entries = listOf(
                        EditableSolarSystem(
                            name = "Sol",
                            star = EditableStar("Sol"),
                            planets = listOf(EditablePlanet.default()),
                        ),
                        EditableCloud("Orion-Wolke"),
                    ),
                ),
            ),
        )
    }
}

data class EditableGalaxy(
    val name: String,
    val entries: List<EditableGalaxyEntry>,
)

sealed interface EditableGalaxyEntry {
    val name: String
}

data class EditableSolarSystem(
    override val name: String,
    val star: EditableStar,
    val planets: List<EditablePlanet>,
) : EditableGalaxyEntry {
    init {
        require(planets.isNotEmpty()) { "A solar system needs at least one planet" }
    }
}

data class EditableCloud(
    override val name: String,
) : EditableGalaxyEntry

data class EditableStar(
    val name: String,
    val dimensionStack: EditableDimensionStack = EditableDimensionStack.starDefault(),
) {
    val dimensions: List<EditableDimension> get() = dimensionStack.layers
}

data class EditablePlanet(
    val name: String,
    val dimensionStack: EditableDimensionStack,
    val dimensionTransitionFactor: Int,
    val coreSize: Int,
) {
    init {
        require(dimensionTransitionFactor in MIN_TRANSITION_FACTOR..MAX_TRANSITION_FACTOR)
        require(coreSize in MIN_CORE_SIZE..MAX_CORE_SIZE)
    }

    val dimensions: List<EditableDimension> get() = dimensionStack.layers
    val intermediateDimensionCount: Int get() = dimensionStack.layers.count { it.role == EditableDimensionRole.INNER }
    val dimensionValidation: EditableStackValidation get() = dimensionStack.validation()

    fun withIntermediateDimensionCount(count: Int) = copy(
        dimensionStack = EditableDimensionStack.planetDefault(count.coerceIn(MIN_INTERMEDIATE_DIMENSIONS, MAX_INTERMEDIATE_DIMENSIONS)),
    )

    fun withTransitionFactor(factor: Int) = copy(
        dimensionTransitionFactor = factor.coerceIn(MIN_TRANSITION_FACTOR, MAX_TRANSITION_FACTOR),
    )

    fun withCoreSize(size: Int) = copy(coreSize = size.coerceIn(MIN_CORE_SIZE, MAX_CORE_SIZE))

    fun addDimension() = copy(dimensionStack = dimensionStack.addIntermediate())

    fun moveDimension(index: Int, delta: Int) = copy(dimensionStack = dimensionStack.moveIntermediate(index, delta))

    fun removeDimension(index: Int) = copy(dimensionStack = dimensionStack.removeIntermediate(index))

    companion object {
        const val MIN_INTERMEDIATE_DIMENSIONS = 0
        const val MAX_INTERMEDIATE_DIMENSIONS = 8
        const val MIN_TRANSITION_FACTOR = 4
        const val MAX_TRANSITION_FACTOR = 64
        const val MIN_CORE_SIZE = 8
        const val MAX_CORE_SIZE = 128
        const val CORE_SIZE_STEP = 8

        fun default() = EditablePlanet(
            name = "Terra",
            dimensionStack = EditableDimensionStack.planetDefault(2),
            dimensionTransitionFactor = 4,
            coreSize = 32,
        )
    }
}

/** A vertical, ordered stack. Every celestial body owns one; it is never inferred from a name. */
data class EditableDimensionStack(val layers: List<EditableDimension>) {
    init {
        require(layers.size >= 2) { "A dimension stack needs at least a core and an outer layer." }
        require(layers.first().role == EditableDimensionRole.CORE) { "The first layer must be the core." }
        require(layers.last().role == EditableDimensionRole.SKY) { "The last layer must be the outer space-facing layer." }
    }

    companion object {
        fun planetDefault(intermediateCount: Int) = EditableDimensionStack(
            listOf(EditableDimension.core()) +
                (1..intermediateCount).map { index ->
                    if (index == 1) EditableDimension("inner_$index", "Innere Dimension $index", EditableDimensionRole.INNER, BoundarySurface.BEDROCK, BoundarySurface.AIR)
                    else EditableDimension("inner_$index", "Innere Dimension $index", EditableDimensionRole.INNER, BoundarySurface.AIR, BoundarySurface.AIR)
                } +
                EditableDimension.sky(),
        )

        fun starDefault() = EditableDimensionStack(
            listOf(
                EditableDimension.core("Sternkern"),
                EditableDimension("radiative_zone", "Strahlungszone", EditableDimensionRole.INNER, BoundarySurface.BEDROCK, BoundarySurface.AIR),
                EditableDimension.sky("Korona"),
            ),
        )
    }

    fun validation(): EditableStackValidation {
        val mismatches = layers.windowed(2).mapIndexedNotNull { index, (inner, outer) ->
            if (inner.outerBoundarySurface == outer.innerBoundarySurface) null
            else EditableBoundaryMismatch(index, inner, outer)
        }
        return EditableStackValidation(mismatches)
    }

    fun addIntermediate(): EditableDimensionStack {
        val intermediateCount = layers.count { it.role == EditableDimensionRole.INNER }
        if (intermediateCount >= EditablePlanet.MAX_INTERMEDIATE_DIMENSIONS) return this
        val next = generateSequence(1) { it + 1 }
            .first { candidate -> layers.none { it.id == "inner_$candidate" } }
        val dimension = EditableDimension(
            id = "inner_$next",
            displayName = "Innere Dimension $next",
            role = EditableDimensionRole.INNER,
            innerBoundarySurface = requireNotNull(layers[layers.lastIndex - 1].outerBoundarySurface),
            outerBoundarySurface = BoundarySurface.AIR,
        )
        return copy(layers = layers.dropLast(1) + dimension + layers.last())
    }

    fun moveIntermediate(index: Int, delta: Int): EditableDimensionStack {
        if (index !in layers.indices || layers[index].role != EditableDimensionRole.INNER) return this
        val target = (index + delta).coerceIn(1, layers.lastIndex - 1)
        if (target == index) return this
        val mutable = layers.toMutableList()
        val dimension = mutable.removeAt(index)
        mutable.add(target, dimension)
        return copy(layers = mutable)
    }

    fun removeIntermediate(index: Int): EditableDimensionStack {
        if (index !in layers.indices || layers[index].role != EditableDimensionRole.INNER) return this
        return copy(layers = layers.filterIndexed { candidate, _ -> candidate != index })
    }
}

data class EditableStackValidation(val mismatches: List<EditableBoundaryMismatch>) {
    val isValid: Boolean get() = mismatches.isEmpty()
}

data class EditableBoundaryMismatch(
    val lowerIndex: Int,
    val lower: EditableDimension,
    val upper: EditableDimension,
)

data class EditableDimension(
    val id: String,
    val displayName: String,
    val role: EditableDimensionRole,
    val innerBoundarySurface: BoundarySurface?,
    val outerBoundarySurface: BoundarySurface?,
) {
    init {
        if (role == EditableDimensionRole.CORE) require(innerBoundarySurface == null)
        if (role == EditableDimensionRole.SKY) require(outerBoundarySurface == null)
    }

    companion object {
        fun core(displayName: String = "Planetenkern") = EditableDimension("core", displayName, EditableDimensionRole.CORE, null, BoundarySurface.BEDROCK)
        fun sky(displayName: String = "Oberfläche") = EditableDimension("surface", displayName, EditableDimensionRole.SKY, BoundarySurface.AIR, null)
    }
}

enum class EditableDimensionRole { CORE, INNER, SKY }
