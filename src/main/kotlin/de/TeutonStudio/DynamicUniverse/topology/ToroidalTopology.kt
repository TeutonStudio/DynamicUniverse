package de.TeutonStudio.DynamicUniverse.topology

import java.lang.Math.floorMod

data class HorizontalPeriod(val blocks: Long) {
    init { require(blocks > 0 && blocks % 16L == 0L) { "Period must be positive and chunk-aligned." } }

    fun canonical(coordinate: Long): Long = floorMod(coordinate + blocks / 2, blocks) - blocks / 2
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
