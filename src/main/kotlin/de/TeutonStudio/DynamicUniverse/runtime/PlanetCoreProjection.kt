package de.TeutonStudio.DynamicUniverse.runtime

import net.minecraft.core.BlockPos
import java.lang.Math.floorMod
import java.security.MessageDigest
import java.util.HexFormat

enum class CoreShellFace { POSITIVE_X, NEGATIVE_X, POSITIVE_Y, NEGATIVE_Y, POSITIVE_Z, NEGATIVE_Z }

data class CoreShellCell(val face: CoreShellFace, val u: Int, val v: Int)

/**
 * A fixed local isometry from a deep-layer aperture cell to one cube-shell face. Persisting it
 * makes a core-originated hole reversible and prevents a later resolver pass from moving it.
 */
data class CoreAperturePlacement(
    val face: CoreShellFace,
    val originU: Int,
    val originV: Int,
    val rotationQuarterTurns: Int,
) {
    init { require(rotationQuarterTurns in 0..3) { "Core aperture rotation must be a quarter turn." } }

    fun project(cell: ApertureCell): CoreShellCell {
        val rotated = rotate(cell, rotationQuarterTurns)
        return CoreShellCell(face, originU + rotated.dx, originV + rotated.dz)
    }

    fun unproject(cell: CoreShellCell): ApertureCell? {
        if (cell.face != face) return null
        val local = ApertureCell(cell.u - originU, cell.v - originV)
        return rotate(local, (4 - rotationQuarterTurns) % 4)
    }
}

data class CoreApertureProjection(
    val apertureId: String,
    val placement: CoreAperturePlacement,
    val mapping: Map<ApertureCell, CoreShellCell>,
) {
    val rotationQuarterTurns: Int get() = placement.rotationQuarterTurns
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
            val candidates = aperture.corePlacement?.let { placement ->
                sequenceOf(projectionFor(geometry, aperture, placement))
            } ?: sequence {
                for (attempt in 0 until maxAttemptsPerAperture) yield(candidate(geometry, aperture, edgeInt, attempt))
            }
            val projection = candidates.filterNotNull().firstOrNull { candidate ->
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

    /** Returns only interior face cells; cube edges and corners are never legal apertures. */
    fun shellCellAt(geometry: PlanetCoreGeometry, position: BlockPos): CoreShellCell? {
        val edge = geometry.edgeBlocks
        if (edge !in 2..Int.MAX_VALUE.toLong()) return null
        val n = edge.toInt()
        val half = n / 2
        val min = -half
        val max = half - 1
        fun index(value: Int): Int = value - min
        val candidates = buildList {
            if (position.x == max && position.y in min..max && position.z in min..max) add(CoreShellCell(CoreShellFace.POSITIVE_X, index(position.z), index(position.y)))
            if (position.x == min && position.y in min..max && position.z in min..max) add(CoreShellCell(CoreShellFace.NEGATIVE_X, n - 1 - index(position.z), index(position.y)))
            if (position.y == max && position.x in min..max && position.z in min..max) add(CoreShellCell(CoreShellFace.POSITIVE_Y, index(position.x), index(position.z)))
            if (position.y == min && position.x in min..max && position.z in min..max) add(CoreShellCell(CoreShellFace.NEGATIVE_Y, index(position.x), n - 1 - index(position.z)))
            if (position.z == max && position.x in min..max && position.y in min..max) add(CoreShellCell(CoreShellFace.POSITIVE_Z, index(position.x), index(position.y)))
            if (position.z == min && position.x in min..max && position.y in min..max) add(CoreShellCell(CoreShellFace.NEGATIVE_Z, n - 1 - index(position.x), index(position.y)))
        }
        val cell = candidates.singleOrNull() ?: return null
        return cell.takeIf { isInsideMargin(geometry, it) }
    }

    fun projectionFor(
        geometry: PlanetCoreGeometry,
        aperture: CoreBoundaryAperture,
        placement: CoreAperturePlacement,
    ): CoreApertureProjection? {
        val mapping = aperture.shape.cells.associateWith(placement::project)
        if (mapping.values.any { !isInsideMargin(geometry, it) }) return null
        return CoreApertureProjection(aperture.id, placement, mapping)
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
        val placement = CoreAperturePlacement(face, u0 - minX, v0 - minZ, rotation)
        return projectionFor(geometry, aperture, placement)
    }

    private fun rotate(cell: ApertureCell, quarterTurns: Int): ApertureCell = when (floorMod(quarterTurns, 4)) {
        0 -> cell
        1 -> ApertureCell(-cell.dz, cell.dx)
        2 -> ApertureCell(-cell.dx, -cell.dz)
        else -> ApertureCell(cell.dz, -cell.dx)
    }

    private fun isInsideMargin(geometry: PlanetCoreGeometry, cell: CoreShellCell): Boolean =
        cell.u in geometry.edgeMarginBlocks until (geometry.edgeBlocks.toInt() - geometry.edgeMarginBlocks) &&
            cell.v in geometry.edgeMarginBlocks until (geometry.edgeBlocks.toInt() - geometry.edgeMarginBlocks)

    private fun neighbourhood(cell: CoreShellCell): List<CoreShellCell> = listOf(
        CoreShellCell(cell.face, cell.u + 1, cell.v),
        CoreShellCell(cell.face, cell.u - 1, cell.v),
        CoreShellCell(cell.face, cell.u, cell.v + 1),
        CoreShellCell(cell.face, cell.u, cell.v - 1),
    )
}

private fun rotate(cell: ApertureCell, quarterTurns: Int): ApertureCell = when (floorMod(quarterTurns, 4)) {
    0 -> cell
    1 -> ApertureCell(-cell.dz, cell.dx)
    2 -> ApertureCell(-cell.dx, -cell.dz)
    else -> ApertureCell(cell.dz, -cell.dx)
}
