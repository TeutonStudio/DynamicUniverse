package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPosition

data class ApertureCell(val dx: Int, val dz: Int)

data class ApertureShape(val cells: Set<ApertureCell>) {
    init { require(cells.isNotEmpty()) { "An aperture shape cannot be empty." } }

    fun contains(cell: ApertureCell): Boolean = cell in cells
    fun touches(cell: ApertureCell): Boolean = NEIGHBOURS.any { delta ->
        ApertureCell(cell.dx + delta.dx, cell.dz + delta.dz) in cells
    }
    fun with(cell: ApertureCell): ApertureShape = ApertureShape(cells + cell)

    companion object {
        val SINGLE = ApertureShape(setOf(ApertureCell(0, 0)))
        private val NEIGHBOURS = listOf(
            ApertureCell(1, 0), ApertureCell(-1, 0),
            ApertureCell(0, 1), ApertureCell(0, -1),
        )
    }
}

sealed interface PersistedBoundaryAperture {
    val id: String
    val connectionId: String
    val createdSequence: Long
    val shape: ApertureShape
}

data class PairedBoundaryAperture(
    override val id: String,
    override val connectionId: String,
    override val createdSequence: Long,
    val sourceAnchor: HorizontalPosition,
    val targetAnchor: HorizontalPosition,
    override val shape: ApertureShape = ApertureShape.SINGLE,
) : PersistedBoundaryAperture

data class CoreBoundaryAperture(
    override val id: String,
    override val connectionId: String,
    override val createdSequence: Long,
    val planetId: String,
    val deepDimension: DimensionId,
    val deepFace: DimensionBoundaryFace,
    val deepAnchor: HorizontalPosition,
    /** Core-originated records retain their concrete shell anchor. */
    val coreAnchor: CoreShellCell? = null,
    val coreRotationQuarterTurns: Int? = null,
    override val shape: ApertureShape = ApertureShape.SINGLE,
) : PersistedBoundaryAperture {
    init {
        require((coreAnchor == null) == (coreRotationQuarterTurns == null)) {
            "A core aperture needs both a shell anchor and a rotation, or neither."
        }
    }
}

internal fun HorizontalPeriod.canonical(position: HorizontalPosition): HorizontalPosition =
    HorizontalPosition(canonical(position.x), canonical(position.z))

internal fun HorizontalPeriod.offset(anchor: HorizontalPosition, position: HorizontalPosition): ApertureCell {
    val dx = canonical(position.x - anchor.x)
    val dz = canonical(position.z - anchor.z)
    require(dx in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "Aperture X offset exceeds Int range." }
    require(dz in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "Aperture Z offset exceeds Int range." }
    return ApertureCell(dx.toInt(), dz.toInt())
}

internal fun HorizontalPeriod.apply(anchor: HorizontalPosition, cell: ApertureCell): HorizontalPosition =
    canonical(HorizontalPosition(anchor.x + cell.dx, anchor.z + cell.dz))
