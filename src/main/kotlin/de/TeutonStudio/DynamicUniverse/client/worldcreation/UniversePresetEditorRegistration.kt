package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldPresets
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent

/** Connects Vanilla's existing "Customize" button to the Universe hierarchy editor. */
@EventBusSubscriber(
    modid = DynamicUniverse.MOD_ID,
    value = [Dist.CLIENT],
    bus = EventBusSubscriber.Bus.MOD,
)
object UniversePresetEditorRegistration {
    @SubscribeEvent
    fun registerPresetEditor(event: RegisterPresetEditorsEvent) {
        event.register(UniverseWorldPresets.UNIVERSE) { createWorldScreen, _ ->
            UniverseHierarchyScreen(createWorldScreen, createWorldScreen)
        }
    }
}
