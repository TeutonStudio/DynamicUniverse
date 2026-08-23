package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent

/** Loads persisted Universe topology only after all server levels are available. */
@EventBusSubscriber(modid = DynamicUniverse.MOD_ID)
object UniverseRuntimeLifecycle {
    @SubscribeEvent
    fun onServerStarted(event: ServerStartedEvent) {
        if (UniverseWorldCreationBridge.restore(event.server)) return
        (UniverseWorldCreationSession.consumeFor(event.server) ?: UniversePresetBootstrap.defaultPlanIfInstalled(event.server))?.let { plan ->
            UniverseWorldCreationBridge.create(event.server, plan.worldType, plan.bedrockPlanes)
        }
    }

    @SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        UniverseWorldCreationBridge.clear()
        UniverseWorldCreationSession.clear()
    }
}
