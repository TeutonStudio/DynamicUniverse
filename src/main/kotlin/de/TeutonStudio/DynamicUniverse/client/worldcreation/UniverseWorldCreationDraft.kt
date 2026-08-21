package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaries
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryMismatch
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryType
import de.TeutonStudio.DynamicUniverse.dimension.PlanetDimensionStackValidator

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
)

data class EditablePlanet(
    val name: String,
    val dimensionTransitionFactor: Int,
    val coreSize: Int,
    val dimensions: List<EditablePlanetDimension>,
) {
    init {
        require(dimensionTransitionFactor in MIN_TRANSITION_FACTOR..MAX_TRANSITION_FACTOR)
        require(coreSize in MIN_CORE_SIZE..MAX_CORE_SIZE)
        require(dimensions.count { it.kind == EditablePlanetDimensionKind.CORE } == 1) { "A planet needs one core dimension." }
        require(dimensions.count { it.kind == EditablePlanetDimensionKind.SURFACE } == 1) { "A planet needs one surface dimension." }
        require(dimensions.count { it.kind == EditablePlanetDimensionKind.SKY } == 1) { "A planet needs one sky dimension." }
        require(dimensions.first().kind == EditablePlanetDimensionKind.CORE) { "The core must be the innermost dimension." }
        require(dimensions.last().kind == EditablePlanetDimensionKind.SKY) { "The sky must be the outermost dimension." }
        require(intermediateDimensionCount in MIN_INTERMEDIATE_DIMENSIONS..MAX_INTERMEDIATE_DIMENSIONS)
    }

    val intermediateDimensionCount: Int
        get() = dimensions.count { it.kind == EditablePlanetDimensionKind.INTERMEDIATE }

    val incompatibleDimensionTransitions: List<DimensionBoundaryMismatch>
        get() = PlanetDimensionStackValidator.incompatibleTransitions(dimensions.map(EditablePlanetDimension::boundaries))

    fun withIntermediateDimensionCount(count: Int) = copy(
        dimensions = defaultDimensions(count.coerceIn(MIN_INTERMEDIATE_DIMENSIONS, MAX_INTERMEDIATE_DIMENSIONS)),
    )

    fun withTransitionFactor(factor: Int) = copy(
        dimensionTransitionFactor = factor.coerceIn(MIN_TRANSITION_FACTOR, MAX_TRANSITION_FACTOR),
    )

    fun withCoreSize(size: Int) = copy(coreSize = size.coerceIn(MIN_CORE_SIZE, MAX_CORE_SIZE))

    fun withDimensionBoundary(
        dimensionIndex: Int,
        edge: DimensionEdge,
        boundary: DimensionBoundaryType,
    ): EditablePlanet = copy(
        dimensions = dimensions.mapIndexed { index, dimension ->
            if (index != dimensionIndex) dimension
            else dimension.copy(
                boundaries = if (dimension.kind == EditablePlanetDimensionKind.SURFACE) {
                    when (edge) {
                        DimensionEdge.INNER -> DimensionBoundaries(boundary, boundary.opposite())
                        DimensionEdge.OUTER -> DimensionBoundaries(boundary.opposite(), boundary)
                    }
                } else DimensionBoundaries(boundary, boundary),
            )
        },
    )

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
            dimensionTransitionFactor = 4,
            coreSize = 32,
            dimensions = defaultDimensions(2),
        )

        private fun defaultDimensions(intermediateDimensionCount: Int): List<EditablePlanetDimension> = buildList {
            add(EditablePlanetDimension("core", "Planetenkern", EditablePlanetDimensionKind.CORE, DimensionBoundaries.BEDROCK_TO_BEDROCK))
            repeat(intermediateDimensionCount) { index ->
                add(
                    EditablePlanetDimension(
                        "layer_${index + 1}",
                        "Dimension ${index + 1}",
                        EditablePlanetDimensionKind.INTERMEDIATE,
                        DimensionBoundaries.BEDROCK_TO_BEDROCK,
                    ),
                )
            }
            add(EditablePlanetDimension("surface", "Oberfläche", EditablePlanetDimensionKind.SURFACE, DimensionBoundaries.BEDROCK_TO_AIR))
            add(EditablePlanetDimension("sky", "Himmel", EditablePlanetDimensionKind.SKY, DimensionBoundaries.AIR_TO_AIR))
        }
    }
}

enum class EditablePlanetDimensionKind { CORE, INTERMEDIATE, SURFACE, SKY }

enum class DimensionEdge { INNER, OUTER }

private fun DimensionBoundaryType.opposite(): DimensionBoundaryType = when (this) {
    DimensionBoundaryType.AIR -> DimensionBoundaryType.BEDROCK
    DimensionBoundaryType.BEDROCK -> DimensionBoundaryType.AIR
}

data class EditablePlanetDimension(
    val id: String,
    val name: String,
    val kind: EditablePlanetDimensionKind,
    val boundaries: DimensionBoundaries,
) {
    init {
        require((kind == EditablePlanetDimensionKind.SURFACE) == (boundaries.inner != boundaries.outer)) {
            "Only a surface may have BEDROCK on one edge and AIR on the other."
        }
    }
}
