package de.TeutonStudio.DynamicUniverse.compat.aether

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.runtime.VerticalBoundaryPortalRuntime
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level

/**
 * Stops Aether's decorative vanilla travel packet for a DynamicUniverse seam that Immersive
 * Portals already owns. It intentionally does not cancel NeoForge's dimension-travel event:
 * Immersive Portals still needs that event to complete the seamless transfer.
 */
object AetherVerticalSeamTravelGuard {
    @JvmStatic
    fun suppressStandardTravel(entity: Entity, destination: ResourceKey<Level>): Boolean {
        val sourceLevel = entity.level() as? ServerLevel ?: return false
        return isManagedTransition(
            DimensionId(sourceLevel.dimension().location().toString()),
            DimensionId(destination.location().toString()),
        )
    }

    internal fun isManagedTransition(source: DimensionId, target: DimensionId): Boolean =
        VerticalBoundaryPortalRuntime.usesPortalTransition(source, target)
}
