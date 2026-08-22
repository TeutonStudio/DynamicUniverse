package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionPosition
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BedrockAperturePlannerTest {
    private val nether = DimensionId("dynamicuniverse:terra/nether")
    private val overworld = DimensionId("dynamicuniverse:terra/overworld")
    private val netherToOverworld = DimensionConnection(
        id = "terra/main/nether-to-overworld",
        source = nether,
        target = overworld,
        scale = DimensionScale(8),
        boundarySurface = BoundarySurface.BEDROCK,
    )
    private val planes = listOf(
        BedrockBoundaryPlane(nether, DimensionBoundaryFace.UPPER, 127),
        BedrockBoundaryPlane(overworld, DimensionBoundaryFace.LOWER, -64),
    )

    @Test
    fun `breaking the overworld floor opens the mapped nether ceiling`() {
        val aperture = BedrockAperturePlanner(listOf(netherToOverworld), planes).apertureFor(
            overworld,
            DimensionPosition(80, -64, -40),
        )

        requireNotNull(aperture)
        assertEquals(nether, aperture.connection.target)
        assertEquals(DimensionBoundaryFace.LOWER, aperture.connection.sourceBoundaryFace)
        assertEquals(DimensionBoundaryFace.UPPER, aperture.connection.targetBoundaryFace)
        assertEquals(DimensionPosition(10, 127, -5), aperture.target)
    }

    @Test
    fun `only the registered bedrock plane can open an aperture`() {
        val planner = BedrockAperturePlanner(listOf(netherToOverworld), planes)

        assertNull(planner.apertureFor(overworld, DimensionPosition(80, -63, -40)))
        assertNull(planner.apertureFor(nether, DimensionPosition(10, 126, -5)))
    }

    @Test
    fun `air boundaries never create bedrock apertures`() {
        val sky = DimensionId("dynamicuniverse:terra/sky")
        val airConnection = netherToOverworld.copy(
            id = "terra/main/overworld-to-sky",
            source = overworld,
            target = sky,
            boundarySurface = BoundarySurface.AIR,
        )
        val planner = BedrockAperturePlanner(
            listOf(airConnection),
            planes + BedrockBoundaryPlane(sky, DimensionBoundaryFace.LOWER, 320),
        )

        assertNull(planner.apertureFor(overworld, DimensionPosition(80, -64, -40)))
    }
}
