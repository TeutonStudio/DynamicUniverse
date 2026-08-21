package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Edits a planet's dimensions and the edges that connect them. */
class PlanetConfigurationScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
    private val galaxyIndex: Int,
    private val entryIndex: Int,
    private val planetIndex: Int,
) : Screen(Component.translatable("dynamicuniverse.planet_config.title")) {
    override fun init() {
        addRenderableWidget(
            PlanetDimensionList(
                requireNotNull(minecraft),
                width,
                height - LIST_TOP - FOOTER_HEIGHT,
                LIST_TOP,
                ::planet,
                ::updatePlanet,
            ),
        )
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(CommonComponents.GUI_DONE) { onClose() }
            .bounds(width / 2 - 150, height - 28, 300, 20).build())
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
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private fun planet(): EditablePlanet = solarSystem().planets[planetIndex]

    private fun solarSystem(): EditableSolarSystem =
        UniverseWorldCreationDraftStore.get(createWorldScreen).universe.galaxies[galaxyIndex].entries[entryIndex] as EditableSolarSystem

    private fun updatePlanet(transform: (EditablePlanet) -> EditablePlanet) {
        UniverseWorldCreationDraftStore.update(createWorldScreen) { draft ->
            val galaxy = draft.universe.galaxies[galaxyIndex]
            val solarSystem = galaxy.entries[entryIndex] as EditableSolarSystem
            val updatedSolarSystem = solarSystem.copy(
                planets = solarSystem.planets.mapIndexed { index, planet ->
                    if (index == planetIndex) transform(planet) else planet
                },
            )
            val updatedGalaxy = galaxy.copy(
                entries = galaxy.entries.mapIndexed { index, entry ->
                    if (index == entryIndex) updatedSolarSystem else entry
                },
            )
            draft.copy(
                universe = draft.universe.copy(
                    galaxies = draft.universe.galaxies.mapIndexed { index, candidate ->
                        if (index == galaxyIndex) updatedGalaxy else candidate
                    },
                ),
            )
        }
    }

    private companion object {
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
        const val LIST_TOP = 56
        const val FOOTER_HEIGHT = 40
    }
}
