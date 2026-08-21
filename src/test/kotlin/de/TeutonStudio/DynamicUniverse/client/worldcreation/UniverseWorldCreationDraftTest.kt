package de.TeutonStudio.DynamicUniverse.client.worldcreation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UniverseWorldCreationDraftTest {
    @Test
    fun `planet values stay within their supported ranges`() {
        val planet = EditablePlanet.default()

        assertEquals(4, planet.dimensionTransitionFactor)
        assertEquals(EditablePlanet.MIN_INTERMEDIATE_DIMENSIONS, planet.withIntermediateDimensionCount(-1).intermediateDimensionCount)
        assertEquals(EditablePlanet.MAX_INTERMEDIATE_DIMENSIONS, planet.withIntermediateDimensionCount(99).intermediateDimensionCount)
        assertEquals(EditablePlanet.MIN_TRANSITION_FACTOR, planet.withTransitionFactor(0).dimensionTransitionFactor)
        assertEquals(5, planet.withTransitionFactor(5).dimensionTransitionFactor)
        assertEquals(6, planet.withTransitionFactor(6).dimensionTransitionFactor)
        assertEquals(EditablePlanet.MAX_TRANSITION_FACTOR, planet.withTransitionFactor(128).dimensionTransitionFactor)
        assertEquals(EditablePlanet.MIN_CORE_SIZE, planet.withCoreSize(0).coreSize)
        assertEquals(EditablePlanet.MAX_CORE_SIZE, planet.withCoreSize(999).coreSize)
    }

    @Test
    fun `default draft contains the initial editable hierarchy`() {
        val universe = EditableUniverse.default()
        val draft = UniverseWorldCreationDraft(universe = universe)
        val galaxy = draft.universe.galaxies.single()
        val solarSystem = galaxy.entries.filterIsInstance<EditableSolarSystem>().single()

        assertEquals("Lokale Gruppe", galaxy.name)
        assertEquals("Sol", solarSystem.name)
        assertEquals("Sol", solarSystem.star.name)
        assertEquals(listOf("Sternkern", "Strahlungszone", "Korona"), solarSystem.star.dimensions.map(EditableDimension::displayName))
        assertEquals("Terra", solarSystem.planets.single().name)
        assertEquals(listOf("Planetenkern", "Innere Dimension 1", "Innere Dimension 2", "Oberfläche"), solarSystem.planets.single().dimensions.map(EditableDimension::displayName))
        assertEquals("Orion-Wolke", galaxy.entries.filterIsInstance<EditableCloud>().single().name)
    }

    @Test
    fun `draft freezes planet and star vertical stacks into the world type`() {
        val worldType = UniverseWorldCreationDraft().toWorldType()
        val system = worldType.galaxies.single().groups.first { it.kind.name == "SOLAR_SYSTEM" }
        val star = requireNotNull(system.star)
        val graph = worldType.connectionGraph()

        assertEquals(3, star.stacks.single().layersInnerToOuter.size)
        assertEquals(4, system.planets.single().stacks.single().layersInnerToOuter.size)
        assertEquals(1, graph.routesFrom(star.stacks.single().layersInnerToOuter.first().dimension).size)
        assertEquals(1, graph.routesFrom(system.planets.single().stacks.single().layersInnerToOuter.first().dimension).size)
    }

    @Test
    fun `moving a selected intermediate dimension can expose an invalid bedrock to air boundary`() {
        val planet = EditablePlanet.default()

        assertTrue(planet.dimensionValidation.isValid)
        assertFalse(planet.moveDimension(1, 1).dimensionValidation.isValid)
    }

    @Test
    fun `adding and removing only changes intermediate dimensions`() {
        val planet = EditablePlanet.default()
        val added = planet.addDimension()

        assertEquals(planet.intermediateDimensionCount + 1, added.intermediateDimensionCount)
        assertTrue(added.dimensionValidation.isValid)
        assertEquals(planet.dimensionStack.layers, added.removeDimension(added.dimensionStack.layers.lastIndex - 1).dimensionStack.layers)
    }
}
