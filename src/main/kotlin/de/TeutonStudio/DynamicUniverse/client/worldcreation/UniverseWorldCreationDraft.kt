package de.TeutonStudio.DynamicUniverse.client.worldcreation

/**
 * Client-side draft used while the Create World screen is open.
 *
 * It is deliberately not written to a world yet. A later world-creation bridge can
 * consume this data after the server-side generator and persistence format exist.
 */
data class UniverseWorldCreationDraft(
    val universe: EditableUniverse = EditableUniverse.default(),
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

    fun withIntermediateDimensionCount(count: Int) = copy(
        dimensionStack = EditableDimensionStack.planetDefault(count.coerceIn(MIN_INTERMEDIATE_DIMENSIONS, MAX_INTERMEDIATE_DIMENSIONS)),
    )

    fun withTransitionFactor(factor: Int) = copy(
        dimensionTransitionFactor = factor.coerceIn(MIN_TRANSITION_FACTOR, MAX_TRANSITION_FACTOR),
    )

    fun withCoreSize(size: Int) = copy(coreSize = size.coerceIn(MIN_CORE_SIZE, MAX_CORE_SIZE))

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
            listOf(EditableDimension("core", "Planetenkern", EditableDimensionRole.CORE)) +
                (1..intermediateCount).map { EditableDimension("inner_$it", "Innere Dimension $it", EditableDimensionRole.INNER) } +
                EditableDimension("surface", "Oberfläche", EditableDimensionRole.SKY),
        )

        fun starDefault() = EditableDimensionStack(
            listOf(
                EditableDimension("core", "Sternkern", EditableDimensionRole.CORE),
                EditableDimension("radiative_zone", "Strahlungszone", EditableDimensionRole.INNER),
                EditableDimension("corona", "Korona", EditableDimensionRole.SKY),
            ),
        )
    }
}

data class EditableDimension(val id: String, val displayName: String, val role: EditableDimensionRole)

enum class EditableDimensionRole { CORE, INNER, SKY }
