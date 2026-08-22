package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Edits only the declared inter-orbit distances; orbital mechanics stay explicitly out of alpha0. */
class SolarSystemConfigurationScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
    private val address: UniverseHierarchyScreen.SolarSystemAddress,
) : Screen(Component.literal("Sonnensystem konfigurieren")) {
    override fun init() {
        val left = width / 2 - 150
        system().planets.indices.forEach { orbitIndex ->
            val from = if (orbitIndex == 0) system().star.name else system().planets[orbitIndex - 1].name
            val to = system().planets[orbitIndex].name
            val y = 76 + orbitIndex * ROW_GAP
            addRenderableWidget(Button.builder(Component.literal("-")) { updateOrbit(orbitIndex, distanceAt(orbitIndex) - 1); refresh() }.bounds(left, y, 40, 20).build())
            addRenderableWidget(Button.builder(Component.literal("$from → $to: ${distanceAt(orbitIndex)} MilBlock")) { }.bounds(left + 44, y, 212, 20).build()).active = false
            addRenderableWidget(Button.builder(Component.literal("+")) { updateOrbit(orbitIndex, distanceAt(orbitIndex) + 1); refresh() }.bounds(left + 260, y, 40, 20).build())
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK) { onClose() }.bounds(left, height - 28, 300, 20).build())
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, 22, TEXT_COLOR)
        guiGraphics.drawCenteredString(font, Component.literal("Abstände sind veränderliche Vorgaben; Tageslänge und Bahnmechanik folgen später."), width / 2, 42, SECONDARY_TEXT_COLOR)
    }

    override fun onClose() { minecraft?.setScreen(parent) }

    private fun system(): EditableSolarSystem = UniverseWorldCreationDraftStore.get(createWorldScreen).universe.galaxies[address.galaxyIndex].entries[address.entryIndex] as EditableSolarSystem
    private fun distanceAt(index: Int): Long = if (index == 0) system().firstPlanetDistanceMilBlocks else system().planetToPlanetDistancesMilBlocks[index - 1]
    private fun updateOrbit(index: Int, distance: Long) {
        UniverseWorldCreationDraftStore.update(createWorldScreen) { draft ->
            val galaxy = draft.universe.galaxies[address.galaxyIndex]
            val updatedGalaxy = galaxy.copy(entries = galaxy.entries.mapIndexed { entryIndex, entry ->
                if (entryIndex == address.entryIndex) (entry as EditableSolarSystem).withOrbitDistance(index, distance) else entry
            })
            draft.copy(universe = draft.universe.copy(galaxies = draft.universe.galaxies.mapIndexed { galaxyIndex, candidate -> if (galaxyIndex == address.galaxyIndex) updatedGalaxy else candidate }))
        }
    }

    private fun refresh() { minecraft?.setScreen(SolarSystemConfigurationScreen(createWorldScreen, parent, address)) }

    private companion object {
        const val ROW_GAP = 28
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
    }
}
