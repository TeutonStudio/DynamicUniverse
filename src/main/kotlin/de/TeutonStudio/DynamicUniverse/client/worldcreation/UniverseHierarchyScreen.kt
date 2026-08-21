package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** First hierarchy level: a vertical list of galaxies. */
class UniverseHierarchyScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
) : Screen(Component.translatable("dynamicuniverse.universe_config.title")) {
    override fun init() {
        val galaxies = UniverseWorldCreationDraftStore.get(createWorldScreen).universe.galaxies
        val listItems = galaxies.mapIndexed { index, galaxy ->
            UniverseListItem(Component.translatable("dynamicuniverse.universe_config.galaxy", galaxy.name)) {
                minecraft?.setScreen(GalaxyContentsScreen(createWorldScreen, this, index))
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
        guiGraphics.drawCenteredString(font, title, width / 2, TITLE_Y, TEXT_COLOR)
        guiGraphics.drawCenteredString(
            font,
            Component.translatable("dynamicuniverse.universe_config.galaxies.subtitle"),
            width / 2,
            SUBTITLE_Y,
            SECONDARY_TEXT_COLOR,
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private companion object {
        const val TITLE_Y = 20
        const val SUBTITLE_Y = 38
        const val LIST_TOP = 56
        const val FOOTER_HEIGHT = 40
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
    }
}
