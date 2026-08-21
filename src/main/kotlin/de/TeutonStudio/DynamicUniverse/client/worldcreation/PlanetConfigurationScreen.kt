package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Edits one body's local profile and ordered dimension-template selection. */
class PlanetConfigurationScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
    private val galaxyIndex: Int,
    private val entryIndex: Int,
    private val planetIndex: Int,
) : Screen(Component.translatable("dynamicuniverse.planet_config.title")) {
    private var selectedLayerIndex: Int? = null
    private var selectedMismatchLowerIndex: Int? = null
    private var layerList: UniverseVerticalList? = null
    private var replacePreviousButton: Button? = null
    private var replaceNextButton: Button? = null
    private var insertBalanceButton: Button? = null
    private var moveUpButton: Button? = null
    private var moveDownButton: Button? = null
    private var removeButton: Button? = null

    override fun init() {
        val left = width / 2 - 150
        addValueControls(
            y = 64,
            label = bodyKindLabel(planet().bodyKind),
            title = Component.translatable("dynamicuniverse.planet_config.body_kind"),
            decrease = { updatePlanet { it.withBodyKind(previousBodyKind(it.bodyKind)) } },
            increase = { updatePlanet { it.withBodyKind(nextBodyKind(it.bodyKind)) } },
        )
        addValueControls(
            y = 108,
            label = planet().coreSize.toString(),
            title = Component.translatable("dynamicuniverse.planet_config.core_size"),
            decrease = { updatePlanet { it.withCoreSize(it.coreSize - EditablePlanet.CORE_SIZE_STEP) } },
            increase = { updatePlanet { it.withCoreSize(it.coreSize + EditablePlanet.CORE_SIZE_STEP) } },
        )
        addRenderableWidget(Button.builder(Component.translatable("dynamicuniverse.planet_config.add_dimension")) {
            updatePlanet { it.addDimension() }
            rebuildLayerList()
        }.bounds(left, 146, 300, 20).build())
        layerList = addRenderableWidget(UniverseVerticalList(requireNotNull(minecraft), width, height - 316, 178, 20, layerItems()))
        replacePreviousButton = addRenderableWidget(Button.builder(Component.literal("< Vorlage")) { cycleSelection(-1) }.bounds(left, height - 130, 96, 20).build())
        replaceNextButton = addRenderableWidget(Button.builder(Component.literal("Vorlage >")) { cycleSelection(1) }.bounds(left + 102, height - 130, 96, 20).build())
        insertBalanceButton = addRenderableWidget(Button.builder(Component.translatable("dynamicuniverse.planet_config.insert_balance")) { insertBalance() }.bounds(left + 204, height - 130, 96, 20).build())
        moveUpButton = addRenderableWidget(Button.builder(Component.translatable("dynamicuniverse.planet_config.move_up")) { moveSelected(-1) }.bounds(left, height - 104, 96, 20).build())
        moveDownButton = addRenderableWidget(Button.builder(Component.translatable("dynamicuniverse.planet_config.move_down")) { moveSelected(1) }.bounds(left + 102, height - 104, 96, 20).build())
        removeButton = addRenderableWidget(Button.builder(Component.translatable("dynamicuniverse.planet_config.remove")) { removeSelected() }.bounds(left + 204, height - 104, 96, 20).build())
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(left, height - 28, 300, 20).build())
        updateActionAvailability()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, 20, TEXT_COLOR)
        guiGraphics.drawCenteredString(font, Component.literal(planet().profileLabel), width / 2, 38, SECONDARY_TEXT_COLOR)
        val validation = planet().dimensionValidation
        val label = if (validation.isValid) Component.translatable("dynamicuniverse.planet_config.valid") else Component.translatable("dynamicuniverse.planet_config.invalid", validation.mismatches.size + validation.unresolvedDimensions.size + validation.shapeErrors.size)
        guiGraphics.drawCenteredString(font, label, width / 2, 170, if (validation.isValid) VALID_COLOR else INVALID_COLOR)
    }

    override fun onClose() { minecraft?.setScreen(parent) }

    private fun layerItems(): List<UniverseListItem> = buildList {
        val stack = planet().dimensionStack
        val mismatchByLower = planet().dimensionValidation.mismatches.associateBy(EditableBoundaryMismatch::lowerIndex)
        stack.layers.forEachIndexed { index, dimension ->
            val prefix = if (index == selectedLayerIndex) "▶ " else "  "
            val lower = dimension.innerBoundarySurface?.name?.lowercase() ?: "—"
            val upper = dimension.outerBoundarySurface?.name?.lowercase() ?: "—"
            val color = when (dimension.status) {
                DimensionCatalogStatus.VERIFIED -> GREEN
                DimensionCatalogStatus.DISCOVERED -> ORANGE
                DimensionCatalogStatus.ISOLATED -> RED
            }
            add(UniverseListItem(Component.literal("$prefix${dimension.displayName} · ${dimension.role} [$lower → $upper]"), color = color) {
                selectedLayerIndex = index
                selectedMismatchLowerIndex = null
                rebuildLayerList()
            })
            mismatchByLower[index]?.let { mismatch ->
                val message = if (mismatch.isForbiddenAirToBedrock) "Fehler: Luft → Bedrock ist verboten. Ausgleichsdimension einfügen." else "Fehler: Ausgleichsdimension einfügen."
                add(UniverseListItem(Component.literal("  ✖ $message"), color = RED, onSelect = {
                    selectedMismatchLowerIndex = mismatch.lowerIndex
                    selectedLayerIndex = null
                    rebuildLayerList()
                }))
            }
        }
    }

    private fun cycleSelection(delta: Int) {
        val index = selectedLayerIndex ?: return
        val candidates = PlanetDimensionCatalogs.current.selectableFor(index, planet().dimensionStack.layers.size)
        val current = planet().dimensions[index].descriptorId
        val currentIndex = candidates.indexOfFirst { it.id == current }
        if (candidates.isEmpty()) return
        val next = candidates[Math.floorMod(currentIndex + delta, candidates.size)]
        updatePlanet { it.replaceDimension(index, next.id) }
        rebuildLayerList()
    }

    private fun insertBalance() {
        val lowerIndex = selectedMismatchLowerIndex ?: return
        val candidate = planet().dimensionStack.balancingCandidates(lowerIndex).firstOrNull() ?: return
        updatePlanet { it.insertBalancingDimension(lowerIndex, candidate.id) }
        selectedMismatchLowerIndex = null
        rebuildLayerList()
    }

    private fun moveSelected(delta: Int) {
        selectedLayerIndex?.let { index ->
            updatePlanet { it.moveDimension(index, delta) }
            selectedLayerIndex = (index + delta).coerceIn(1, planet().dimensionStack.layers.lastIndex - 1)
            rebuildLayerList()
        }
    }

    private fun removeSelected() {
        selectedLayerIndex?.let { index ->
            updatePlanet { it.removeDimension(index) }
            selectedLayerIndex = null
            rebuildLayerList()
        }
    }

    private fun rebuildLayerList() { layerList?.replaceItems(layerItems()); updateActionAvailability() }

    private fun updateActionAvailability() {
        val index = selectedLayerIndex
        val inner = index != null && index in 1 until planet().dimensionStack.layers.lastIndex
        replacePreviousButton?.active = index != null
        replaceNextButton?.active = index != null
        insertBalanceButton?.active = selectedMismatchLowerIndex?.let { planet().dimensionStack.balancingCandidates(it).isNotEmpty() } == true
        moveUpButton?.active = inner && index > 1
        moveDownButton?.active = inner && index < planet().dimensionStack.layers.lastIndex - 1
        removeButton?.active = inner
    }

    private fun addValueControls(y: Int, title: Component, label: String, decrease: () -> Unit, increase: () -> Unit) {
        val left = width / 2 - 150
        addRenderableWidget(Button.builder(Component.literal("-")) { decrease(); refresh() }.bounds(left, y, 40, 20).build())
        addRenderableWidget(Button.builder(Component.literal(label)) { }.bounds(left + 44, y, 212, 20).build()).active = false
        addRenderableWidget(Button.builder(Component.literal("+")) { increase(); refresh() }.bounds(left + 260, y, 40, 20).build())
        addRenderableWidget(Button.builder(title) { }.bounds(left, y - 22, 300, 20).build()).active = false
    }

    private fun refresh() { minecraft?.setScreen(PlanetConfigurationScreen(createWorldScreen, parent, galaxyIndex, entryIndex, planetIndex)) }
    private fun planet(): EditablePlanet = solarSystem().planets[planetIndex]
    private fun solarSystem(): EditableSolarSystem = UniverseWorldCreationDraftStore.get(createWorldScreen).universe.galaxies[galaxyIndex].entries[entryIndex] as EditableSolarSystem

    private fun updatePlanet(transform: (EditablePlanet) -> EditablePlanet) {
        UniverseWorldCreationDraftStore.update(createWorldScreen) { draft ->
            val galaxy = draft.universe.galaxies[galaxyIndex]
            val system = galaxy.entries[entryIndex] as EditableSolarSystem
            val updatedSystem = system.copy(planets = system.planets.mapIndexed { index, candidate -> if (index == planetIndex) transform(candidate) else candidate })
            val updatedGalaxy = galaxy.copy(entries = galaxy.entries.mapIndexed { index, entry -> if (index == entryIndex) updatedSystem else entry })
            draft.copy(universe = draft.universe.copy(galaxies = draft.universe.galaxies.mapIndexed { index, candidate -> if (index == galaxyIndex) updatedGalaxy else candidate }))
        }
    }

    private fun nextBodyKind(kind: CelestialBodyKind): CelestialBodyKind = CelestialBodyKind.entries[(kind.ordinal + 1) % CelestialBodyKind.entries.size]
    private fun previousBodyKind(kind: CelestialBodyKind): CelestialBodyKind = CelestialBodyKind.entries[Math.floorMod(kind.ordinal - 1, CelestialBodyKind.entries.size)]
    private fun bodyKindLabel(kind: CelestialBodyKind): String = "${kind.name.lowercase().replace('_', ' ')} ×${kind.radialScale}"

    private companion object {
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
        const val VALID_COLOR = 0x73D673
        const val INVALID_COLOR = 0xFF6B6B
        const val GREEN = 0x73D673
        const val ORANGE = 0xFFB347
        const val RED = 0xFF5555
    }
}
