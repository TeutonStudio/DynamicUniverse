package de.TeutonStudio.DynamicUniverse.client.render

import de.TeutonStudio.DynamicUniverse.cosmos.GlobeProjection
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod

/**
 * CPU-side unit-sphere geometry for the R³ renderer. This is intentionally a real triangle mesh,
 * not a collection of affine portal planes. A GPU adapter expands it with the body's visual
 * radius and samples [GlobeProjection] per vertex/fragment into its top-down terrain atlas.
 */
data class GeometricGlobeMesh(val directions: List<Vector3>, val indices: IntArray) {
    init {
        require(indices.size % 3 == 0) { "A globe mesh needs triangle indices." }
        require(indices.all { it in directions.indices }) { "A globe index is out of range." }
    }

    val triangleCount: Int get() = indices.size / 3
}

object GeometricGlobeMeshes {
    fun icosphere(subdivisions: Int): GeometricGlobeMesh {
        require(subdivisions in 0..6) { "Globe subdivisions outside the supported LOD range." }
        val t = (1.0 + kotlin.math.sqrt(5.0)) / 2.0
        val vertices = mutableListOf(
            Vector3(-1.0, t, 0.0).normalized(), Vector3(1.0, t, 0.0).normalized(),
            Vector3(-1.0, -t, 0.0).normalized(), Vector3(1.0, -t, 0.0).normalized(),
            Vector3(0.0, -1.0, t).normalized(), Vector3(0.0, 1.0, t).normalized(),
            Vector3(0.0, -1.0, -t).normalized(), Vector3(0.0, 1.0, -t).normalized(),
            Vector3(t, 0.0, -1.0).normalized(), Vector3(t, 0.0, 1.0).normalized(),
            Vector3(-t, 0.0, -1.0).normalized(), Vector3(-t, 0.0, 1.0).normalized(),
        )
        var faces = intArrayOf(
            0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11,
            1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8, 3, 9, 4,
            3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9, 4, 9, 5, 2, 4, 11,
            6, 2, 10, 8, 6, 7, 9, 8, 1,
        )
        repeat(subdivisions) {
            val midpointIndices = hashMapOf<Pair<Int, Int>, Int>()
            fun midpoint(first: Int, second: Int): Int {
                val key = minOf(first, second) to maxOf(first, second)
                return midpointIndices.getOrPut(key) {
                    val a = vertices[first]
                    val b = vertices[second]
                    vertices += (a + b).normalized()
                    vertices.lastIndex
                }
            }
            val next = IntArray(faces.size * 4)
            var out = 0
            for (index in faces.indices step 3) {
                val a = faces[index]; val b = faces[index + 1]; val c = faces[index + 2]
                val ab = midpoint(a, b); val bc = midpoint(b, c); val ca = midpoint(c, a)
                intArrayOf(a, ab, ca, b, bc, ab, c, ca, bc, ab, bc, ca).copyInto(next, out)
                out += 12
            }
            faces = next
        }
        return GeometricGlobeMesh(vertices, faces)
    }
}

/** Projection payload consumed by a terrain-atlas shader for one geometric sphere vertex. */
data class GlobeMeshSample(val direction: Vector3, val source: de.TeutonStudio.DynamicUniverse.cosmos.GlobeSample)

fun GeometricGlobeMesh.sample(period: HorizontalPeriod, body: GlobeRenderBody): List<GlobeMeshSample> =
    directions.map { direction -> GlobeMeshSample(direction, GlobeProjection.sample(direction, period, body.visual)) }
