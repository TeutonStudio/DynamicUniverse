package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Second hierarchy level: a galaxy's solar systems and clouds. */
class GalaxyContentsScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
    private val galaxyIndex: Int,
) : Screen(Component.translatable("dynamicuniverse.galaxy_config.screen_title")) {
    override fun init() {
        val galaxy = galaxy()
        val listItems = galaxy.entries.mapIndexed { entryIndex, entry ->
            when (entry) {
                is EditableSolarSystem -> UniverseListItem(
                    Component.translatable("dynamicuniverse.universe_config.solar_system", entry.name),
                ) {
                    minecraft?.setScreen(SolarSystemContentsScreen(createWorldScreen, this, galaxyIndex, entryIndex))
                }
                is EditableCloud -> UniverseListItem(Component.translatable("dynamicuniverse.universe_config.cloud", entry.name)) {
                    minecraft?.setScreen(
                        WorldCreationInfoScreen(
                            this,
                            Component.translatable("dynamicuniverse.cloud_config.title", entry.name),
                            listOf(Component.translatable("dynamicuniverse.cloud_config.info")),
                        ),
                    )
                }
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
        guiGraphics.drawCenteredString(font, Component.translatable("dynamicuniverse.galaxy_config.title", galaxy().name), width / 2, TITLE_Y, TEXT_COLOR)
        guiGraphics.drawCenteredString(
            font,
            Component.translatable("dynamicuniverse.galaxy_config.subtitle"),
            width / 2,
            SUBTITLE_Y,
            SECONDARY_TEXT_COLOR,
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private fun galaxy() = UniverseWorldCreationDraftStore.get(createWorldScreen).universe.galaxies[galaxyIndex]

    private companion object {
        const val TITLE_Y = 20
        const val SUBTITLE_Y = 38
        const val LIST_TOP = 56
        const val FOOTER_HEIGHT = 40
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
    }
}
