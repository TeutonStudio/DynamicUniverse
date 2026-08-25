package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.client.worldcreation.UniverseWorldCreationDraft
import de.TeutonStudio.DynamicUniverse.client.worldcreation.toLevelStemPlan
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UniverseLevelStemPlanTest {
    @Test
    fun `data preset fallback exactly matches the unedited Terra draft`() {
        assertEquals(UniverseDefaultWorldType.plan, UniverseWorldCreationDraft().toLevelStemPlan())
    }

    @Test
    fun `default Terra plan preserves nether undergarden and aether stems`() {
        val plan = UniverseWorldCreationDraft().toLevelStemPlan()

        assertTrue(plan.templates.containsKey(DimensionId("dynamicuniverse:created/planet/0/0/0/core")))
        assertTrue(plan.templates.containsKey(DimensionId("minecraft:the_nether")))
        assertEquals(UniverseStemTemplate.EXTERNAL, plan.templates[DimensionId("undergarden:undergarden")])
        assertTrue(plan.templates.containsKey(DimensionId("minecraft:overworld")))
        assertEquals(UniverseStemTemplate.EXTERNAL, plan.templates[DimensionId("aether:the_aether")])
        assertTrue(plan.templates.containsKey(DimensionId("dynamicuniverse:created/universe")))

        val corePlanes = plan.bedrockPlanes.filter { it.dimension.value == "dynamicuniverse:created/planet/0/0/0/core" }
        assertTrue(corePlanes.isEmpty())
        val undergardenPlanes = plan.bedrockPlanes.filter { it.dimension.value == "undergarden:undergarden" }
        assertEquals(setOf(DimensionBoundaryFace.LOWER, DimensionBoundaryFace.UPPER), undergardenPlanes.map { it.face }.toSet())
    }
}
