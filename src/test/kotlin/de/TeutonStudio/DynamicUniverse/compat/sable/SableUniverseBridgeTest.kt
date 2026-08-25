package de.TeutonStudio.DynamicUniverse.compat.sable

import de.TeutonStudio.DynamicUniverse.cosmos.CelestialSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseKinematicState
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse

class SableUniverseBridgeTest {
    @Test fun `bridge bytecode remains loadable without the optional Sable API`() {
        val resource = requireNotNull(SableUniverseBridge::class.java.getResourceAsStream("SableUniverseBridge.class"))
        val bytecode = resource.use { it.readBytes().decodeToString() }
        assertFalse(bytecode.contains("dev/ryanhcode/sable/"))
    }

    @Test fun `bridge remains usable when no Sable sublevel is bound`() {
        val bridge = SableUniverseBridge()
        val object_ = CelestialSpatialObject("station", 1.0, UniverseKinematicState(Vector3.ZERO), 1.0)
        assertNull(bridge.universePosition(SableSublevelId("missing"), Vector3.ZERO, SableSublevelPose(Vector3.ZERO), object_))
    }

    @Test fun `bound sublevel projects through its spatial object`() {
        val bridge = SableUniverseBridge()
        bridge.bind(SableSublevelBinding(SableSublevelId("plot_1"), "station"))
        val object_ = CelestialSpatialObject("station", 1.0, UniverseKinematicState(Vector3(10.0, 0.0, 0.0)), 1.0)
        assertEquals(Vector3(13.0, 0.0, 0.0), bridge.universePosition(SableSublevelId("plot_1"), Vector3(2.0, 0.0, 0.0), SableSublevelPose(Vector3(1.0, 0.0, 0.0)), object_))
    }
}
