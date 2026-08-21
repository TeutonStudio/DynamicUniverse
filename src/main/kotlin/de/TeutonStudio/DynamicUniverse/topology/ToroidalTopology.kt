package de.TeutonStudio.DynamicUniverse.topology

import java.lang.Math.floorMod

data class HorizontalPeriod(val blocks: Long) {
    init {
        require(blocks > 0 && blocks % 16L == 0L) { "Period must be positive and chunk-aligned." }
        require(blocks <= MAX_SUPPORTED_BLOCKS) { "Period exceeds the supported Minecraft/IP wrapping range." }
    }

    /** Avoids adding half the period to an arbitrary Long coordinate and therefore cannot overflow. */
    fun canonical(coordinate: Long): Long {
        val remainder = floorMod(coordinate, blocks)
        return if (remainder >= blocks / 2) remainder - blocks else remainder
    }

    val halfBlocks: Long get() = blocks / 2

    companion object {
        /** Below the vanilla world border and representable by Immersive Portals' Int API. */
        const val MAX_SUPPORTED_BLOCKS: Long = 29_999_984L
    }
}

data class HorizontalPosition(val x: Long, val z: Long)

enum class HorizontalEdge { EAST, WEST, NORTH, SOUTH }

data class HorizontalConnection(
    val entered: HorizontalEdge,
    val destination: HorizontalPosition,
)

class ToroidalTopology(private val period: HorizontalPeriod) {
    fun canonical(position: HorizontalPosition) = HorizontalPosition(period.canonical(position.x), period.canonical(position.z))

    fun cross(position: HorizontalPosition): HorizontalConnection? {
        val half = period.blocks / 2
        return when {
            position.x >= half -> HorizontalConnection(HorizontalEdge.EAST, canonical(HorizontalPosition(position.x - period.blocks, position.z)))
            position.x < -half -> HorizontalConnection(HorizontalEdge.WEST, canonical(HorizontalPosition(position.x + period.blocks, position.z)))
            position.z >= half -> HorizontalConnection(HorizontalEdge.SOUTH, canonical(HorizontalPosition(position.x, position.z - period.blocks)))
            position.z < -half -> HorizontalConnection(HorizontalEdge.NORTH, canonical(HorizontalPosition(position.x, position.z + period.blocks)))
            else -> null
        }
    }
}
