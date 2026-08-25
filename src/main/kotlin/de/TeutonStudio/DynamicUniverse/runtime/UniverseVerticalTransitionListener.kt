package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Server fallback for vertical loops and optional air seams when no visible portal adapter is active. */
@EventBusSubscriber(modid = DynamicUniverse.MOD_ID)
object UniverseVerticalTransitionListener {
    private val cooldownUntilTick = ConcurrentHashMap<UUID, Long>()

    /** Predictive hand-off prevents vanilla from applying a void-fall tick at the lower boundary. */
    @SubscribeEvent
    fun beforeEntityTick(event: EntityTickEvent.Pre) {
        val entity = event.entity
        val level = entity.level() as? ServerLevel ?: return
        if (VerticalBoundaryPortalRuntime.usesPortal(DimensionId(level.dimension().location().toString()))) return
        val velocity = entity.deltaMovement
        if (velocity.y == 0.0) return
        transition(entity, level, entity.y + velocity.y)
    }

    @SubscribeEvent
    fun afterEntityTick(event: EntityTickEvent.Post) {
        val entity = event.entity
        val level = entity.level() as? ServerLevel ?: return
        if (VerticalBoundaryPortalRuntime.usesPortal(DimensionId(level.dimension().location().toString()))) return
        transition(entity, level, entity.y)
    }

    private fun transition(entity: net.minecraft.world.entity.Entity, level: ServerLevel, y: Double) {
        if (entity.isRemoved || entity.isPassenger) return
        val tick = level.gameTime
        if ((cooldownUntilTick[entity.uuid] ?: Long.MIN_VALUE) > tick) return
        val state = TraversalState(
            SpatialPosition(entity.x, y, entity.z),
            SpatialVelocity(entity.deltaMovement.x, entity.deltaMovement.y, entity.deltaMovement.z),
            entity.passengers.map { it.uuid },
        )
        val traversal = VerticalDimensionTransitionRuntime.traverse(
            DimensionId(level.dimension().location().toString()),
            level.bounds(),
            state,
        ) { target -> level.server.level(target)?.bounds() } ?: return
        val targetLevel = level.server.level(traversal.target) ?: return
        entity.teleportTo(
            targetLevel,
            traversal.state.position.x,
            traversal.state.position.y,
            traversal.state.position.z,
            Collections.emptySet(),
            entity.yRot,
            entity.xRot,
        )
        entity.deltaMovement = Vec3(traversal.state.velocity.x, traversal.state.velocity.y, traversal.state.velocity.z)
        cooldownUntilTick[entity.uuid] = tick + COOLDOWN_TICKS
    }

    fun clear() = cooldownUntilTick.clear()

    private fun ServerLevel.bounds() = VerticalDimensionBounds(minBuildHeight.toDouble(), maxBuildHeight.toDouble())
    private fun net.minecraft.server.MinecraftServer.level(dimension: DimensionId): ServerLevel? {
        val id = ResourceLocation.tryParse(dimension.value) ?: return null
        return getLevel(ResourceKey.create(Registries.DIMENSION, id))
    }

    private const val COOLDOWN_TICKS = 2L
}
