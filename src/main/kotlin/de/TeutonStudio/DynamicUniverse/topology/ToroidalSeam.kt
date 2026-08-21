package de.TeutonStudio.DynamicUniverse.topology

/**
 * Materialization-neutral description of the four global portals that turn one layer into
 * a horizontal torus. It is consumed by the optional Immersive Portals bridge.
 */
data class ToroidalSeamSpec(
    val period: HorizontalPeriod,
    val minX: Int,
    val minZ: Int,
    val maxX: Int,
    val maxZ: Int,
) {
    init {
        require(minX < maxX && minZ < maxZ)
        require(maxX.toLong() - minX.toLong() == period.blocks)
        require(maxZ.toLong() - minZ.toLong() == period.blocks)
    }

    companion object {
        fun centered(period: HorizontalPeriod): ToroidalSeamSpec {
            val half = period.halfBlocks
            return ToroidalSeamSpec(period, -half.toInt(), -half.toInt(), half.toInt(), half.toInt())
        }
    }
}
