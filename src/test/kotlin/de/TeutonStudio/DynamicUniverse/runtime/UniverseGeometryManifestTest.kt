package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.client.worldcreation.UniverseWorldCreationDraft
import de.TeutonStudio.DynamicUniverse.client.worldcreation.toWorldType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UniverseGeometryManifestTest {
    @Test
    fun `compiler assigns a toroidal period to every generated layer`() {
        val manifest = UniverseGeometryCompiler.compile(UniverseWorldCreationDraft().toWorldType())

        assertEquals(7, manifest.layers.size)
        assertEquals(32L * 16L, manifest.layers.first { it.dimension.value.endsWith("planet/0/0/0/core") }.period.blocks)
        assertEquals(32L * 16L * 4L, manifest.layers.first { it.dimension.value.endsWith("planet/0/0/0/inner_1") }.period.blocks)
    }
}
