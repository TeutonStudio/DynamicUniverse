package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.Component
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ScreenEvent

/** Adds the DynamicUniverse entry point to Minecraft's normal world creation screen. */
@EventBusSubscriber(modid = DynamicUniverse.MOD_ID, value = [Dist.CLIENT])
object UniverseWorldCreationClient {
    @SubscribeEvent
    fun addWorldTypeSelector(event: ScreenEvent.Init.Post) {
        val screen = event.screen as? CreateWorldScreen ?: return
        val button = Button.builder(Component.translatable("dynamicuniverse.world_creation.open")) {
            screen.minecraft.setScreen(UniverseWorldTypeScreen(screen))
        }
            .bounds(screen.width - BUTTON_WIDTH - MARGIN, MARGIN, BUTTON_WIDTH, Button.DEFAULT_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("dynamicuniverse.world_creation.open.tooltip")))
            .build()
        event.addListener(button)
    }

    private const val BUTTON_WIDTH = 104
    private const val MARGIN = 5
}
