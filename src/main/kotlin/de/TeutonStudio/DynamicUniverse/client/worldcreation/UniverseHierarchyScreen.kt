package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/**
 * A single, expandable Universe tree modelled after Vanilla's flat-world list.
 *
 * Expanding a galaxy or solar system stays on this screen. Only an actual settings
 * action opens a child screen, which returns directly to this preserved tree.
 */
class UniverseHierarchyScreen(
    private val createWorldScreen: CreateWorldScreen,
    private val parent: Screen,
) : Screen(Component.translatable("dynamicuniverse.universe_config.title")) {
    private val expandedGalaxies = mutableSetOf<Int>()
    private val expandedSolarSystems = mutableSetOf<SolarSystemAddress>()
    private var tree: UniverseVerticalList? = null

    override fun init() {
        tree = addRenderableWidget(
            UniverseVerticalList(requireNotNull(minecraft), width, height, LIST_TOP, height - FOOTER_HEIGHT, treeItems()),
        )
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { onClose() }
                .bounds(width / 2 - 150, height - 28, 300, 20)
                .build(),
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, TITLE_Y, TEXT_COLOR)
        guiGraphics.drawCenteredString(
            font,
            Component.translatable("dynamicuniverse.universe_config.tree.subtitle"),
            width / 2,
            SUBTITLE_Y,
            SECONDARY_TEXT_COLOR,
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private fun treeItems(): List<UniverseListItem> {
        val universe = UniverseWorldCreationDraftStore.get(createWorldScreen).universe
        return buildList {
            universe.galaxies.forEachIndexed { galaxyIndex, galaxy ->
                val galaxyExpanded = galaxyIndex in expandedGalaxies
                add(
                    UniverseListItem(expandLabel(galaxyExpanded, Component.translatable("dynamicuniverse.universe_config.galaxy", galaxy.name))) {
                        if (!expandedGalaxies.add(galaxyIndex)) expandedGalaxies.remove(galaxyIndex)
                        rebuildTree()
                    },
                )
                if (!galaxyExpanded) return@forEachIndexed

                galaxy.entries.forEachIndexed { entryIndex, entry ->
                    when (entry) {
                        is EditableCloud -> add(
                            UniverseListItem(
                                Component.translatable("dynamicuniverse.universe_config.cloud", entry.name),
                                GALAXY_CHILD_INDENT,
                            ) {
                                openCloudSettings(entry)
                            },
                        )
                        is EditableSolarSystem -> addSolarSystemItems(galaxyIndex, entryIndex, entry)
                    }
                }
            }
        }
    }

    private fun MutableList<UniverseListItem>.addSolarSystemItems(
        galaxyIndex: Int,
        entryIndex: Int,
        solarSystem: EditableSolarSystem,
    ) {
        val address = SolarSystemAddress(galaxyIndex, entryIndex)
        val expanded = address in expandedSolarSystems
        add(
            UniverseListItem(
                expandLabel(expanded, Component.translatable("dynamicuniverse.universe_config.solar_system", solarSystem.name)),
                GALAXY_CHILD_INDENT,
            ) {
                if (!expandedSolarSystems.add(address)) expandedSolarSystems.remove(address)
                rebuildTree()
            },
        )
        if (!expanded) return

        add(
            UniverseListItem(
                Component.translatable("dynamicuniverse.universe_config.star", solarSystem.star.name),
                SOLAR_SYSTEM_CHILD_INDENT,
            ) {
                minecraft?.setScreen(
                    WorldCreationInfoScreen(
                        this@UniverseHierarchyScreen,
                        Component.translatable("dynamicuniverse.star_config.title", solarSystem.star.name),
                        listOf(Component.translatable("dynamicuniverse.star_config.info")),
                    ),
                )
            },
        )
        solarSystem.planets.forEachIndexed { planetIndex, planet ->
            add(
                UniverseListItem(
                    Component.translatable("dynamicuniverse.universe_config.planet", planet.name),
                    SOLAR_SYSTEM_CHILD_INDENT,
                ) {
                    minecraft?.setScreen(
                        PlanetConfigurationScreen(
                            createWorldScreen,
                            this@UniverseHierarchyScreen,
                            galaxyIndex,
                            entryIndex,
                            planetIndex,
                        ),
                    )
                },
            )
        }
    }

    private fun openCloudSettings(cloud: EditableCloud) {
        minecraft?.setScreen(
            WorldCreationInfoScreen(
                this,
                Component.translatable("dynamicuniverse.cloud_config.title", cloud.name),
                listOf(Component.translatable("dynamicuniverse.cloud_config.info")),
            ),
        )
    }

    private fun rebuildTree() {
        tree?.replaceItems(treeItems())
    }

    private fun expandLabel(expanded: Boolean, label: Component): Component =
        Component.literal(if (expanded) "▼ " else "▶ ").append(label)

    private data class SolarSystemAddress(
        val galaxyIndex: Int,
        val entryIndex: Int,
    )

    private companion object {
        const val TITLE_Y = 20
        const val SUBTITLE_Y = 38
        const val LIST_TOP = 56
        const val FOOTER_HEIGHT = 40
        const val GALAXY_CHILD_INDENT = 1
        const val SOLAR_SYSTEM_CHILD_INDENT = 2
        const val TEXT_COLOR = 0xFFFFFF
        const val SECONDARY_TEXT_COLOR = 0xA0A0A0
    }
}
