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
        assertEquals(32L * 16L, manifest.layers.first { it.dimension.value.endsWith("planet/0/0/0/core") }.period.blocks)
        val underground = manifest.layers.first { it.dimension.value == "undergarden:undergarden" }
        assertEquals(32L * 16L * 8L * 8L, underground.period.blocks)
        assertEquals(underground.period.blocks.toDouble() / (2.0 * Math.PI), underground.projectionRadiusBlocks)
        assertEquals(2, manifest.airBuffers.size)
        assertTrue(manifest.airBuffers.all { it.lowerBlocks == 5 && it.upperBlocks == 5 })
    }
}
