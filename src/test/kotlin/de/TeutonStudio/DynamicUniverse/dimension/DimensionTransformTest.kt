package de.TeutonStudio.DynamicUniverse.dimension

import kotlin.test.Test
import kotlin.test.assertEquals
import de.TeutonStudio.DynamicUniverse.runtime.PortalTraversalReconciler
import de.TeutonStudio.DynamicUniverse.runtime.TraversalState
import de.TeutonStudio.DynamicUniverse.cosmos.PlanetFrame
import de.TeutonStudio.DynamicUniverse.cosmos.PlanetFrameLinkResolver
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseFrame

class DimensionTransformTest {
    @Test
    fun `travel factor scales horizontal coordinates and velocity but keeps gravity one to one`() {
        val link = DimensionConnection(
            id = "dynamicuniverse:test/core-to-surface",
            source = DimensionId("dynamicuniverse:test/core"),
            target = DimensionId("dynamicuniverse:test/surface"),
            scale = DimensionScale(8),
            boundarySurface = BoundarySurface.BEDROCK,
        )

        assertEquals(1.0, link.physicalScale)
        assertEquals(SpatialPosition(16.0, 0.5, -24.0), link.targetPosition(SpatialPosition(2.0, 0.5, -3.0)))
        assertEquals(SpatialVelocity(8.0, -1.25, -4.0), link.targetVelocity(SpatialVelocity(1.0, -1.25, -0.5)))
        assertEquals(SpatialPosition(2.0, 0.5, -3.0), link.inverse().targetPosition(link.targetPosition(SpatialPosition(2.0, 0.5, -3.0))))
        assertEquals(
            TraversalState(SpatialPosition(16.0, 0.5, -24.0), SpatialVelocity(8.0, -1.25, -4.0)),
            PortalTraversalReconciler.reconcile(link, TraversalState(SpatialPosition(2.0, 0.5, -3.0), SpatialVelocity(1.0, -1.25, -0.5))),
        )
    }

    @Test
    fun `outer link adds moving planet velocity without moving local chunks`() {
        val link = DimensionConnection(
            id = "dynamicuniverse:test/surface-to-universe",
            source = DimensionId("dynamicuniverse:test/surface"),
            target = DimensionId("dynamicuniverse:universe"),
            scale = DimensionScale.ONE,
            kind = DimensionConnectionKind.UNIVERSE_TRANSITION,
        )
        val frame = PlanetFrame(
            id = "dynamicuniverse:test:frame",
            universeFrame = UniverseFrame(),
            anchor = SpatialPosition(100.0, 0.0, 0.0),
            velocity = SpatialVelocity(3.0, 0.0, 0.0),
        )

        assertEquals(
            SpatialPosition(102.0, 0.0, 0.0),
            PlanetFrameLinkResolver.toUniverse(link, frame, SpatialPosition(2.0, 0.0, 0.0), SpatialVelocity.ZERO).position,
        )
        assertEquals(
            SpatialVelocity(4.0, 0.0, 0.0),
            PlanetFrameLinkResolver.toUniverse(link, frame, SpatialPosition.ZERO, SpatialVelocity(1.0, 0.0, 0.0)).velocity,
        )
    }
}
