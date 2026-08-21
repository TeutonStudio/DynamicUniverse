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
    val galaxyName: String,
    val solarSystemName: String,
    val starName: String,
    val planet: EditablePlanet,
) {
    companion object {
        fun default() = EditableUniverse(
            galaxyName = "Lokale Gruppe",
            solarSystemName = "Sol",
            starName = "Sol",
            planet = EditablePlanet.default(),
        )
    }
}

data class EditablePlanet(
    val name: String,
    val intermediateDimensionCount: Int,
    val dimensionTransitionFactor: Int,
    val coreSize: Int,
) {
    init {
        require(intermediateDimensionCount in MIN_INTERMEDIATE_DIMENSIONS..MAX_INTERMEDIATE_DIMENSIONS)
        require(dimensionTransitionFactor in MIN_TRANSITION_FACTOR..MAX_TRANSITION_FACTOR)
        require(dimensionTransitionFactor.countOneBits() == 1) {
            "dimensionTransitionFactor must be a power of two"
        }
        require(coreSize in MIN_CORE_SIZE..MAX_CORE_SIZE)
    }

    fun withIntermediateDimensionCount(count: Int) = copy(
        intermediateDimensionCount = count.coerceIn(MIN_INTERMEDIATE_DIMENSIONS, MAX_INTERMEDIATE_DIMENSIONS),
    )

    fun withTransitionFactor(factor: Int) = copy(
        dimensionTransitionFactor = factor.coerceIn(MIN_TRANSITION_FACTOR, MAX_TRANSITION_FACTOR)
            .takeHighestOneBit(),
    )

    fun withCoreSize(size: Int) = copy(coreSize = size.coerceIn(MIN_CORE_SIZE, MAX_CORE_SIZE))

    companion object {
        const val MIN_INTERMEDIATE_DIMENSIONS = 0
        const val MAX_INTERMEDIATE_DIMENSIONS = 8
        const val MIN_TRANSITION_FACTOR = 1
        const val MAX_TRANSITION_FACTOR = 64
        const val MIN_CORE_SIZE = 8
        const val MAX_CORE_SIZE = 128
        const val CORE_SIZE_STEP = 8

        fun default() = EditablePlanet(
            name = "Terra",
            intermediateDimensionCount = 2,
            dimensionTransitionFactor = 8,
            coreSize = 32,
        )
    }
}
