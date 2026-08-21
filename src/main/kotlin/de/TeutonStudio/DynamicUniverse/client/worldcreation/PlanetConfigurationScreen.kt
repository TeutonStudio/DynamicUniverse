package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Identifies either a planet's settings or one of its direct moons' settings. */
data class PlanetSettingsAddress(
    val galaxyIndex: Int,
    val entryIndex: Int,
    val planetIndex: Int,
    val moonIndex: Int? = null,
)

/** Edits the shared topology settings of a planet or moon. */
class PlanetConfigurationScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
    private val address: PlanetSettingsAddress,
) : Screen(Component.translatable("dynamicuniverse.planet_config.title")) {
    override fun init() {
        addRenderableWidget(
            PlanetDimensionList(
                requireNotNull(minecraft), width, height - LIST_TOP - FOOTER_HEIGHT, LIST_TOP,
                ::settings, ::updateSettings,
            ),
        )
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { onClose() }
                .bounds(width / 2 - 150, height - 28, 300, 20).build(),
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, 20, TEXT_COLOR)
        guiGraphics.drawCenteredString(
            font, Component.translatable("dynamicuniverse.planet_config.subtitle", bodyName()),
            width / 2, 38, SECONDARY_TEXT_COLOR,
        )
    }

    override fun onClose() { minecraft?.setScreen(parent) }

    private fun bodyName(): String = address.moonIndex?.let { planet().moons[it].name } ?: planet().name
    private fun settings(): EditablePlanetSettings = address.moonIndex?.let { planet().moons[it].settings } ?: planet().settings

    private fun planet(): EditablePlanet = solarSystem().planets[address.planetIndex]
    private fun solarSystem(): EditableSolarSystem =
        UniverseWorldCreationDraftStore.get(createWorldScreen).universe.galaxies[address.galaxyIndex].entries[address.entryIndex] as EditableSolarSystem

    private fun updateSettings(transform: (EditablePlanetSettings) -> EditablePlanetSettings) {
        UniverseWorldCreationDraftStore.update(createWorldScreen) { draft ->
            val galaxy = draft.universe.galaxies[address.galaxyIndex]
            val solarSystem = galaxy.entries[address.entryIndex] as EditableSolarSystem
            val updatedSolarSystem = solarSystem.copy(planets = solarSystem.planets.mapIndexed { planetIndex, planet ->
                if (planetIndex != address.planetIndex) planet else updatePlanetSettings(planet, transform)
            })
            val updatedGalaxy = galaxy.copy(entries = galaxy.entries.mapIndexed { entryIndex, entry ->
                if (entryIndex == address.entryIndex) updatedSolarSystem else entry
            })
            draft.copy(universe = draft.universe.copy(galaxies = draft.universe.galaxies.mapIndexed { galaxyIndex, candidate ->
                if (galaxyIndex == address.galaxyIndex) updatedGalaxy else candidate
            }))
        }
    }

    private fun updatePlanetSettings(
        planet: EditablePlanet,
        transform: (EditablePlanetSettings) -> EditablePlanetSettings,
    ): EditablePlanet = address.moonIndex?.let { moonIndex ->
        planet.copy(moons = planet.moons.mapIndexed { index, moon ->
            if (index == moonIndex) moon.copy(settings = transform(moon.settings)) else moon
        })
    } ?: planet.copy(settings = transform(planet.settings))

    private companion object {
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
        const val LIST_TOP = 56
        const val FOOTER_HEIGHT = 40
    }
}
