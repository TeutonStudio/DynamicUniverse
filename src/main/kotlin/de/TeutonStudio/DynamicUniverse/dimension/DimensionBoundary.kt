package de.TeutonStudio.DynamicUniverse.dimension

/**
 * Material at one radial edge of a dimension.
 *
 * The inner edge faces the planet core; the outer edge faces space. Two adjacent
 * dimensions can only be connected when both sides of their shared edge agree.
 */
enum class DimensionBoundaryType {
    AIR,
    BEDROCK,
    ;

    fun isCompatibleWith(other: DimensionBoundaryType): Boolean = this == other

    fun next(): DimensionBoundaryType = entries[(ordinal + 1) % entries.size]
}

data class DimensionBoundaries(
    val inner: DimensionBoundaryType,
    val outer: DimensionBoundaryType,
) {
    companion object {
        val AIR_TO_AIR = DimensionBoundaries(DimensionBoundaryType.AIR, DimensionBoundaryType.AIR)
        val BEDROCK_TO_BEDROCK = DimensionBoundaries(DimensionBoundaryType.BEDROCK, DimensionBoundaryType.BEDROCK)
        val BEDROCK_TO_AIR = DimensionBoundaries(DimensionBoundaryType.BEDROCK, DimensionBoundaryType.AIR)
    }
}

/** Describes one invalid shared edge in an inner-to-outer dimension stack. */
data class DimensionBoundaryMismatch(
    val innerLayerIndex: Int,
    val outerLayerIndex: Int,
    val innerBoundary: DimensionBoundaryType,
    val outerBoundary: DimensionBoundaryType,
) {
    val message: String
        get() = "$innerBoundary cannot connect to $outerBoundary"
}

/** Shared validation for both the editable client draft and the runtime world model. */
object PlanetDimensionStackValidator {
    fun incompatibleTransitions(boundaries: List<DimensionBoundaries>): List<DimensionBoundaryMismatch> =
        boundaries.windowed(2).mapIndexedNotNull { index, (inner, outer) ->
            if (inner.outer.isCompatibleWith(outer.inner)) null
            else DimensionBoundaryMismatch(index, index + 1, inner.outer, outer.inner)
        }
}
