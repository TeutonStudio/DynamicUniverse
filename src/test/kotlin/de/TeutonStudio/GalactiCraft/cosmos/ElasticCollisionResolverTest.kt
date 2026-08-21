package de.TeutonStudio.GalactiCraft.cosmos

import kotlin.test.Test
import kotlin.test.assertEquals

class ElasticCollisionResolverTest {
    @Test
    fun `equal mass head-on collision exchanges velocity when perfectly elastic`() {
        val material = CollisionMaterial("test", restitution = 1.0)
        val left = CelestialBody("left", 1.0, 1.0, material, Vector3(0.0, 0.0, 0.0), Vector3(1.0, 0.0, 0.0))
        val right = CelestialBody("right", 1.0, 1.0, material, Vector3(2.0, 0.0, 0.0), Vector3(-1.0, 0.0, 0.0))

        val (resolvedLeft, resolvedRight) = requireNotNull(ElasticCollisionResolver.resolve(left, right))

        assertEquals(-1.0, resolvedLeft.velocity.x)
        assertEquals(1.0, resolvedRight.velocity.x)
    }
}
