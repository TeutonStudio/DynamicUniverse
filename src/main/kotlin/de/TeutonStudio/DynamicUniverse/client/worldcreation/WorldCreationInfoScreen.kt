package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Small modal-style information screen with a deterministic return target. */
class WorldCreationInfoScreen(
    private val parent: Screen,
    title: Component,
    private val lines: List<Component>,
) : Screen(title) {
    override fun init() {
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { onClose() }
                .bounds(width / 2 - 100, height - 28, 200, 20)
                .build(),
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, 50, TEXT_COLOR)
        lines.forEachIndexed { index, line ->
            guiGraphics.drawCenteredString(font, line, width / 2, 84 + index * 14, SECONDARY_TEXT_COLOR)
        }
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private companion object {
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
    }
}
