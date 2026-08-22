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
    private val address: PlanetAddress,
) : Screen(Component.translatable("dynamicuniverse.planet_config.title")) {
    private var selectedLayerIndex: Int? = null
    private var selectedMismatchLowerIndex: Int? = null
    private var layerList: DimensionStackList? = null
    private var replacePreviousButton: Button? = null
    private var replaceNextButton: Button? = null
    private var insertBalanceButton: Button? = null
    private val hasParentOrbit: Boolean get() = address.moonIndexes.isNotEmpty()
    private val addDimensionY: Int get() = if (hasParentOrbit) 234 else 190
    private val tableTop: Int get() = addDimensionY + 42

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
        addValueControls(
            y = 152,
            label = planet().transitionFactor.toString(),
            title = Component.literal("Übergangsfaktor nach außen"),
            decrease = { updatePlanet { it.withTransitionFactor(it.transitionFactor - 1) } },
            increase = { updatePlanet { it.withTransitionFactor(it.transitionFactor + 1) } },
        )
        if (hasParentOrbit) addValueControls(
            y = 196,
            label = "${planet().parentOrbitDistanceMilBlocks} MilBlock",
            title = Component.literal("Abstand zum Elternkörper"),
            decrease = { updatePlanet { it.withParentOrbitDistance(it.parentOrbitDistanceMilBlocks - 1) } },
            increase = { updatePlanet { it.withParentOrbitDistance(it.parentOrbitDistanceMilBlocks + 1) } },
        )
        addRenderableWidget(Button.builder(Component.translatable("dynamicuniverse.planet_config.add_dimension")) {
            updatePlanet { it.addDimension() }
            rebuildLayerList()
        }.bounds(left, addDimensionY, 300, 20).build())
        layerList = addRenderableWidget(DimensionStackList(
            requireNotNull(minecraft), width, height - 350 + (tableTop - 232), tableTop,
            ::planet, { selectedLayerIndex }, { index -> selectedLayerIndex = index; selectedMismatchLowerIndex = null; updateActionAvailability() },
            { lowerIndex -> selectedMismatchLowerIndex = lowerIndex; selectedLayerIndex = null; updateActionAvailability() },
            { index, delta -> updatePlanet { it.moveDimension(index, delta) }; selectedLayerIndex = (index + delta).coerceAtLeast(1) },
            { index -> updatePlanet { it.removeDimension(index) }; selectedLayerIndex = null },
        ))
        replacePreviousButton = addRenderableWidget(Button.builder(Component.literal("< Vorlage")) { cycleSelection(-1) }.bounds(left, height - 130, 96, 20).build())
        replaceNextButton = addRenderableWidget(Button.builder(Component.literal("Vorlage >")) { cycleSelection(1) }.bounds(left + 102, height - 130, 96, 20).build())
        insertBalanceButton = addRenderableWidget(Button.builder(Component.translatable("dynamicuniverse.planet_config.insert_balance")) { insertBalance() }.bounds(left + 204, height - 130, 96, 20).build())
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(left, height - 28, 300, 20).build())
        updateActionAvailability()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, 20, TEXT_COLOR)
        guiGraphics.drawCenteredString(font, Component.literal(planet().profileLabel), width / 2, 38, SECONDARY_TEXT_COLOR)
        val validation = planet().dimensionValidation
        val label = if (validation.isValid) Component.translatable("dynamicuniverse.planet_config.valid") else Component.translatable("dynamicuniverse.planet_config.invalid", validation.mismatches.size + validation.unresolvedDimensions.size + validation.shapeErrors.size)
        guiGraphics.drawCenteredString(font, label, width / 2, tableTop - 18, if (validation.isValid) VALID_COLOR else INVALID_COLOR)
    }

    override fun onClose() { minecraft?.setScreen(parent) }

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

    private fun rebuildLayerList() { layerList?.rebuild(); updateActionAvailability() }

    private fun updateActionAvailability() {
        val index = selectedLayerIndex
        replacePreviousButton?.active = index != null
        replaceNextButton?.active = index != null
        insertBalanceButton?.active = selectedMismatchLowerIndex?.let { planet().dimensionStack.balancingCandidates(it).isNotEmpty() } == true
    }

    private fun addValueControls(y: Int, title: Component, label: String, decrease: () -> Unit, increase: () -> Unit) {
        val left = width / 2 - 150
        addRenderableWidget(Button.builder(Component.literal("-")) { decrease(); refresh() }.bounds(left, y, 40, 20).build())
        addRenderableWidget(Button.builder(Component.literal(label)) { }.bounds(left + 44, y, 212, 20).build()).active = false
        addRenderableWidget(Button.builder(Component.literal("+")) { increase(); refresh() }.bounds(left + 260, y, 40, 20).build())
        addRenderableWidget(Button.builder(title) { }.bounds(left, y - 22, 300, 20).build()).active = false
    }

    private fun refresh() { minecraft?.setScreen(PlanetConfigurationScreen(createWorldScreen, parent, address)) }
    private fun planet(): EditablePlanet = UniverseWorldCreationDraftStore.get(createWorldScreen).planetAt(address)

    private fun updatePlanet(transform: (EditablePlanet) -> EditablePlanet) {
        UniverseWorldCreationDraftStore.update(createWorldScreen) { it.updatePlanet(address, transform) }
    }

    private fun nextBodyKind(kind: CelestialBodyKind): CelestialBodyKind = CelestialBodyKind.entries[(kind.ordinal + 1) % CelestialBodyKind.entries.size]
    private fun previousBodyKind(kind: CelestialBodyKind): CelestialBodyKind = CelestialBodyKind.entries[Math.floorMod(kind.ordinal - 1, CelestialBodyKind.entries.size)]
    private fun bodyKindLabel(kind: CelestialBodyKind): String = "${kind.name.lowercase().replace('_', ' ')} ×${kind.radialScale}"

    private companion object {
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
        const val VALID_COLOR = 0x73D673
        const val INVALID_COLOR = 0xFF6B6B
    }
}
