package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Vertical Universe hierarchy: galaxy, solar system, star, and planets. */
class UniverseHierarchyScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
) : Screen(Component.translatable("dynamicuniverse.universe_config.title")) {
    override fun init() {
        val universe = UniverseWorldCreationDraftStore.get(createWorldScreen).universe
        val left = width / 2 - 150
        var y = 58

        addRenderableWidget(
            Button.builder(Component.translatable("dynamicuniverse.universe_config.galaxy", universe.galaxyName)) { }
                .bounds(left, y, 300, 20).build(),
        ).active = false
        y += 24
        addRenderableWidget(
            Button.builder(Component.translatable("dynamicuniverse.universe_config.solar_system", universe.solarSystemName)) { }
                .bounds(left + 16, y, 284, 20).build(),
        ).active = false
        y += 24
        addRenderableWidget(
            Button.builder(Component.translatable("dynamicuniverse.universe_config.star", universe.starName)) {
                minecraft?.setScreen(
                    WorldCreationInfoScreen(
                        this,
                        Component.translatable("dynamicuniverse.star_config.title", universe.starName),
                        listOf(Component.translatable("dynamicuniverse.star_config.info")),
                    ),
                )
            }.bounds(left + 32, y, 268, 20).build(),
        )
        y += 24
        addRenderableWidget(
            Button.builder(Component.translatable("dynamicuniverse.universe_config.planet", universe.planet.name)) {
                minecraft?.setScreen(PlanetConfigurationScreen(createWorldScreen, this))
            }.bounds(left + 32, y, 268, 20).build(),
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
            Component.translatable("dynamicuniverse.universe_config.subtitle"),
            width / 2,
            38,
            SECONDARY_TEXT_COLOR,
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private companion object {
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
    }
}
