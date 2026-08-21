package de.TeutonStudio.DynamicUniverse.client.worldcreation

import kotlin.test.Test
import kotlin.test.assertEquals

class UniverseWorldCreationDraftTest {
    @Test
    fun `planet values stay within their supported ranges`() {
        val planet = EditablePlanet.default()

        assertEquals(EditablePlanet.MIN_INTERMEDIATE_DIMENSIONS, planet.withIntermediateDimensionCount(-1).intermediateDimensionCount)
        assertEquals(EditablePlanet.MAX_INTERMEDIATE_DIMENSIONS, planet.withIntermediateDimensionCount(99).intermediateDimensionCount)
        assertEquals(EditablePlanet.MIN_TRANSITION_FACTOR, planet.withTransitionFactor(0).dimensionTransitionFactor)
        assertEquals(EditablePlanet.MAX_TRANSITION_FACTOR, planet.withTransitionFactor(128).dimensionTransitionFactor)
        assertEquals(EditablePlanet.MIN_CORE_SIZE, planet.withCoreSize(0).coreSize)
        assertEquals(EditablePlanet.MAX_CORE_SIZE, planet.withCoreSize(999).coreSize)
    }

    @Test
    fun `draft changes world type without losing universe choices`() {
        val universe = EditableUniverse.default().copy(planet = EditablePlanet.default().withCoreSize(48))
        val draft = UniverseWorldCreationDraft(universe = universe)

        val selected = draft.withSelectedType(DynamicWorldType.UNIVERSE)

        assertEquals(DynamicWorldType.UNIVERSE, selected.selectedType)
        assertEquals(48, selected.universe.planet.coreSize)
    }
}
