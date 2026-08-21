package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryMismatch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.network.chat.Component

/**
 * Scrollable editable sublist for a planet's radial dimensions.
 *
 * Boundaries are supplied by registered dimension descriptors. A red row immediately
 * below an invalid shared edge identifies a registry combination that cannot be packed.
 */
class PlanetDimensionList(
    minecraft: Minecraft,
    width: Int,
    listHeight: Int,
    top: Int,
    private val settings: () -> EditablePlanetSettings,
    private val updateSettings: (transform: (EditablePlanetSettings) -> EditablePlanetSettings) -> Unit,
) : ObjectSelectionList<PlanetDimensionList.Entry>(minecraft, width, listHeight, top, ROW_HEIGHT) {
    init {
        rebuild()
    }

    override fun getRowWidth() = ROW_WIDTH

    fun rebuild() {
        clearEntries()
        val currentSettings = settings()
        val currentPrefabIndex = PlanetPrefabRegistry.all.indexOfFirst { it.id == currentSettings.sourcePrefabId }
            .takeIf { it >= 0 } ?: 0
        addEntry(Entry(EntryContent.Value(
            label = Component.translatable("dynamicuniverse.planet_config.prefab"),
            value = PlanetPrefabRegistry.all[currentPrefabIndex].name,
            decrease = { applyPrefab(currentPrefabIndex - 1) },
            increase = { applyPrefab(currentPrefabIndex + 1) },
        )))
        addEntry(Entry(EntryContent.Value(
            label = Component.translatable("dynamicuniverse.planet_config.layers"),
            value = currentSettings.intermediateDimensionCount.toString(),
            decrease = { updateSettings { it.withIntermediateDimensionCount(it.intermediateDimensionCount - 1) } },
            increase = { updateSettings { it.withIntermediateDimensionCount(it.intermediateDimensionCount + 1) } },
        )))
        addEntry(Entry(EntryContent.Value(
            label = Component.translatable("dynamicuniverse.planet_config.transition_factor"),
            value = "1/${currentSettings.dimensionTransitionFactor}",
            decrease = { updateSettings { it.withTransitionFactor(it.dimensionTransitionFactor - 1) } },
            increase = { updateSettings { it.withTransitionFactor(it.dimensionTransitionFactor + 1) } },
        )))
        addEntry(Entry(EntryContent.Value(
            label = Component.translatable("dynamicuniverse.planet_config.core_size"),
            value = currentSettings.coreSize.toString(),
            decrease = { updateSettings { it.withCoreSize(it.coreSize - EditablePlanetSettings.CORE_SIZE_STEP) } },
            increase = { updateSettings { it.withCoreSize(it.coreSize + EditablePlanetSettings.CORE_SIZE_STEP) } },
        )))
        currentSettings.dimensions.forEachIndexed { index, dimension ->
            addEntry(Entry(EntryContent.Dimension(index, dimension)))
            currentSettings.incompatibleDimensionTransitions
                .singleOrNull { it.innerLayerIndex == index }
                ?.let { addEntry(Entry(EntryContent.Error(it, currentSettings.dimensions[index], currentSettings.dimensions[it.outerLayerIndex]))) }
        }
    }

    private fun applyPrefab(index: Int) {
        val prefabs = PlanetPrefabRegistry.all
        val normalizedIndex = Math.floorMod(index, prefabs.size)
        updateSettings { it.applyPrefab(prefabs[normalizedIndex]) }
    }

    inner class Entry(
        private val content: EntryContent,
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
            when (content) {
                is EntryContent.Value -> {
                    guiGraphics.drawString(minecraft.font, content.label, left + 4, top + 7, TEXT_COLOR, false)
                    guiGraphics.drawString(minecraft.font, "-", left + DECREASE_X, top + 7, TEXT_COLOR, false)
                    guiGraphics.drawCenteredString(minecraft.font, content.value, left + VALUE_CENTER_X, top + 7, TEXT_COLOR)
                    guiGraphics.drawString(minecraft.font, "+", left + INCREASE_X, top + 7, TEXT_COLOR, false)
                }
                is EntryContent.Dimension -> {
                    guiGraphics.drawString(minecraft.font, content.dimension.name, left + 4, top + 7, TEXT_COLOR, false)
                    guiGraphics.drawString(
                        minecraft.font,
                        Component.translatable("dynamicuniverse.planet_config.inner_boundary", content.dimension.boundaries.inner.name),
                        left + INNER_BOUNDARY_X,
                        top + 7,
                        BOUNDARY_COLOR,
                        false,
                    )
                    guiGraphics.drawString(
                        minecraft.font,
                        Component.translatable("dynamicuniverse.planet_config.outer_boundary", content.dimension.boundaries.outer.name),
                        left + OUTER_BOUNDARY_X,
                        top + 7,
                        BOUNDARY_COLOR,
                        false,
                    )
                }
                is EntryContent.Error -> guiGraphics.drawString(
                    minecraft.font,
                    Component.translatable(
                        "dynamicuniverse.planet_config.incompatible_transition",
                        content.inner.name,
                        content.outer.name,
                        content.mismatch.innerBoundary.name,
                        content.mismatch.outerBoundary.name,
                    ),
                    left + ERROR_INSET,
                    top + 7,
                    ERROR_COLOR,
                    false,
                )
            }
        }

        override fun getNarration(): Component = when (content) {
            is EntryContent.Value -> content.label.copy().append(": ${content.value}")
            is EntryContent.Dimension -> Component.literal(
                "${content.dimension.name}: ${content.dimension.boundaries.inner} / ${content.dimension.boundaries.outer}",
            )
            is EntryContent.Error -> Component.literal("Error: ${content.mismatch.message}")
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (button != 0) return false
            this@PlanetDimensionList.setSelected(this)
            when (content) {
                is EntryContent.Value -> when {
                    mouseX < contentLeft + VALUE_DECREASE_AREA_END -> content.decrease()
                    mouseX > contentLeft + VALUE_INCREASE_AREA_START -> content.increase()
                    else -> return true
                }
                is EntryContent.Dimension -> return true
                is EntryContent.Error -> return true
            }
            rebuild()
            return true
        }
    }

    sealed interface EntryContent {
        data class Value(
            val label: Component,
            val value: String,
            val decrease: () -> Unit,
            val increase: () -> Unit,
        ) : EntryContent

        data class Dimension(
            val dimensionIndex: Int,
            val dimension: EditablePlanetDimension,
        ) : EntryContent

        data class Error(
            val mismatch: DimensionBoundaryMismatch,
            val inner: EditablePlanetDimension,
            val outer: EditablePlanetDimension,
        ) : EntryContent
    }

    private val contentLeft get() = (width - ROW_WIDTH) / 2

    private companion object {
        const val ROW_WIDTH = 300
        const val ROW_HEIGHT = 22
        const val DECREASE_X = 166
        const val VALUE_CENTER_X = 204
        const val INCREASE_X = 284
        const val VALUE_DECREASE_AREA_END = 184
        const val VALUE_INCREASE_AREA_START = 266
        const val INNER_BOUNDARY_X = 108
        const val OUTER_BOUNDARY_X = 208
        const val ERROR_INSET = 18
        const val TEXT_COLOR = 0xFFFFFF
        const val BOUNDARY_COLOR = 0xB9D6FF
        const val ERROR_COLOR = 0xFF5555
    }
}
