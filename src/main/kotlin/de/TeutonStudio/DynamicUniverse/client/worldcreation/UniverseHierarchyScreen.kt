package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** A selectable cosmos tree: expanding and editing have their own compact icon targets. */
class UniverseHierarchyScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
) : Screen(Component.translatable("dynamicuniverse.universe_config.title")) {
    private val expandedGalaxies = mutableSetOf<Int>()
    private val expandedSolarSystems = mutableSetOf<SolarSystemAddress>()
    private val expandedPlanets = mutableSetOf<PlanetAddress>()
    private var selectedKey: String? = null
    private var tree: UniverseVerticalList? = null

    override fun init() {
        tree = addRenderableWidget(UniverseVerticalList(requireNotNull(minecraft), width, height - LIST_TOP - FOOTER_HEIGHT, LIST_TOP, ROW_HEIGHT, treeItems()))
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(width / 2 - 150, height - 28, 300, 20).build())
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, TITLE_Y, TEXT_COLOR)
        guiGraphics.drawCenteredString(font, Component.literal("Zeile auswählen · › aufklappen · ✎ bearbeiten"), width / 2, SUBTITLE_Y, SECONDARY_TEXT_COLOR)
    }

    override fun onClose() {
        val validation = UniverseWorldCreationDraftStore.get(createWorldScreen).validation()
        if (!validation.isValid) {
            minecraft?.setScreen(WorldCreationInfoScreen(this, Component.translatable("dynamicuniverse.universe_config.invalid.title"), listOf(Component.translatable("dynamicuniverse.universe_config.invalid.body", validation.invalidPlanets.size))))
            return
        }
        UniverseWorldCreationDraftStore.freezeWorldType(createWorldScreen)
        UniverseWorldCreationDraftStore.prepareServerCreation(createWorldScreen)
        minecraft?.setScreen(parent)
    }

    private fun treeItems(): List<UniverseListItem> {
        val universe = UniverseWorldCreationDraftStore.get(createWorldScreen).universe
        return buildList {
            universe.galaxies.forEachIndexed { galaxyIndex, galaxy ->
                val key = "galaxy:$galaxyIndex"
                val expanded = galaxyIndex in expandedGalaxies
                add(UniverseListItem(Component.literal("Galaxie · ${galaxy.name}"), selected = selectedKey == key, expanded = expanded,
                    onSelect = { select(key) }, onExpand = { toggle(expandedGalaxies, galaxyIndex) },
                    onEdit = { openInfo("Galaxie · ${galaxy.name}", "Die räumliche Beziehung zwischen Sonnensystemen und Wolken wird später definiert.") }))
                if (!expanded) return@forEachIndexed
                galaxy.entries.forEachIndexed { entryIndex, entry -> when (entry) {
                    is EditableCloud -> addCloudItem(galaxyIndex, entryIndex, entry)
                    is EditableSolarSystem -> addSolarSystemItems(galaxyIndex, entryIndex, entry)
                } }
            }
        }
    }

    private fun MutableList<UniverseListItem>.addCloudItem(galaxyIndex: Int, entryIndex: Int, cloud: EditableCloud) {
        val key = "cloud:$galaxyIndex:$entryIndex"
        add(UniverseListItem(Component.literal("Wolke · ${cloud.name}"), GALAXY_CHILD_INDENT, selected = selectedKey == key,
            onSelect = { select(key) }, onEdit = { openInfo("Wolke · ${cloud.name}", "Wolkenparameter werden in einem späteren Ausbauschritt definiert.") }))
    }

    private fun MutableList<UniverseListItem>.addSolarSystemItems(galaxyIndex: Int, entryIndex: Int, solarSystem: EditableSolarSystem) {
        val address = SolarSystemAddress(galaxyIndex, entryIndex)
        val key = "system:$galaxyIndex:$entryIndex"
        val expanded = address in expandedSolarSystems
        add(UniverseListItem(Component.literal("Sonne / Sonnensystem · ${solarSystem.star.name}"), GALAXY_CHILD_INDENT, selected = selectedKey == key, expanded = expanded,
            onSelect = { select(key) }, onExpand = { toggle(expandedSolarSystems, address) },
            onEdit = { minecraft?.setScreen(SolarSystemConfigurationScreen(createWorldScreen, this@UniverseHierarchyScreen, address)) }))
        if (!expanded) return
        solarSystem.planets.forEachIndexed { planetIndex, planet -> addPlanetItems(PlanetAddress(galaxyIndex, entryIndex, planetIndex), planet, SOLAR_SYSTEM_CHILD_INDENT) }
    }

    private fun MutableList<UniverseListItem>.addPlanetItems(address: PlanetAddress, planet: EditablePlanet, indentation: Int) {
        val key = "planet:${address.galaxyIndex}:${address.entryIndex}:${address.planetIndex}:${address.moonIndexes.joinToString(",")}"
        val hasChildren = planet.moons.isNotEmpty() || planet.nebulaRings.isNotEmpty()
        val expanded = address in expandedPlanets
        add(UniverseListItem(Component.literal("${if (address.moonIndexes.isEmpty()) "Planet" else "Mond"} · ${planet.name}"), indentation, selected = selectedKey == key, expanded = expanded,
            onSelect = { select(key) }, onExpand = if (hasChildren) ({ toggle(expandedPlanets, address) }) else null,
            onEdit = { minecraft?.setScreen(PlanetConfigurationScreen(createWorldScreen, this@UniverseHierarchyScreen, address)) }))
        if (!expanded || !hasChildren) return
        planet.moons.forEachIndexed { moonIndex, moon -> addPlanetItems(address.moon(moonIndex), moon, indentation + 1) }
        planet.nebulaRings.forEachIndexed { ringIndex, ring ->
            val ringKey = "$key:ring:$ringIndex"
            add(UniverseListItem(Component.literal("Nebelring · ${ring.name}"), indentation + 1, selected = selectedKey == ringKey,
                onSelect = { select(ringKey) }, onEdit = { openInfo("Nebelring · ${ring.name}", "Ein Nebelring ist als umlaufendes Objekt vorbereitet; Form und Simulation sind noch nicht definiert.") }))
        }
    }

    private fun openInfo(title: String, line: String) { minecraft?.setScreen(WorldCreationInfoScreen(this, Component.literal(title), listOf(Component.literal(line)))) }
    private fun select(key: String) { selectedKey = key; rebuildTree() }
    private fun <T> toggle(set: MutableSet<T>, value: T) { if (!set.add(value)) set.remove(value); rebuildTree() }
    private fun rebuildTree() { tree?.replaceItems(treeItems()) }

    data class SolarSystemAddress(val galaxyIndex: Int, val entryIndex: Int)

    private companion object {
        const val TITLE_Y = 20
        const val SUBTITLE_Y = 38
        const val LIST_TOP = 56
        const val FOOTER_HEIGHT = 40
        const val ROW_HEIGHT = 20
        const val GALAXY_CHILD_INDENT = 1
        const val SOLAR_SYSTEM_CHILD_INDENT = 2
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
    }
}
