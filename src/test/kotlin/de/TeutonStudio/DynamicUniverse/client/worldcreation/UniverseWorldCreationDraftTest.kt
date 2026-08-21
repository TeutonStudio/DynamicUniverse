package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryType
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertEquals("Terra", solarSystem.planets.single().name)
        assertEquals("Orion-Wolke", galaxy.entries.filterIsInstance<EditableCloud>().single().name)
    }

    @Test
    fun `each editable dimension records its inner and outer boundary`() {
        val planet = EditablePlanet.default()

        assertEquals(5, planet.dimensions.size)
        assertEquals(DimensionBoundaryType.BEDROCK, planet.dimensions.first().boundaries.outer)
        assertEquals(DimensionBoundaryType.BEDROCK, planet.dimensions.single { it.kind == EditablePlanetDimensionKind.SURFACE }.boundaries.inner)
        assertEquals(DimensionBoundaryType.AIR, planet.dimensions.single { it.kind == EditablePlanetDimensionKind.SURFACE }.boundaries.outer)
        assertTrue(planet.incompatibleDimensionTransitions.isEmpty())
    }

    @Test
    fun `editable stack reports the exact incompatible transition`() {
        val planet = EditablePlanet.default().withDimensionBoundary(0, DimensionEdge.OUTER, DimensionBoundaryType.AIR)

        val mismatch = planet.incompatibleDimensionTransitions.single()
        assertEquals(0, mismatch.innerLayerIndex)
        assertEquals(1, mismatch.outerLayerIndex)
        assertEquals(DimensionBoundaryType.AIR, mismatch.innerBoundary)
        assertEquals(DimensionBoundaryType.BEDROCK, mismatch.outerBoundary)
    }

    @Test
    fun `changing a surface edge preserves its bedrock to air boundary`() {
        val surfaceIndex = EditablePlanet.default().dimensions.indexOfFirst { it.kind == EditablePlanetDimensionKind.SURFACE }
        val planet = EditablePlanet.default().withDimensionBoundary(surfaceIndex, DimensionEdge.INNER, DimensionBoundaryType.AIR)
        val surface = planet.dimensions[surfaceIndex]

        assertEquals(DimensionBoundaryType.AIR, surface.boundaries.inner)
        assertEquals(DimensionBoundaryType.BEDROCK, surface.boundaries.outer)
    }

    @Test
    fun `changing a non surface edge keeps both of its edges alike`() {
        val planet = EditablePlanet.default().withDimensionBoundary(0, DimensionEdge.OUTER, DimensionBoundaryType.AIR)

        assertEquals(DimensionBoundaryType.AIR, planet.dimensions.first().boundaries.inner)
        assertEquals(DimensionBoundaryType.AIR, planet.dimensions.first().boundaries.outer)
    }
}
