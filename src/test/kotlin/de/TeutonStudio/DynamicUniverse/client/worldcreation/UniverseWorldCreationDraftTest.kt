package de.TeutonStudio.DynamicUniverse.client.worldcreation

import kotlin.test.Test
import kotlin.test.assertEquals

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
        assertEquals("Terra", solarSystem.planets.single().name)
        assertEquals("Orion-Wolke", galaxy.entries.filterIsInstance<EditableCloud>().single().name)
    }
}
