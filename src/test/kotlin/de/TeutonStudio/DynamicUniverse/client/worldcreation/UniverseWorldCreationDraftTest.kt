package de.TeutonStudio.DynamicUniverse.client.worldcreation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface

class UniverseWorldCreationDraftTest {
    @Test
    fun `default Terra stack is a valid core to sky sequence`() {
        val planet = EditablePlanet.default()

        assertEquals(CelestialBodyKind.PLANET, planet.bodyKind)
        assertEquals(8, planet.radialScale)
        assertEquals(
            listOf("Planetenkern", "Tiefer Nether", "Nether", "Oberwelt", "Himmel"),
            planet.dimensions.map(EditableDimension::displayName),
        )
        assertTrue(planet.dimensionValidation.isValid)
    }

    @Test
    fun `body kind selects every outward radial scale`() {
        val planet = EditablePlanet.default()

        assertEquals(4, planet.withBodyKind(CelestialBodyKind.DWARF_PLANET).radialScale)
        assertEquals(2, planet.withBodyKind(CelestialBodyKind.MOON).radialScale)
    }

    @Test
    fun `first effective edit creates a local profile and retains its provenance`() {
        val edited = EditablePlanet.default().withCoreSize(64)

        assertTrue(edited.profile.isLocal)
        assertEquals("Erde Custom", edited.profile.localProfileName)
        assertEquals("Terra · Erde Custom", edited.profileLabel)
        assertEquals("dynamicuniverse:earth", edited.profile.templateId)
    }

    @Test
    fun `moving a shell above the surface exposes forbidden air to bedrock order`() {
        val invalid = EditablePlanet.default().moveDimension(1, 2)

        assertFalse(invalid.dimensionValidation.isValid)
        assertTrue(invalid.dimensionValidation.mismatches.any(EditableBoundaryMismatch::isForbiddenAirToBedrock))
    }

    @Test
    fun `balancing row offers only a descriptor matching both adjacent boundaries`() {
        val invalid = EditablePlanet.default().copy(
            dimensionStack = EditableDimensionStack(listOf(
                EditableDimension("core", PlanetDimensionCatalog.CORE_ID),
                EditableDimension("nether", PlanetDimensionCatalog.NETHER_ID),
                EditableDimension("sky", PlanetDimensionCatalog.SKY_ID),
            )),
        )
        val mismatch = invalid.dimensionValidation.mismatches.single()

        val candidates = invalid.dimensionStack.balancingCandidates(mismatch.lowerIndex)

        assertEquals(listOf(PlanetDimensionCatalog.OVERWORLD_ID), candidates.map(RegisteredDimensionDescriptor::id))
        assertTrue(invalid.insertBalancingDimension(mismatch.lowerIndex, PlanetDimensionCatalog.OVERWORLD_ID).dimensionValidation.isValid)
    }

    @Test
    fun `discovered unresolved dimensions are selectable but block world creation`() {
        val aether = PlanetDimensionCatalog.discovered("aether:the_aether", "Aether")
        val catalog = PlanetDimensionCatalog(PlanetDimensionCatalog.standard.selectable() + aether)
        PlanetDimensionCatalogs.install(catalog)
        try {
            val selected = EditablePlanet.default().replaceDimension(EditablePlanet.default().dimensions.lastIndex, aether.id)

            assertTrue(aether.isSelectable)
            assertTrue(aether.blocksWorldCreation)
            assertEquals(DimensionCatalogStatus.DISCOVERED, catalog.require(aether.id).catalogStatus)
            assertFalse(selected.dimensionValidation.isValid)
            assertEquals(listOf(aether.id), selected.dimensionValidation.unresolvedDimensions.map(EditableDimension::descriptorId))
        } finally {
            PlanetDimensionCatalogs.resetForTests()
        }
    }

    @Test
    fun `the End is isolated and cannot become a planet layer`() {
        val end = PlanetDimensionCatalog.standard.require(PlanetDimensionCatalog.END_ID)

        assertFalse(end.isSelectable)
        assertEquals(RegisteredDimensionKind.ISOLATED, end.kind)
        assertEquals(PlanetDimensionCatalog.standard, PlanetDimensionCatalog.standard)
    }

    @Test
    fun `air to bedrock templates are rejected before they can enter a stack`() {
        assertFailsWith<IllegalArgumentException> {
            RegisteredDimensionDescriptor(
                "test:inverted",
                "Inverted",
                DimensionCatalogStatus.DISCOVERED,
                DimensionBoundaryInspection(BoundarySurface.AIR, BoundarySurface.BEDROCK, BoundaryEvidence.UNRESOLVED),
            )
        }
    }
}
