package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId

/** A generated Bedrock plane. Generator adapters report it; runtime code never guesses world height. */
data class BedrockBoundaryPlane(
    val dimension: DimensionId,
    val face: DimensionBoundaryFace,
    val y: Int,
)
