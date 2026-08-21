package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldPresets
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ScreenEvent
import java.util.WeakHashMap

/**
 * Keeps Immersive Portals' Dimension Stack mutually exclusive with the Universe preset.
 *
 * Immersive Portals adds this button through a mixin and offers no public reset API for
 * a pending stack. The reflection is therefore client-only, optional, and only reached
 * after the user has selected Universe.
 */
@EventBusSubscriber(modid = DynamicUniverse.MOD_ID, value = [Dist.CLIENT])
object UniverseDimensionStackGuard {
    private const val DIMENSION_STACK_BUTTON_KEY = "imm_ptl.altius_screen_button"
    private val previousUniverseSelection = WeakHashMap<CreateWorldScreen, Boolean>()

    @SubscribeEvent
    fun updateDimensionStackAvailability(event: ScreenEvent.Render.Pre) {
        val screen = event.screen as? CreateWorldScreen ?: return
        val universeSelected = screen.uiState.worldType.preset()?.unwrapKey()?.orElse(null) == UniverseWorldPresets.UNIVERSE
        dimensionStackButton(screen.children())?.let { button ->
            button.active = !universeSelected
            button.setTooltip(
                if (universeSelected) Tooltip.create(Component.translatable("dynamicuniverse.dimension_stack.disabled")) else null,
            )
        }

        val wasUniverseSelected = previousUniverseSelection.put(screen, universeSelected) == true
        if (universeSelected && !wasUniverseSelected) {
            clearImmersivePortalsPendingStack(screen)
        }
    }

    private fun dimensionStackButton(children: List<GuiEventListener>): Button? =
        children.filterIsInstance<Button>().firstOrNull { button ->
            (button.message.contents as? TranslatableContents)?.key == DIMENSION_STACK_BUTTON_KEY
        }

    private fun clearImmersivePortalsPendingStack(screen: CreateWorldScreen) {
        runCatching {
            val managementClass = Class.forName(
                "qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement",
                false,
                screen.javaClass.classLoader,
            )
            managementClass.getField("dimStackToApply").set(null, null)
        }
    }
}
