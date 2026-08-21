package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class UniverseWorldCreationDraftTest {
    @Test
    fun `planet settings stay within their supported ranges`() {
        val settings = EditablePlanetSettings.default()

        assertEquals(4, settings.dimensionTransitionFactor)
        assertEquals(EditablePlanetSettings.MIN_INTERMEDIATE_DIMENSIONS, settings.withIntermediateDimensionCount(-1).intermediateDimensionCount)
        assertEquals(EditablePlanetSettings.MAX_INTERMEDIATE_DIMENSIONS, settings.withIntermediateDimensionCount(99).intermediateDimensionCount)
        assertEquals(EditablePlanetSettings.MIN_TRANSITION_FACTOR, settings.withTransitionFactor(0).dimensionTransitionFactor)
        assertEquals(5, settings.withTransitionFactor(5).dimensionTransitionFactor)
        assertEquals(EditablePlanetSettings.MAX_TRANSITION_FACTOR, settings.withTransitionFactor(128).dimensionTransitionFactor)
        assertEquals(EditablePlanetSettings.MIN_CORE_SIZE, settings.withCoreSize(0).coreSize)
        assertEquals(EditablePlanetSettings.MAX_CORE_SIZE, settings.withCoreSize(999).coreSize)
    }

    @Test
    fun `default draft contains planets with direct moon subobjects`() {
        val universe = EditableUniverse.default()
        val galaxy = universe.galaxies.single()
        val solarSystem = galaxy.entries.filterIsInstance<EditableSolarSystem>().single()
        val planet = solarSystem.planets.single()

        assertEquals("Lokale Gruppe", galaxy.name)
        assertEquals("Sol", solarSystem.name)
        assertEquals("Sol", solarSystem.star.name)
        assertEquals("Terra", planet.name)
        assertEquals("Luna", planet.moons.single().name)
        assertEquals(PlanetPrefabRegistry.MOON_ID, planet.moons.single().settings.sourcePrefabId)
        assertEquals("Orion-Wolke", galaxy.entries.filterIsInstance<EditableCloud>().single().name)
    }

    @Test
    fun `dimensions use registered descriptor IDs and retain compatible boundaries`() {
        val settings = EditablePlanetSettings.default()

        assertEquals(5, settings.dimensions.size)
        assertEquals(PlanetDimensionRegistry.CORE_ID, settings.dimensions.first().descriptorId)
        assertEquals(PlanetDimensionRegistry.SURFACE_ID, settings.dimensions.single { it.kind == EditablePlanetDimensionKind.SURFACE }.descriptorId)
        assertEquals(DimensionBoundaryType.BEDROCK, settings.dimensions.single { it.kind == EditablePlanetDimensionKind.SURFACE }.boundaries.inner)
        assertEquals(DimensionBoundaryType.AIR, settings.dimensions.single { it.kind == EditablePlanetDimensionKind.SURFACE }.boundaries.outer)
        assertTrue(settings.incompatibleDimensionTransitions.isEmpty())
    }

    @Test
    fun `applying a prefab creates independent editable settings`() {
        val earth = PlanetPrefabRegistry.require(PlanetPrefabRegistry.EARTH_ID)
        val firstPlanetSettings = EditablePlanetSettings.default().applyPrefab(earth)
        val secondPlanetSettings = EditablePlanetSettings.default().applyPrefab(earth)
        val editedFirstPlanetSettings = firstPlanetSettings.withCoreSize(64)

        assertEquals(PlanetPrefabRegistry.EARTH_ID, firstPlanetSettings.sourcePrefabId)
        assertEquals(32, secondPlanetSettings.coreSize)
        assertEquals(64, editedFirstPlanetSettings.coreSize)
        assertEquals(PlanetPrefabRegistry.EARTH_ID, editedFirstPlanetSettings.sourcePrefabId)
        assertNotSame(firstPlanetSettings.dimensions, secondPlanetSettings.dimensions)
        assertEquals(32, earth.coreSize)
    }

    @Test
    fun `prefab application replaces the complete topology in its defined order`() {
        val moonSettings = EditablePlanetSettings.default().applyPrefab(PlanetPrefabRegistry.require(PlanetPrefabRegistry.MOON_ID))

        assertEquals(PlanetPrefabRegistry.MOON_ID, moonSettings.sourcePrefabId)
        assertEquals(3, moonSettings.dimensions.size)
        assertEquals(listOf(PlanetDimensionRegistry.CORE_ID, PlanetDimensionRegistry.SURFACE_ID, PlanetDimensionRegistry.SKY_ID), moonSettings.dimensions.map { it.descriptorId })
    }
}
