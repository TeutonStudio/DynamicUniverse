package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.CosmicSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.PlanetSpaceBinding
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseKinematicState
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseSpace
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class UniverseRuntimePersistenceTest {
    @Test
    fun `runtime snapshot restores host objects and planet bindings`() {
        val space = UniverseSpace("dynamicuniverse:sol")
        val host = UniverseHost("dynamicuniverse:sol-runtime", space)
        host.register(CosmicSpatialObject("earth", 5.972e24, 6.371e6, UniverseKinematicState(Vector3(1.0, 2.0, 3.0))))
        val binding = PlanetSpaceBinding("earth", "dynamicuniverse:earth/surface", space, UniverseKinematicState(Vector3(1.0, 2.0, 3.0)))

        val restored = UniverseRuntimePersistence().restore(UniverseRuntimePersistence().snapshot(host, listOf(binding)))

        assertEquals(host.id, restored.host.id)
        assertEquals(space, restored.host.space)
        assertEquals(listOf("earth"), restored.host.objects().map { it.id })
        assertEquals(listOf(binding), restored.planetBindings)
    }
}
