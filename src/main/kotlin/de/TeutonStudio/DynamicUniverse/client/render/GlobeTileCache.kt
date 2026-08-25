package de.TeutonStudio.DynamicUniverse.client.render

import de.TeutonStudio.DynamicUniverse.cosmos.GlobeAtlas

/** Identity of one server-authorized, top-down source tile in the visual globe atlas. */
data class GlobeTileKey(
    val bodyId: String,
    val atlas: GlobeAtlas,
    val lod: Int,
    val tileX: Int,
    val tileZ: Int,
    val terrainRevision: Long,
) {
    init {
        require(bodyId.isNotBlank() && lod >= 0 && terrainRevision >= 0L)
    }
}

data class GlobeTile(val rgba: IntArray, val width: Int, val height: Int) {
    init { require(width > 0 && height > 0 && rgba.size == width * height) }
}

/** Small LRU cache; GPU texture ownership intentionally stays in the future renderer adapter. */
class GlobeTileCache(private val maxTiles: Int = 256) {
    init { require(maxTiles > 0) }
    private val entries = object : LinkedHashMap<GlobeTileKey, GlobeTile>(maxTiles, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<GlobeTileKey, GlobeTile>?) = size > maxTiles
    }

    @Synchronized fun get(key: GlobeTileKey): GlobeTile? = entries[key]
    @Synchronized fun put(key: GlobeTileKey, tile: GlobeTile) { entries[key] = tile }
    @Synchronized fun invalidate(bodyId: String, beforeRevision: Long = Long.MAX_VALUE) {
        entries.keys.removeIf { it.bodyId == bodyId && it.terrainRevision < beforeRevision }
    }
    @Synchronized fun size(): Int = entries.size
}

/** Screen-space policy, independent of a specific terrain or GPU renderer. */
object GlobeLodPolicy {
    fun lodFor(projectedDiameterPixels: Double, maxLod: Int): Int {
        require(projectedDiameterPixels.isFinite() && projectedDiameterPixels >= 0.0)
        require(maxLod >= 0)
        return when {
            projectedDiameterPixels < 16.0 -> maxLod
            projectedDiameterPixels < 64.0 -> (maxLod - 1).coerceAtLeast(0)
            projectedDiameterPixels < 256.0 -> (maxLod - 2).coerceAtLeast(0)
            else -> 0
        }
    }
}
