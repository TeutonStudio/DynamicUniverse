package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent
import java.util.Collections

/**
 * Non-IP fallback for a finite horizontal layer. Only vehicle roots are moved; Minecraft
 * keeps their passenger tree attached, so no passenger is independently canonicalized.
 */
@EventBusSubscriber(modid = DynamicUniverse.MOD_ID)
object UniverseHorizontalTransitionListener {
    @SubscribeEvent
    fun afterEntityTick(event: EntityTickEvent.Post) {
        val entity = event.entity
        if (entity.isRemoved || entity.isPassenger) return
        val level = entity.level() as? ServerLevel ?: return
        val state = TraversalState(
            SpatialPosition(entity.x, entity.y, entity.z),
            SpatialVelocity(entity.deltaMovement.x, entity.deltaMovement.y, entity.deltaMovement.z),
            entity.passengers.map { it.uuid },
        )
        val wrapped = UniverseTransitionRuntime.horizontal(DimensionId(level.dimension().location().toString()), state) ?: return
        entity.teleportTo(
            level,
            wrapped.position.x,
            wrapped.position.y,
            wrapped.position.z,
            Collections.emptySet(),
            entity.yRot,
            entity.xRot,
        )
        entity.deltaMovement = Vec3(wrapped.velocity.x, wrapped.velocity.y, wrapped.velocity.z)
    }
}
