package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StackRenderContextTest {
    @AfterEach
    fun clear() {
        StackRenderContext.clear()
    }

    @Test
    fun `portal target is offset relative to the viewing stack layer`() {
        val core = DimensionId("dynamicuniverse:terra/core")
        val surface = DimensionId("minecraft:overworld")
        val sky = DimensionId("dynamicuniverse:terra/sky")
        StackRenderContext.install(
            listOf(
                StackRenderLayer(core, "terra/main", 0),
                StackRenderLayer(surface, "terra/main", 1),
                StackRenderLayer(sky, "terra/main", 2),
            ),
        )

        assertEquals(0, StackRenderContext.verticalOffset(surface, surface))
        assertEquals(UniverseGeometryManifest.RENDER_LAYER_SPACING_BLOCKS, StackRenderContext.verticalOffset(sky, surface))
        assertEquals(-UniverseGeometryManifest.RENDER_LAYER_SPACING_BLOCKS, StackRenderContext.verticalOffset(core, surface))
    }

    @Test
    fun `unrelated stack cannot acquire an assumed render offset`() {
        val surface = DimensionId("minecraft:overworld")
        val moon = DimensionId("dynamicuniverse:moon/surface")
        StackRenderContext.install(
            listOf(
                StackRenderLayer(surface, "terra/main", 1),
                StackRenderLayer(moon, "luna/main", 1),
            ),
        )

        assertEquals(0, StackRenderContext.verticalOffset(moon, surface))
    }
}
