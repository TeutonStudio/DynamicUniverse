package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionPosition
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPosition
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DynamicApertureTopologyTest {
    @Test
    fun `dimension scale chooses the anchor but aperture growth stays locally one to one`() {
        val connection = DimensionConnection(
            id = "terra/main/deep-to-surface",
            source = DimensionId("dynamicuniverse:deep"),
            target = DimensionId("dynamicuniverse:surface"),
            scale = DimensionScale(8),
        )
        val mapped = connection.targetPosition(DimensionPosition(10, 0, -5))
        val aperture = PairedBoundaryAperture(
            id = "aperture-1",
            connectionId = connection.id,
            createdSequence = 1,
            sourceAnchor = HorizontalPosition(10, -5),
            targetAnchor = HorizontalPosition(mapped.x, mapped.z),
            shape = ApertureShape(setOf(ApertureCell(0, 0), ApertureCell(1, 0))),
        )
        val targetPeriod = HorizontalPeriod(4096)

        assertEquals(HorizontalPosition(80, -40), aperture.targetAnchor)
        assertEquals(HorizontalPosition(81, -40), targetPeriod.apply(aperture.targetAnchor, ApertureCell(1, 0)))
        assertNotEquals(HorizontalPosition(88, -40), targetPeriod.apply(aperture.targetAnchor, ApertureCell(1, 0)))
    }

    @Test
    fun `aperture adjacency crosses a toroidal seam`() {
        val period = HorizontalPeriod(32)
        val anchor = HorizontalPosition(15, 0)
        val acrossSeam = HorizontalPosition(-16, 0)

        assertEquals(ApertureCell(1, 0), period.offset(anchor, acrossSeam))
        assertEquals(acrossSeam, period.apply(anchor, ApertureCell(1, 0)))
    }

    @Test
    fun `planet core projection is deterministic edge safe and non overlapping`() {
        val geometry = PlanetCoreGeometry(
            planetId = "terra",
            connectionId = "terra/main/core-to-deep",
            coreDimension = DimensionId("dynamicuniverse:terra/core"),
            deepDimension = DimensionId("dynamicuniverse:terra/deep"),
            edgeBlocks = 64,
            edgeMarginBlocks = 3,
        )
        val first = CoreBoundaryAperture(
            id = "aperture-1",
            connectionId = geometry.connectionId,
            createdSequence = 1,
            planetId = geometry.planetId,
            deepDimension = geometry.deepDimension,
            deepFace = DimensionBoundaryFace.LOWER,
            deepAnchor = HorizontalPosition(0, 0),
            shape = ApertureShape(setOf(ApertureCell(0, 0), ApertureCell(1, 0), ApertureCell(1, 1))),
        )
        val second = first.copy(
            id = "aperture-2",
            createdSequence = 2,
            deepAnchor = HorizontalPosition(20, 20),
        )
        val resolver = PlanetCoreProjectionResolver()
        val a = assertNotNull(resolver.resolve(geometry, listOf(first, second)))
        val b = assertNotNull(resolver.resolve(geometry, listOf(first, second)))

        assertEquals(a, b)
        val firstCells = assertNotNull(a[first.id]).cells
        val secondCells = assertNotNull(a[second.id]).cells
        assertTrue(firstCells.intersect(secondCells).isEmpty())
        (firstCells + secondCells).forEach { cell ->
            assertTrue(cell.u in 3 until 61)
            assertTrue(cell.v in 3 until 61)
        }
    }

    @Test
    fun `persisted core placement keeps a core-originated aperture at its selected shell cell`() {
        val geometry = PlanetCoreGeometry(
            planetId = "terra",
            connectionId = "terra/main/core-to-deep",
            coreDimension = DimensionId("dynamicuniverse:terra/core"),
            deepDimension = DimensionId("dynamicuniverse:terra/deep"),
            edgeBlocks = 64,
            edgeMarginBlocks = 3,
        )
        val placement = CoreAperturePlacement(CoreShellFace.POSITIVE_Y, 20, 21, 1)
        val aperture = CoreBoundaryAperture(
            id = "core-origin",
            connectionId = geometry.connectionId,
            createdSequence = 1,
            planetId = geometry.planetId,
            deepDimension = geometry.deepDimension,
            deepFace = DimensionBoundaryFace.LOWER,
            deepAnchor = HorizontalPosition(0, 0),
            shape = ApertureShape(setOf(ApertureCell(0, 0), ApertureCell(1, 0))),
            corePlacement = placement,
        )

        val projections = assertNotNull(PlanetCoreProjectionResolver().resolve(geometry, listOf(aperture)))
        val projection = assertNotNull(projections[aperture.id])
        assertEquals(CoreShellCell(CoreShellFace.POSITIVE_Y, 20, 21), projection.mapping[ApertureCell(0, 0)])
        assertEquals(CoreShellCell(CoreShellFace.POSITIVE_Y, 20, 22), projection.mapping[ApertureCell(1, 0)])
        assertEquals(ApertureCell(1, 0), placement.unproject(CoreShellCell(CoreShellFace.POSITIVE_Y, 20, 22)))
    }
}
