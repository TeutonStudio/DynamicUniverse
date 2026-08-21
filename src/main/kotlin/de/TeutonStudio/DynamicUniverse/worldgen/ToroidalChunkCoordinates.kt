package de.TeutonStudio.DynamicUniverse.worldgen

import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod

/**
 * The contract a later chunk generator uses before sampling noise, structures, or features.
 * Portal wrapping without this canonicalization would visibly join unrelated terrain.
 */
class ToroidalChunkCoordinates(private val period: HorizontalPeriod) {
    private val topologyPeriodChunks = period.blocks / 16L

    fun canonical(x: Long, z: Long): ToroidalChunkPosition =
        ToroidalChunkPosition(canonicalChunk(x), canonicalChunk(z))

    private fun canonicalChunk(coordinate: Long): Long {
        val remainder = Math.floorMod(coordinate, topologyPeriodChunks)
        return if (remainder >= topologyPeriodChunks / 2L) remainder - topologyPeriodChunks else remainder
    }
}

data class ToroidalChunkPosition(val x: Long, val z: Long)
