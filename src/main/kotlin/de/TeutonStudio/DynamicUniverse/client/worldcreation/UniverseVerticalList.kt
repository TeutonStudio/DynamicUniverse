package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.network.chat.Component

/** A click-selectable vertical list, styled after the Flat World layer list. */
class UniverseVerticalList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    top: Int,
    bottom: Int,
    items: List<UniverseListItem>,
) : ObjectSelectionList<UniverseVerticalList.Entry>(minecraft, width, height, top, bottom) {
    init {
        replaceItems(items)
    }

    override fun getRowWidth() = ROW_WIDTH

    fun replaceItems(items: List<UniverseListItem>) {
        clearEntries()
        items.forEach { addEntry(Entry(it)) }
    }

    inner class Entry(
        private val item: UniverseListItem,
    ) : ObjectSelectionList.Entry<Entry>() {
        override fun render(
            guiGraphics: GuiGraphics,
            index: Int,
            top: Int,
            left: Int,
            width: Int,
            height: Int,
            mouseX: Int,
            mouseY: Int,
            hovering: Boolean,
            partialTick: Float,
        ) {
            guiGraphics.drawString(
                minecraft.font,
                item.label,
                left + ROW_TEXT_INSET + item.indentation * INDENT_WIDTH,
                top + 5,
                TEXT_COLOR,
                false,
            )
        }

        override fun getNarration(): Component = item.label

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (button != 0) return false
            this@UniverseVerticalList.setSelected(this)
            item.onSelect()
            return true
        }
    }

    private companion object {
        const val ROW_WIDTH = 300
        const val ROW_TEXT_INSET = 8
        const val INDENT_WIDTH = 16
        const val TEXT_COLOR = 0xFFFFFF
    }
}

data class UniverseListItem(
    val label: Component,
    val indentation: Int = 0,
    val onSelect: () -> Unit,
)
