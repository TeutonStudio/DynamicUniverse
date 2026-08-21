package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Edits the three planet values supported by the current Universe world-type draft. */
class PlanetConfigurationScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
) : Screen(Component.translatable("dynamicuniverse.planet_config.title")) {
    override fun init() {
        val left = width / 2 - 150
        addValueControls(
            y = 64,
            label = planet().intermediateDimensionCount.toString(),
            title = Component.translatable("dynamicuniverse.planet_config.layers"),
            decrease = { updatePlanet { it.withIntermediateDimensionCount(it.intermediateDimensionCount - 1) } },
            increase = { updatePlanet { it.withIntermediateDimensionCount(it.intermediateDimensionCount + 1) } },
        )
        addValueControls(
            y = 108,
            label = "1:${planet().dimensionTransitionFactor}",
            title = Component.translatable("dynamicuniverse.planet_config.transition_factor"),
            decrease = { updatePlanet { it.withTransitionFactor(it.dimensionTransitionFactor / 2) } },
            increase = { updatePlanet { it.withTransitionFactor(it.dimensionTransitionFactor * 2) } },
        )
        addValueControls(
            y = 152,
            label = planet().coreSize.toString(),
            title = Component.translatable("dynamicuniverse.planet_config.core_size"),
            decrease = { updatePlanet { it.withCoreSize(it.coreSize - EditablePlanet.CORE_SIZE_STEP) } },
            increase = { updatePlanet { it.withCoreSize(it.coreSize + EditablePlanet.CORE_SIZE_STEP) } },
        )
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { onClose() }
                .bounds(left, height - 28, 300, 20)
                .build(),
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, 20, TEXT_COLOR)
        guiGraphics.drawCenteredString(
            font,
            Component.translatable("dynamicuniverse.planet_config.subtitle", planet().name),
            width / 2,
            38,
            SECONDARY_TEXT_COLOR,
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private fun addValueControls(
        y: Int,
        title: Component,
        label: String,
        decrease: () -> Unit,
        increase: () -> Unit,
    ) {
        val left = width / 2 - 150
        addRenderableWidget(Button.builder(Component.literal("-")) {
            decrease()
            minecraft?.setScreen(PlanetConfigurationScreen(createWorldScreen, parent))
        }.bounds(left, y, 40, 20).build())
        addRenderableWidget(Button.builder(Component.literal(label)) { }.bounds(left + 44, y, 212, 20).build()).active = false
        addRenderableWidget(Button.builder(Component.literal("+")) {
            increase()
            minecraft?.setScreen(PlanetConfigurationScreen(createWorldScreen, parent))
        }.bounds(left + 260, y, 40, 20).build())
        addRenderableWidget(Button.builder(title) { }.bounds(left, y - 22, 300, 20).build()).active = false
    }

    private fun planet(): EditablePlanet = UniverseWorldCreationDraftStore.get(createWorldScreen).universe.planet

    private fun updatePlanet(transform: (EditablePlanet) -> EditablePlanet) {
        UniverseWorldCreationDraftStore.update(createWorldScreen) { draft ->
            draft.copy(universe = draft.universe.copy(planet = transform(draft.universe.planet)))
        }
    }

    private companion object {
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
    }
}
