package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.network.chat.Component
import java.math.BigInteger
import java.util.Locale
import kotlin.math.pow

/** Scrollable, column-aligned dimension table modelled after Vanilla's flat-world layers. */
class DimensionStackList(
    minecraft: Minecraft,
    width: Int,
    listHeight: Int,
    top: Int,
    private val planet: () -> EditablePlanet,
    private val selectedIndex: () -> Int?,
    private val select: (Int) -> Unit,
    private val selectMismatch: (Int) -> Unit,
    private val move: (Int, Int) -> Unit,
    private val remove: (Int) -> Unit,
) : ObjectSelectionList<DimensionStackList.Entry>(minecraft, width, listHeight, top, ROW_HEIGHT) {
    private val headerY = top - 12

    init { rebuild() }

    override fun getRowWidth() = ROW_WIDTH

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.drawCenteredString(minecraft.font, "Name", width / 2 - 135, headerY, HEADER_COLOR)
        guiGraphics.drawCenteredString(minecraft.font, "Unten", width / 2 - 48, headerY, HEADER_COLOR)
        guiGraphics.drawCenteredString(minecraft.font, "Oben", width / 2 + 6, headerY, HEADER_COLOR)
        guiGraphics.drawCenteredString(minecraft.font, "Art", width / 2 + 62, headerY, HEADER_COLOR)
        guiGraphics.drawCenteredString(minecraft.font, "Fläche", width / 2 + 145, headerY, HEADER_COLOR)
        guiGraphics.drawCenteredString(minecraft.font, "Radius", width / 2 + 224, headerY, HEADER_COLOR)
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
    }

    fun rebuild() {
        clearEntries()
        val mismatchByLower = planet().dimensionValidation.mismatches.associateBy(EditableBoundaryMismatch::lowerIndex)
        planet().dimensions.forEachIndexed { index, dimension ->
            addEntry(LayerEntry(index, dimension))
            mismatchByLower[index]?.let { addEntry(MismatchEntry(it)) }
        }
    }

    sealed class Entry : ObjectSelectionList.Entry<Entry>()

    inner class LayerEntry(private val index: Int, private val dimension: EditableDimension) : Entry() {
        override fun render(guiGraphics: GuiGraphics, ignored: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            val body = planet()
            if (selectedIndex() == index) guiGraphics.fill(left, top, left + width, top + height, SELECTED_BACKGROUND)
            val color = when (dimension.status) {
                DimensionCatalogStatus.VERIFIED -> GREEN
                DimensionCatalogStatus.DISCOVERED -> ORANGE
                DimensionCatalogStatus.ISOLATED -> RED
            }
            guiGraphics.drawCenteredString(minecraft.font, dimension.displayName, left + 102, top + 6, color)
            guiGraphics.drawCenteredString(minecraft.font, dimension.innerBoundarySurface?.name ?: "—", left + 189, top + 6, TEXT_COLOR)
            guiGraphics.drawCenteredString(minecraft.font, dimension.outerBoundarySurface?.name ?: "—", left + 243, top + 6, TEXT_COLOR)
            guiGraphics.drawCenteredString(minecraft.font, roleLabel(dimension.role), left + 299, top + 6, TEXT_COLOR)
            val area = if (dimension.role == EditableDimensionRole.CORE) "—" else formatArea(DimensionStackMetrics.uniqueAreaAt(body, index))
            guiGraphics.drawCenteredString(minecraft.font, area, left + 382, top + 6, TEXT_COLOR)
            guiGraphics.drawCenteredString(minecraft.font, DimensionStackMetrics.equivalentSurfaceRadiusBlocks(body, index)?.let(::formatDistance) ?: "—", left + 461, top + 6, TEXT_COLOR)
            val isShell = dimension.role == EditableDimensionRole.SHELL
            val canMoveUp = isShell && index > 1 && body.dimensions[index - 1].role == EditableDimensionRole.SHELL
            val canMoveDown = isShell && index < body.dimensions.lastIndex - 1 && body.dimensions[index + 1].role == EditableDimensionRole.SHELL
            guiGraphics.drawString(minecraft.font, if (canMoveUp) "↑" else "·", left + 4, top + 6, if (canMoveUp) ACTION_COLOR else DISABLED_COLOR, false)
            guiGraphics.drawString(minecraft.font, if (canMoveDown) "↓" else "·", left + 20, top + 6, if (canMoveDown) ACTION_COLOR else DISABLED_COLOR, false)
            val removable = isShell || (dimension.role == EditableDimensionRole.SKY && index == body.dimensions.lastIndex)
            guiGraphics.drawString(minecraft.font, if (removable) "⌫" else "·", left + 486, top + 6, if (removable) RED else DISABLED_COLOR, false)
        }

        override fun getNarration(): Component = Component.literal("${dimension.displayName}, ${roleLabel(dimension.role)}")

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (button != 0) return false
            val localX = mouseX - (this@DimensionStackList.width / 2 - ROW_WIDTH / 2)
            val body = planet()
            val isShell = dimension.role == EditableDimensionRole.SHELL
            when {
                localX in 0.0..15.0 && isShell && index > 1 && body.dimensions[index - 1].role == EditableDimensionRole.SHELL -> move(index, -1)
                localX in 16.0..31.0 && isShell && index < body.dimensions.lastIndex - 1 && body.dimensions[index + 1].role == EditableDimensionRole.SHELL -> move(index, 1)
                localX in 480.0..510.0 && (isShell || (dimension.role == EditableDimensionRole.SKY && index == body.dimensions.lastIndex)) -> remove(index)
                else -> select(index)
            }
            rebuild()
            return true
        }
    }

    inner class MismatchEntry(private val mismatch: EditableBoundaryMismatch) : Entry() {
        override fun render(guiGraphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            val message = if (mismatch.isForbiddenAirToBedrock) "✖ Luft → Bedrock: Ausgleichsdimension nötig" else "✖ Übergang: Ausgleichsdimension nötig"
            guiGraphics.fill(left, top, left + width, top + height, ERROR_BACKGROUND)
            guiGraphics.drawString(minecraft.font, message, left + 38, top + 6, RED, false)
        }

        override fun getNarration(): Component = Component.literal("Ungültiger Dimensionsübergang")

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (button != 0) return false
            selectMismatch(mismatch.lowerIndex)
            return true
        }
    }

    private companion object {
        const val ROW_WIDTH = 520
        const val ROW_HEIGHT = 20
        const val TEXT_COLOR = 0xFFFFFF
        const val HEADER_COLOR = 0xA0A0A0
        const val ACTION_COLOR = 0xFFFFFF
        const val DISABLED_COLOR = 0x555555
        const val SELECTED_BACKGROUND = 0x553F3F3F
        const val ERROR_BACKGROUND = 0x552A0000
        const val GREEN = 0x73D673
        const val ORANGE = 0xFFB347
        const val RED = 0xFF5555

        fun roleLabel(role: EditableDimensionRole): String = when (role) {
            EditableDimensionRole.CORE -> "Kern"
            EditableDimensionRole.SHELL -> "Schale"
            EditableDimensionRole.SURFACE -> "Oberfläche"
            EditableDimensionRole.SKY -> "Himmel"
        }

        fun formatArea(value: BigInteger): String = when {
            value >= BigInteger.TEN.pow(18) -> "${decimal(value, 18)} Em²"
            value >= BigInteger.TEN.pow(12) -> "${decimal(value, 12)} Tm²"
            value >= BigInteger.TEN.pow(6) -> "${decimal(value, 6)} Mm²"
            else -> "$value m²"
        }

        fun decimal(value: BigInteger, exponent: Int): String = "%.2f".format(Locale.ROOT, value.toDouble() / 10.0.pow(exponent))
        fun formatDistance(value: Double): String = when {
            value >= 1_000_000.0 -> "%.2f MilBlock".format(Locale.ROOT, value / 1_000_000.0)
            value >= 1_000.0 -> "%.2f km".format(Locale.ROOT, value / 1_000.0)
            else -> "%.0f m".format(Locale.ROOT, value)
        }
    }
}
