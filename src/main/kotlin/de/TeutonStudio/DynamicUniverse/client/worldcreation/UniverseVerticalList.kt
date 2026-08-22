package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.network.chat.Component

/** A click-selectable vertical list, styled after the Flat World layer list. */
class UniverseVerticalList(
    minecraft: Minecraft,
    width: Int,
    listHeight: Int,
    top: Int,
    rowHeight: Int,
    items: List<UniverseListItem>,
) : ObjectSelectionList<UniverseVerticalList.Entry>(minecraft, width, listHeight, top, rowHeight) {
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
            if (item.selected) guiGraphics.fill(left, top, left + width, top + height, SELECTED_BACKGROUND)
            val contentLeft = left + ROW_TEXT_INSET + item.indentation * INDENT_WIDTH
            item.onExpand?.let {
                guiGraphics.drawString(minecraft.font, if (item.expanded) "⌄" else "›", contentLeft, top + 5, item.color, false)
            }
            guiGraphics.drawString(
                minecraft.font,
                item.label,
                contentLeft + if (item.onExpand == null) 0 else ICON_WIDTH,
                top + 5,
                item.color,
                false,
            )
            item.onEdit?.let { guiGraphics.drawString(minecraft.font, "✎", left + width - EDIT_INSET, top + 5, item.color, false) }
        }

        override fun getNarration(): Component = item.label

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (button != 0) return false
            this@UniverseVerticalList.setSelected(this)
            val rowLeft = this@UniverseVerticalList.width / 2 - ROW_WIDTH / 2
            val localX = mouseX - rowLeft
            val itemLeft = ROW_TEXT_INSET + item.indentation * INDENT_WIDTH
            when {
                item.onExpand != null && localX in itemLeft.toDouble()..(itemLeft + ICON_WIDTH).toDouble() -> item.onExpand.invoke()
                item.onEdit != null && localX >= ROW_WIDTH - EDIT_HIT_WIDTH -> item.onEdit.invoke()
                else -> item.onSelect()
            }
            return true
        }
    }

    private companion object {
        const val ROW_WIDTH = 300
        const val ROW_TEXT_INSET = 8
        const val INDENT_WIDTH = 16
        const val ICON_WIDTH = 14
        const val EDIT_INSET = 16
        const val EDIT_HIT_WIDTH = 34
        const val SELECTED_BACKGROUND = 0x553F3F3F
        const val TEXT_COLOR = 0xFFFFFF
    }
}

data class UniverseListItem(
    val label: Component,
    val indentation: Int = 0,
    val color: Int = TEXT_COLOR,
    val selected: Boolean = false,
    val expanded: Boolean = false,
    val onSelect: () -> Unit = {},
    val onExpand: (() -> Unit)? = null,
    val onEdit: (() -> Unit)? = null,
)

private const val TEXT_COLOR = 0xFFFFFF
