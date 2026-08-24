package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.client.worldcreation.UniverseWorldCreationDraft
import de.TeutonStudio.DynamicUniverse.client.worldcreation.toWorldType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UniverseGeometryManifestTest {
    @Test
    fun `compiler assigns a toroidal period to every generated layer`() {
        val manifest = UniverseGeometryCompiler.compile(UniverseWorldCreationDraft().toWorldType())

        assertEquals(8, manifest.layers.size)
        assertEquals(32L, manifest.layers.first { it.dimension.value.endsWith("planet/0/0/0/core") }.period.blocks)
        val nether = manifest.layers.first { it.dimension.value == "minecraft:the_nether" }
        assertEquals(32L * 8L, nether.period.blocks)
        assertEquals(nether.period.blocks.toDouble() / (2.0 * Math.PI), nether.projectionRadiusBlocks)
        assertEquals(2, manifest.airBuffers.size)
        assertTrue(manifest.airBuffers.all { it.lowerBlocks == 5 && it.upperBlocks == 5 })
    }

    @Test
    fun `core edge is the exact configured block count while topology remains chunk aligned`() {
        val draft = UniverseWorldCreationDraft().copy(
            universe = UniverseWorldCreationDraft().universe.copy(
                galaxies = UniverseWorldCreationDraft().universe.galaxies.map { galaxy ->
                    galaxy.copy(entries = galaxy.entries.map { entry ->
                        if (entry is de.TeutonStudio.DynamicUniverse.client.worldcreation.EditableSolarSystem) {
                            entry.copy(planets = entry.planets.map { it.withCoreSize(17) })
                        } else entry
                    })
                },
            ),
        )

        val manifest = UniverseGeometryCompiler.compile(draft.toWorldType())
        val geometry = manifest.planetCores.single()
        assertEquals(17L, geometry.edgeBlocks)
        assertEquals(32L, manifest.layers.first { it.dimension == geometry.coreDimension }.period.blocks)
    }
}
