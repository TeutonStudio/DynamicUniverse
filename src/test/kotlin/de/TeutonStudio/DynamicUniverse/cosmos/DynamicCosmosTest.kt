package de.TeutonStudio.DynamicUniverse.cosmos

import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicCosmosTest {
    @Test
    fun `elastic equal-mass head-on collision exchanges velocity`() {
        val cosmos = DynamicCosmos()
        cosmos.register(CelestialBody("a", 1.0, 1.0, Vector3(0.0, 0.0, 0.0), Vector3(1.0, 0.0, 0.0), 1.0))
        cosmos.register(CelestialBody("b", 1.0, 1.0, Vector3(2.0, 0.0, 0.0), Vector3(-1.0, 0.0, 0.0), 1.0))

        cosmos.tick(0.001)

        assertEquals(-1.0, cosmos.snapshot().first { it.id == "a" }.velocity.x, absoluteTolerance = 1e-10)
        assertEquals(1.0, cosmos.snapshot().first { it.id == "b" }.velocity.x, absoluteTolerance = 1e-10)
    }
}
