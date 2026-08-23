package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.chunk.ChunkAccess
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.level.ChunkEvent

/** Generates the playable Bedrock shell inside the otherwise void planet-core level. */
@EventBusSubscriber(modid = DynamicUniverse.MOD_ID)
object PlanetCoreShellMaterializer {
    @SubscribeEvent
    fun onChunkLoad(event: ChunkEvent.Load) {
        val level = event.chunk.level as? ServerLevel ?: return
        BedrockApertureRuntime.materializeCoreShell(level, event.chunk)
    }

    fun materialize(chunk: ChunkAccess, geometry: PlanetCoreGeometry, openings: Set<BlockPos>) {
        val half = geometry.edgeBlocks.toInt() / 2
        val min = -half
        val max = half - 1
        val minY = chunk.minBuildHeight.coerceAtLeast(min)
        val maxY = (chunk.minBuildHeight + chunk.height - 1).coerceAtMost(max)
        if (minY > maxY) return
        val minX = chunk.pos.minBlockX
        val maxX = chunk.pos.maxBlockX
        val minZ = chunk.pos.minBlockZ
        val maxZ = chunk.pos.maxBlockZ

        fun place(x: Int, y: Int, z: Int) {
            val pos = BlockPos(x, y, z)
            if (pos in openings || !chunk.getBlockState(pos).isAir) return
            chunk.setBlockState(pos, net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(), false)
        }
        fun horizontal(y: Int) {
            if (y !in minY..maxY) return
            for (x in minX..maxX) for (z in minZ..maxZ) place(x, y, z)
        }
        fun xFace(x: Int) {
            if (x !in minX..maxX) return
            for (y in minY..maxY) for (z in minZ..maxZ) place(x, y, z)
        }
        fun zFace(z: Int) {
            if (z !in minZ..maxZ) return
            for (y in minY..maxY) for (x in minX..maxX) place(x, y, z)
        }

        horizontal(min)
        horizontal(max)
        xFace(min)
        xFace(max)
        zFace(min)
        zFace(max)
    }

    /** Ensures a newly selected projection lands on shell Bedrock before its transaction checks it. */
    fun ensurePositions(level: ServerLevel, positions: Set<BlockPos>) {
        positions.forEach { position ->
            // Loading first lets the chunk-load listener build the complete shell; this also
            // removes the first-attempt race between a fresh portal target and its chunk.
            level.getChunkAt(position)
            if (level.getBlockState(position).isAir) {
                level.setBlock(position, net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(), 2)
            }
        }
    }
}
