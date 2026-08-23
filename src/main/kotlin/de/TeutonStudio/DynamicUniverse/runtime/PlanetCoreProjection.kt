package de.TeutonStudio.DynamicUniverse.runtime

import net.minecraft.core.BlockPos
import java.lang.Math.floorMod
import java.security.MessageDigest
import java.util.HexFormat

enum class CoreShellFace { POSITIVE_X, NEGATIVE_X, POSITIVE_Y, NEGATIVE_Y, POSITIVE_Z, NEGATIVE_Z }

data class CoreShellCell(val face: CoreShellFace, val u: Int, val v: Int)

data class CoreApertureProjection(
    val apertureId: String,
    val rotationQuarterTurns: Int,
    val mapping: Map<ApertureCell, CoreShellCell>,
) {
    val cells: Set<CoreShellCell> get() = mapping.values.toSet()
}

class PlanetCoreProjectionResolver(
    private val maxAttemptsPerAperture: Int = 4096,
) {
    fun resolve(
        geometry: PlanetCoreGeometry,
        apertures: Collection<CoreBoundaryAperture>,
    ): Map<String, CoreApertureProjection>? {
        val edge = geometry.edgeBlocks
        if (edge !in 1..Int.MAX_VALUE.toLong()) return null
        val edgeInt = edge.toInt()
        val occupied = mutableSetOf<CoreShellCell>()
        val result = linkedMapOf<String, CoreApertureProjection>()

        for (aperture in apertures.sortedWith(compareBy<CoreBoundaryAperture> { it.createdSequence }.thenBy { it.id })) {
            val projection = sequence {
                for (attempt in 0 until maxAttemptsPerAperture) yield(candidate(geometry, aperture, edgeInt, attempt))
            }.filterNotNull().firstOrNull { candidate ->
                candidate.cells.none { it in occupied } && candidate.cells.flatMap(::neighbourhood).none { it in occupied }
            } ?: return null
            occupied += projection.cells
            result[aperture.id] = projection
        }
        return result
    }

    fun blockPositions(
        geometry: PlanetCoreGeometry,
        projection: CoreApertureProjection,
    ): Set<BlockPos>? = projection.cells.mapTo(linkedSetOf()) { cell ->
        blockPosition(geometry, cell) ?: return null
    }

    fun blockPosition(geometry: PlanetCoreGeometry, cell: CoreShellCell): BlockPos? {
        val edge = geometry.edgeBlocks
        if (edge !in 2..Int.MAX_VALUE.toLong()) return null
        val n = edge.toInt()
        val half = n / 2
        fun axis(index: Int): Int = -half + index
        return when (cell.face) {
            CoreShellFace.POSITIVE_X -> BlockPos(half - 1, axis(cell.v), axis(cell.u))
            CoreShellFace.NEGATIVE_X -> BlockPos(-half, axis(cell.v), axis(n - 1 - cell.u))
            CoreShellFace.POSITIVE_Y -> BlockPos(axis(cell.u), half - 1, axis(cell.v))
            CoreShellFace.NEGATIVE_Y -> BlockPos(axis(cell.u), -half, axis(n - 1 - cell.v))
            CoreShellFace.POSITIVE_Z -> BlockPos(axis(cell.u), axis(cell.v), half - 1)
            CoreShellFace.NEGATIVE_Z -> BlockPos(axis(n - 1 - cell.u), axis(cell.v), -half)
        }
    }

    private fun candidate(
        geometry: PlanetCoreGeometry,
        aperture: CoreBoundaryAperture,
        edge: Int,
        attempt: Int,
    ): CoreApertureProjection? {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("${geometry.planetId}|${aperture.id}|$attempt".toByteArray())
        val seed = HexFormat.of().formatHex(digest)
        val face = CoreShellFace.entries[floorMod(seed.substring(0, 8).toLong(16), CoreShellFace.entries.size.toLong()).toInt()]
        val rotation = floorMod(seed.substring(8, 16).toLong(16), 4L).toInt()
        val rotatedByOriginal = aperture.shape.cells.associateWith { rotate(it, rotation) }
        val minX = rotatedByOriginal.values.minOf { it.dx }
        val minZ = rotatedByOriginal.values.minOf { it.dz }
        val normalizedByOriginal = rotatedByOriginal.mapValues { (_, cell) -> ApertureCell(cell.dx - minX, cell.dz - minZ) }
        val width = normalizedByOriginal.values.maxOf { it.dx } + 1
        val height = normalizedByOriginal.values.maxOf { it.dz } + 1
        val margin = geometry.edgeMarginBlocks
        val availableU = edge - 2 * margin - width + 1
        val availableV = edge - 2 * margin - height + 1
        if (availableU <= 0 || availableV <= 0) return null
        val u0 = margin + floorMod(seed.substring(16, 24).toLong(16), availableU.toLong()).toInt()
        val v0 = margin + floorMod(seed.substring(24, 32).toLong(16), availableV.toLong()).toInt()
        val mapping = normalizedByOriginal.mapValues { (_, cell) -> CoreShellCell(face, u0 + cell.dx, v0 + cell.dz) }
        return CoreApertureProjection(aperture.id, rotation, mapping)
    }

    private fun rotate(cell: ApertureCell, quarterTurns: Int): ApertureCell = when (floorMod(quarterTurns, 4)) {
        0 -> cell
        1 -> ApertureCell(-cell.dz, cell.dx)
        2 -> ApertureCell(-cell.dx, -cell.dz)
        else -> ApertureCell(cell.dz, -cell.dx)
    }

    private fun neighbourhood(cell: CoreShellCell): List<CoreShellCell> = listOf(
        CoreShellCell(cell.face, cell.u + 1, cell.v),
        CoreShellCell(cell.face, cell.u - 1, cell.v),
        CoreShellCell(cell.face, cell.u, cell.v + 1),
        CoreShellCell(cell.face, cell.u, cell.v - 1),
    )
}
