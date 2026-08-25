package de.TeutonStudio.DynamicUniverse.client.render

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.runtime.StackRenderContext
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent

/** Keeps the optional DH classes unreachable unless the DH runtime is actually installed. */
@EventBusSubscriber(modid = DynamicUniverse.MOD_ID, value = [Dist.CLIENT], bus = EventBusSubscriber.Bus.MOD)
object DistantHorizonsClientLifecycle {
    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            if (!ModList.get().isLoaded("distanthorizons")) return@enqueueWork
            runCatching {
                Class.forName("de.TeutonStudio.DynamicUniverse.compat.distanthorizons.DistantHorizonsStackRenderAdapter")
                    .getMethod("install")
                    .invoke(null)
            }
        }
    }
}

@EventBusSubscriber(modid = DynamicUniverse.MOD_ID, value = [Dist.CLIENT])
object StackRenderClientDisconnect {
    @SubscribeEvent
    fun onLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        StackRenderContext.clear()
    }
}
