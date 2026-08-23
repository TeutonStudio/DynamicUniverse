package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import net.minecraft.server.level.ServerLevel
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * Core interiors have no privileged world-Y direction. The shell is solid, while entities in
 * the void float and use Minecraft's swimming pose for an unambiguous zero-gravity affordance.
 */
@EventBusSubscriber(modid = DynamicUniverse.MOD_ID)
object PlanetCorePhysics {
    private val restoredStates = ConcurrentHashMap<java.util.UUID, CoreMotionState>()

    @SubscribeEvent
    fun afterEntityTick(event: EntityTickEvent.Post) {
        val entity = event.entity
        val level = entity.level() as? ServerLevel ?: return
        val inCore = BedrockApertureRuntime.isPlanetCore(DimensionId(level.dimension().location().toString()))
        if (inCore) {
            restoredStates.putIfAbsent(entity.uuid, CoreMotionState(entity.isNoGravity, entity.isSwimming))
            entity.setNoGravity(true)
            entity.setSwimming(true)
            entity.fallDistance = 0f
        } else {
            restoredStates.remove(entity.uuid)?.let { before ->
                entity.setNoGravity(before.noGravity)
                entity.setSwimming(before.swimming)
            }
        }
    }

    private data class CoreMotionState(val noGravity: Boolean, val swimming: Boolean)
}
