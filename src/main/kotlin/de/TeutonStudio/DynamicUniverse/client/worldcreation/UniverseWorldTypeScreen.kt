package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/**
 * DynamicUniverse's selection layer over the ordinary Minecraft Create World screen.
 * The parent still owns all vanilla settings and the final Create action.
 */
class UniverseWorldTypeScreen(
    private val parent: CreateWorldScreen,
) : Screen(Component.translatable("dynamicuniverse.world_creation.title")) {
    override fun init() {
        val draft = UniverseWorldCreationDraftStore.get(parent)
        val left = width / 2 - 150
        var y = 58

        DynamicWorldType.entries.forEach { type ->
            addRenderableWidget(
                Button.builder(selectionLabel(type, draft.selectedType)) {
                    UniverseWorldCreationDraftStore.update(parent) { it.withSelectedType(type) }
                    minecraft?.setScreen(UniverseWorldTypeScreen(parent))
                }.bounds(left, y, 300, 20).build(),
            )
            y += 24
        }

        y += 12
        val customize = addRenderableWidget(
            Button.builder(Component.translatable("dynamicuniverse.world_creation.customize")) {
                when (UniverseWorldCreationDraftStore.get(parent).selectedType) {
                    DynamicWorldType.UNIVERSE -> minecraft?.setScreen(UniverseHierarchyScreen(parent, this))
                    DynamicWorldType.VANILLA -> minecraft?.setScreen(
                        WorldCreationInfoScreen(
                            this,
                            Component.translatable("dynamicuniverse.world_creation.vanilla.title"),
                            listOf(Component.translatable("dynamicuniverse.world_creation.vanilla.info")),
                        ),
                    )
                    DynamicWorldType.DIMENSION_STACK -> Unit
                }
            }.bounds(left, y, 300, 20).build(),
        )
        val stackSelected = draft.selectedType == DynamicWorldType.DIMENSION_STACK
        customize.active = !stackSelected

        if (stackSelected) {
            addRenderableWidget(
                Button.builder(Component.translatable("dynamicuniverse.world_creation.dimension_stack.why")) {
                    minecraft?.setScreen(
                        WorldCreationInfoScreen(
                            this,
                            Component.translatable("dynamicuniverse.world_creation.dimension_stack.title"),
                            listOf(
                                Component.translatable("dynamicuniverse.world_creation.dimension_stack.info.1"),
                                Component.translatable("dynamicuniverse.world_creation.dimension_stack.info.2"),
                            ),
                        ),
                    )
                }.bounds(left, y + 26, 300, 20).build(),
            )
        }

        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { onClose() }
                .bounds(left, height - 28, 300, 20)
                .build(),
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, 20, TEXT_COLOR)
        val draft = UniverseWorldCreationDraftStore.get(parent)
        guiGraphics.drawCenteredString(
            font,
            Component.translatable(draft.selectedType.descriptionKey),
            width / 2,
            38,
            SECONDARY_TEXT_COLOR,
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private fun selectionLabel(type: DynamicWorldType, selected: DynamicWorldType): Component =
        Component.translatable(if (type == selected) "dynamicuniverse.world_creation.selected" else "dynamicuniverse.world_creation.unselected", Component.translatable(type.translationKey))

    private companion object {
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
    }
}
