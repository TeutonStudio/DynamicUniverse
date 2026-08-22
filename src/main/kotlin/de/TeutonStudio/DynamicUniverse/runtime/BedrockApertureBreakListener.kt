package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionPosition
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.level.BlockEvent

/**
 * Installs the generated Bedrock planes for the active Universe world. Calling this is part of
 * server-level creation, after all local planet dimensions have been created and loaded.
 */
object BedrockApertureRuntime {
    @Volatile
    private var bridge: ServerBedrockApertureBridge? = null

    fun install(manifest: UniverseGeometryManifest, planes: Collection<BedrockBoundaryPlane>) {
        bridge = ServerBedrockApertureBridge(manifest.links, planes)
    }

    fun clear() {
        bridge = null
    }

    internal fun mirrorBedrockBreak(sourceLevel: ServerLevel, sourcePos: BlockPos): BedrockAperture? =
        bridge?.mirrorBedrockBreak(sourceLevel, sourcePos)
}

/** Minecraft-specific half of the aperture operation. It never opens a non-Bedrock target. */
class ServerBedrockApertureBridge(
    connections: Collection<de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection>,
    planes: Collection<BedrockBoundaryPlane>,
) {
    private val planner = BedrockAperturePlanner(connections, planes)

    fun mirrorBedrockBreak(sourceLevel: ServerLevel, sourcePos: BlockPos): BedrockAperture? {
        if (!sourceLevel.getBlockState(sourcePos).`is`(Blocks.BEDROCK)) return null
        val sourceDimension = DimensionId(sourceLevel.dimension().location().toString())
        val source = DimensionPosition(sourcePos.x.toLong(), sourcePos.y.toLong(), sourcePos.z.toLong())
        val aperture = planner.apertureFor(sourceDimension, source) ?: return null
        val targetLevel = sourceLevel.server.allLevels.firstOrNull { level ->
            level.dimension().location().toString() == aperture.connection.target.value
        } ?: return null
        val targetPos = aperture.target.toBlockPosOrNull() ?: return null
        if (!targetLevel.getBlockState(targetPos).`is`(Blocks.BEDROCK)) return null

        targetLevel.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)
        return aperture
    }
}

/** Mirrors a player-created boundary hole before NeoForge completes the source block break. */
@EventBusSubscriber(modid = DynamicUniverse.MOD_ID)
object BedrockApertureBreakListener {
    @SubscribeEvent
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        if (event.isCanceled || !event.state.`is`(Blocks.BEDROCK)) return
        val level = event.level as? ServerLevel ?: return
        BedrockApertureRuntime.mirrorBedrockBreak(level, event.pos)
    }
}

private fun DimensionPosition.toBlockPosOrNull(): BlockPos? {
    if (x !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return null
    if (y !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return null
    if (z !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return null
    return BlockPos(x.toInt(), y.toInt(), z.toInt())
}
