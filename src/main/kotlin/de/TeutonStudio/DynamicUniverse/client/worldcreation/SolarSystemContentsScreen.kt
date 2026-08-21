package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Third hierarchy level: a dedicated sun button and a vertical list of planets. */
class SolarSystemContentsScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
    private val galaxyIndex: Int,
    private val entryIndex: Int,
) : Screen(Component.translatable("dynamicuniverse.solar_system_config.screen_title")) {
    override fun init() {
        val solarSystem = solarSystem()
        addRenderableWidget(
            Button.builder(Component.translatable("dynamicuniverse.universe_config.star", solarSystem.star.name)) {
                minecraft?.setScreen(
                    WorldCreationInfoScreen(
                        this,
                        Component.translatable("dynamicuniverse.star_config.title", solarSystem.star.name),
                        listOf(Component.translatable("dynamicuniverse.star_config.info")),
                    ),
                )
            }.bounds(width / 2 - 150, SUN_BUTTON_Y, 300, 20).build(),
        )
        val listItems = solarSystem.planets.mapIndexed { planetIndex, planet ->
            UniverseListItem(Component.translatable("dynamicuniverse.universe_config.planet", planet.name)) {
                minecraft?.setScreen(PlanetConfigurationScreen(createWorldScreen, this, galaxyIndex, entryIndex, planetIndex))
            }
        }
        addRenderableWidget(UniverseVerticalList(requireNotNull(minecraft), width, height, LIST_TOP, height - FOOTER_HEIGHT, listItems))
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { onClose() }
                .bounds(width / 2 - 150, height - 28, 300, 20)
                .build(),
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(
            font,
            Component.translatable("dynamicuniverse.solar_system_config.title", solarSystem().name),
            width / 2,
            TITLE_Y,
            TEXT_COLOR,
        )
        guiGraphics.drawCenteredString(
            font,
            Component.translatable("dynamicuniverse.solar_system_config.planets"),
            width / 2,
            PLANETS_LABEL_Y,
            SECONDARY_TEXT_COLOR,
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private fun solarSystem(): EditableSolarSystem =
        UniverseWorldCreationDraftStore.get(createWorldScreen).universe.galaxies[galaxyIndex].entries[entryIndex] as EditableSolarSystem

    private companion object {
        const val TITLE_Y = 12
        const val SUN_BUTTON_Y = 32
        const val PLANETS_LABEL_Y = 60
        const val LIST_TOP = 76
        const val FOOTER_HEIGHT = 40
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
    }
}
