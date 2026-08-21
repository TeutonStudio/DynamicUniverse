package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/**
 * Edits planet geometry and its ordered layer list. The core and sky are fixed endpoints;
 * only a selected intermediate layer can be moved or removed.
 */
class PlanetConfigurationScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
    private val galaxyIndex: Int,
    private val entryIndex: Int,
    private val planetIndex: Int,
) : Screen(Component.translatable("dynamicuniverse.planet_config.title")) {
    private var selectedLayerIndex: Int? = null
    private var layerList: UniverseVerticalList? = null
    private var moveUpButton: Button? = null
    private var moveDownButton: Button? = null
    private var removeButton: Button? = null

    override fun init() {
        val left = width / 2 - 150
        addValueControls(
            y = 64,
            label = "×${planet().dimensionTransitionFactor}",
            title = Component.translatable("dynamicuniverse.planet_config.transition_factor"),
            decrease = { updatePlanet { it.withTransitionFactor(it.dimensionTransitionFactor - 1) } },
            increase = { updatePlanet { it.withTransitionFactor(it.dimensionTransitionFactor + 1) } },
        )
        addValueControls(
            y = 108,
            label = planet().coreSize.toString(),
            title = Component.translatable("dynamicuniverse.planet_config.core_size"),
            decrease = { updatePlanet { it.withCoreSize(it.coreSize - EditablePlanet.CORE_SIZE_STEP) } },
            increase = { updatePlanet { it.withCoreSize(it.coreSize + EditablePlanet.CORE_SIZE_STEP) } },
        )
        addRenderableWidget(
            Button.builder(Component.translatable("dynamicuniverse.planet_config.add_dimension")) {
                updatePlanet { it.addDimension() }
                rebuildLayerList()
            }.bounds(left, 146, 300, 20).build(),
        )
        layerList = addRenderableWidget(
            UniverseVerticalList(requireNotNull(minecraft), width, height - 266, 178, 20, layerItems()),
        )
        moveUpButton = addRenderableWidget(
            Button.builder(Component.translatable("dynamicuniverse.planet_config.move_up")) {
                selectedLayerIndex?.let { index ->
                    updatePlanet { it.moveDimension(index, -1) }
                    selectedLayerIndex = index - 1
                    rebuildLayerList()
                }
            }.bounds(left, height - 80, 96, 20).build(),
        )
        moveDownButton = addRenderableWidget(
            Button.builder(Component.translatable("dynamicuniverse.planet_config.move_down")) {
                selectedLayerIndex?.let { index ->
                    updatePlanet { it.moveDimension(index, 1) }
                    selectedLayerIndex = index + 1
                    rebuildLayerList()
                }
            }.bounds(left + 102, height - 80, 96, 20).build(),
        )
        removeButton = addRenderableWidget(
            Button.builder(Component.translatable("dynamicuniverse.planet_config.remove")) {
                selectedLayerIndex?.let { index ->
                    updatePlanet { it.removeDimension(index) }
                    selectedLayerIndex = null
                    rebuildLayerList()
                }
            }.bounds(left + 204, height - 80, 96, 20).build(),
        )
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { onClose() }
                .bounds(left, height - 28, 300, 20)
                .build(),
        )
        updateActionAvailability()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, 20, TEXT_COLOR)
        guiGraphics.drawCenteredString(
            font,
            Component.translatable("dynamicuniverse.planet_config.subtitle", planet().name),
            width / 2,
            38,
            SECONDARY_TEXT_COLOR,
        )
        val validation = planet().dimensionValidation
        val validationLabel = if (validation.isValid) {
            Component.translatable("dynamicuniverse.planet_config.valid")
        } else {
            Component.translatable("dynamicuniverse.planet_config.invalid", validation.mismatches.size)
        }
        guiGraphics.drawCenteredString(font, validationLabel, width / 2, 170, if (validation.isValid) VALID_COLOR else INVALID_COLOR)
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private fun layerItems(): List<UniverseListItem> = planet().dimensionStack.layers.mapIndexed { index, dimension ->
        val prefix = if (index == selectedLayerIndex) "▶ " else "  "
        val inner = dimension.innerBoundarySurface?.name?.lowercase() ?: "—"
        val outer = dimension.outerBoundarySurface?.name?.lowercase() ?: "—"
        UniverseListItem(Component.literal("$prefix${dimension.displayName}  [$inner → $outer]"), onSelect = {
            selectedLayerIndex = index
            rebuildLayerList()
        })
    }

    private fun rebuildLayerList() {
        layerList?.replaceItems(layerItems())
        updateActionAvailability()
    }

    private fun updateActionAvailability() {
        val selected = selectedLayerIndex?.let { planet().dimensionStack.layers.getOrNull(it) }
        val editable = selected?.role == EditableDimensionRole.INNER
        val index = selectedLayerIndex
        moveUpButton?.active = editable && index != null && index > 1
        moveDownButton?.active = editable && index != null && index < planet().dimensionStack.layers.lastIndex - 1
        removeButton?.active = editable
    }

    private fun addValueControls(
        y: Int,
        title: Component,
        label: String,
        decrease: () -> Unit,
        increase: () -> Unit,
    ) {
        val left = width / 2 - 150
        addRenderableWidget(Button.builder(Component.literal("-")) { decrease(); refresh() }.bounds(left, y, 40, 20).build())
        addRenderableWidget(Button.builder(Component.literal(label)) { }.bounds(left + 44, y, 212, 20).build()).active = false
        addRenderableWidget(Button.builder(Component.literal("+")) { increase(); refresh() }.bounds(left + 260, y, 40, 20).build())
        addRenderableWidget(Button.builder(title) { }.bounds(left, y - 22, 300, 20).build()).active = false
    }

    private fun refresh() {
        minecraft?.setScreen(PlanetConfigurationScreen(createWorldScreen, parent, galaxyIndex, entryIndex, planetIndex))
    }

    private fun planet(): EditablePlanet = solarSystem().planets[planetIndex]

    private fun solarSystem(): EditableSolarSystem =
        UniverseWorldCreationDraftStore.get(createWorldScreen).universe.galaxies[galaxyIndex].entries[entryIndex] as EditableSolarSystem

    private fun updatePlanet(transform: (EditablePlanet) -> EditablePlanet) {
        UniverseWorldCreationDraftStore.update(createWorldScreen) { draft ->
            val galaxy = draft.universe.galaxies[galaxyIndex]
            val solarSystem = galaxy.entries[entryIndex] as EditableSolarSystem
            val updatedSolarSystem = solarSystem.copy(
                planets = solarSystem.planets.mapIndexed { index, planet -> if (index == planetIndex) transform(planet) else planet },
            )
            val updatedGalaxy = galaxy.copy(
                entries = galaxy.entries.mapIndexed { index, entry -> if (index == entryIndex) updatedSolarSystem else entry },
            )
            draft.copy(universe = draft.universe.copy(galaxies = draft.universe.galaxies.mapIndexed { index, candidate -> if (index == galaxyIndex) updatedGalaxy else candidate }))
        }
    }

    private companion object {
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
        const val VALID_COLOR = 0x73D673
        const val INVALID_COLOR = 0xFF6B6B
    }
}
