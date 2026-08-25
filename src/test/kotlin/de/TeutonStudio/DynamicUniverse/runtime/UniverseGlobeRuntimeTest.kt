package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.cosmos.PlanetSpaceBinding
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseKinematicState
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseSpace
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnectionKind
import de.TeutonStudio.DynamicUniverse.dimension.SpatialPosition
import de.TeutonStudio.DynamicUniverse.dimension.SpatialVelocity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UniverseGlobeRuntimeTest {
    @Test fun `default geometry exposes visual sources for Sol and Terra`() {
        val manifest = UniverseGeometryCompiler.compile(UniverseDefaultWorldType.worldType)
        assertEquals(setOf("star_0_0", "planet_0_0_0"), manifest.celestialGlobes.map { it.bodyId }.toSet())
        assertTrue(manifest.celestialGlobes.single { it.bodyId == "planet_0_0_0" }.period.blocks > 0L)
    }

    @Test fun `outer traversal canonicalizes torus coordinates before entering universe space`() {
        val manifest = UniverseGeometryCompiler.compile(UniverseDefaultWorldType.worldType)
        val terra = manifest.celestialGlobes.single { it.bodyId == "planet_0_0_0" }
        val space = UniverseSpace(manifest.universe.id)
        val state = UniverseRuntimeState(
            hostId = "dynamicuniverse:test/host",
            universeSpaceId = space.id,
            planetBindings = manifest.celestialGlobes.map { PlanetSpaceBinding(it.bodyId, "${it.bodyId}:local", space) },
            planetKinematics = manifest.celestialGlobes.map { globe ->
                PlanetKinematicState(globe.bodyId, UniverseKinematicState(if (globe.bodyId == terra.bodyId) Vector3(100.0, 20.0, -50.0) else Vector3.ZERO))
            },
        )
        val route = manifest.links.single { it.kind == DimensionConnectionKind.UNIVERSE_TRANSITION && it.source == terra.sourceDimension }
        val planner = UniverseSpaceTraversalPlanner(manifest, state)
        val exit = assertNotNull(planner.exit(route, TraversalState(
            SpatialPosition(terra.period.halfBlocks + 2.25, 321.0, -4.5),
            SpatialVelocity(1.0, 2.0, 3.0),
        )))
        assertEquals(-terra.period.halfBlocks + 2.25, exit.canonicalSourcePosition.x)
        assertEquals(100.0 - terra.period.halfBlocks + 2.25, exit.universePosition.x)

        val entry = assertNotNull(planner.enter(terra.bodyId, exit.universePosition, exit.universeVelocity, 200.0))
        assertEquals(terra.sourceDimension, entry.target)
        assertEquals(-terra.period.halfBlocks + 2.25, entry.position.x)
        assertEquals(200.0, entry.position.y)
    }
}
