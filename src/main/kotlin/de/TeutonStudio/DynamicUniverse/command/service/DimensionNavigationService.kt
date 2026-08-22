package de.TeutonStudio.DynamicUniverse.command.service

import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionPosition
import de.TeutonStudio.DynamicUniverse.runtime.UniverseRuntime
import de.TeutonStudio.DynamicUniverse.runtime.UniverseRuntimeApi
import de.TeutonStudio.DynamicUniverse.runtime.UniverseTopology
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldType
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.neoforged.neoforge.server.ServerLifecycleHooks

interface DimensionNavigationService {
    fun moveToLayer(player: ServerPlayer, planetReference: String, stackId: String, layerId: String): CommandFeedback
}

/** Minecraft-specific seam; the navigation service itself only requests a transfer. */
interface LayerTransferGateway {
    fun level(dimension: DimensionId): ServerLevel?
    fun transfer(player: ServerPlayer, target: ServerLevel, position: DimensionPosition): Boolean
}

object MinecraftLayerTransferGateway : LayerTransferGateway {
    override fun level(dimension: DimensionId): ServerLevel? {
        val id = ResourceLocation.tryParse(dimension.value) ?: return null
        return ServerLifecycleHooks.getCurrentServer()?.getLevel(ResourceKey.create(Registries.DIMENSION, id))
    }

    override fun transfer(player: ServerPlayer, target: ServerLevel, position: DimensionPosition): Boolean {
        player.teleportTo(target, position.x.toDouble(), player.y, position.z.toDouble(), player.yRot, player.xRot)
        return true
    }
}

/** Uses the existing connection graph for both path finding and all coordinate scaling. */
object DimensionRoutePlanner {
    fun route(
        universe: UniverseWorldType,
        source: DimensionId,
        target: DimensionId,
        position: DimensionPosition,
    ): DimensionPosition? {
        if (source == target) return position
        val graph = universe.connectionGraph()
        val queue = ArrayDeque<Pair<DimensionId, List<DimensionConnection>>>()
        val visited = mutableSetOf(source)
        queue += source to emptyList()
        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()
            graph.routesFrom(current).forEach { connection ->
                if (!visited.add(connection.target)) return@forEach
                val nextPath = path + connection
                if (connection.target == target) {
                    return nextPath.fold(position) { transformed, route -> route.targetPosition(transformed) }
                }
                queue += connection.target to nextPath
            }
        }
        return null
    }
}

class StackDimensionNavigationService(
    private val runtime: UniverseRuntimeApi = UniverseRuntime.api(),
    private val gateway: LayerTransferGateway = MinecraftLayerTransferGateway,
) : DimensionNavigationService {
    override fun moveToLayer(player: ServerPlayer, planetReference: String, stackId: String, layerId: String): CommandFeedback {
        val target = UniverseTopology.resolveLayer(runtime, planetReference, stackId, layerId)
            ?: return CommandFeedback(listOf("Layer $layerId is not registered for $planetReference/$stackId."), false)
        val source = runCatching { DimensionId(player.level().dimension().location().toString()) }.getOrNull()
            ?: return CommandFeedback(listOf("The current Minecraft dimension cannot be mapped to a Universe dimension."), false)
        val position = DimensionRoutePlanner.route(
            target.stack.planet.universe,
            source,
            target.layer.dimension,
            DimensionPosition(player.blockX.toLong(), player.blockY.toLong(), player.blockZ.toLong()),
        ) ?: return CommandFeedback(listOf("No configured route connects ${source.value} to ${target.layer.dimension.value}."), false)
        val level = gateway.level(target.layer.dimension)
            ?: return CommandFeedback(listOf("Target level ${target.layer.dimension.value} is not loaded or registered."), false)
        if (!gateway.transfer(player, level, position)) {
            return CommandFeedback(listOf("The runtime gateway rejected the transfer to ${target.layer.dimension.value}."), false)
        }
        return CommandFeedback(listOf("Moved to ${target.layer.dimension.value} at x=${position.x}, y=${player.blockY}, z=${position.z}."))
    }
}
