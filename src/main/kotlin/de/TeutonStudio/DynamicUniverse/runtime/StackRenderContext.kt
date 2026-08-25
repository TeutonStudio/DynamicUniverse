package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId

/**
 * Client-side copy of the server-authoritative visual stack layout.
 *
 * The values are intentionally relative. A normal world render has an offset of zero; a
 * recursively rendered portal target is shifted relative to the player's source layer.
 */
data class StackRenderLayer(
    val dimension: DimensionId,
    val stackId: String,
    val index: Int,
)

object StackRenderContext {
    @Volatile
    private var layersByDimension: Map<DimensionId, StackRenderLayer> = emptyMap()

    fun install(layers: Collection<StackRenderLayer>) {
        layersByDimension = layers.associateBy(StackRenderLayer::dimension)
    }

    fun clear() {
        layersByDimension = emptyMap()
    }

    fun verticalOffset(rendered: DimensionId, viewer: DimensionId): Int {
        val target = layersByDimension[rendered] ?: return 0
        val source = layersByDimension[viewer] ?: return 0
        if (target.stackId != source.stackId) return 0
        return Math.multiplyExact(
            target.index - source.index,
            UniverseGeometryManifest.RENDER_LAYER_SPACING_BLOCKS,
        )
    }
}
