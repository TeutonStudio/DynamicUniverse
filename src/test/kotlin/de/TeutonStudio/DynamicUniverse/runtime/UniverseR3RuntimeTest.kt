package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.CelestialSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseKinematicState
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseSpace
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class UniverseR3RuntimeTest {
    @Test fun `rebase changes only local coordinates`() {
        val bubble = SimulationBubble(Vector3.ZERO, 1_000.0, 400.0, 100.0)
        val global = Vector3(1_250.0, -20.0, 55.0)
        val rebase = requireNotNull(bubble.rebaseFor(Vector3(1_120.0, 0.0, 0.0)))
        assertEquals(rebase.next.localPosition(global), rebase.rebaseLocalPosition(bubble.localPosition(global)))
        assertEquals(global, rebase.next.origin + rebase.next.localPosition(global))
        assertNull(bubble.rebaseFor(Vector3(399.0, 0.0, 0.0)))
    }

    @Test fun `remote bubbles remain separate while nearby bubbles are merge candidates`() {
        val near = UniverseHostSlot("near")
        val far = UniverseHostSlot("far")
        val layout = UniverseHostLayout(bubbles = listOf(
            HostedSimulationBubble(near, SimulationBubble(Vector3.ZERO, 100.0)),
            HostedSimulationBubble(far, SimulationBubble(Vector3(10_000.0, 0.0, 0.0), 100.0)),
        ))
        assertFalse(layout.mergeable(near, far))
        val adjacent = layout.copy(bubbles = listOf(
            HostedSimulationBubble(near, SimulationBubble(Vector3.ZERO, 100.0)),
            HostedSimulationBubble(far, SimulationBubble(Vector3(199.0, 0.0, 0.0), 100.0)),
        ))
        assertEquals(true, adjacent.mergeable(near, far))
    }

    @Test fun `host boundary rebasing preserves an unbounded vertical R3 position`() {
        val bubble = SimulationBubble(Vector3(1_000.0, 50_000.0, -20.0), 2_000.0)
        val local = Vector3(5.0, 289.0, 3.0)
        val plan = requireNotNull(UniverseHostRebasePlanner.plan(bubble, local, UniverseHostLocalBounds.aroundOrigin()))
        assertEquals(Vector3(1_005.0, 50_289.0, -17.0), plan.globalPosition)
        assertEquals(Vector3.ZERO, plan.rebasedLocalPosition)
        assertEquals(plan.globalPosition, plan.rebase.next.origin + plan.rebasedLocalPosition)
    }

    @Test fun `plotyard reservation rejects colliding host slots`() {
        assertFailsWith<IllegalArgumentException> {
            UniverseHostLayout(
                bubbles = listOf(HostedSimulationBubble(UniverseHostSlot("player"), SimulationBubble(Vector3.ZERO, 100.0))),
                sablePlotyard = SablePlotyardReservation(Vector3(150.0, 0.0, 0.0), 100.0),
            )
        }
    }

    @Test fun `runtime state survives save snapshot and restore`() {
        val space = UniverseSpace("dynamicuniverse:test/universe")
        val object_ = CelestialSpatialObject("terra", 5.972e24, UniverseKinematicState(Vector3(1.0, 2.0, 3.0)), 6.371e6)
        val host = UniverseHost("dynamicuniverse:test/host", space, layout = UniverseHostLayout(
            bubbles = listOf(HostedSimulationBubble(UniverseHostSlot("player"), SimulationBubble(Vector3.ZERO, 128.0))),
        ))
        host.register(object_)
        val snapshot = UniverseRuntimePersistence().snapshot(host, emptyList())
        val restored = UniverseRuntimePersistence().restore(snapshot)
        assertEquals(snapshot, UniverseRuntimePersistence().snapshot(restored.host, restored.planetBindings, restored.planetKinematics))
        assertEquals(object_, restored.host.objectById("terra"))
    }
}
