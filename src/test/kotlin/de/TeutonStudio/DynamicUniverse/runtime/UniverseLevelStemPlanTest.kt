package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.client.worldcreation.UniverseWorldCreationDraft
import de.TeutonStudio.DynamicUniverse.client.worldcreation.toLevelStemPlan
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
    fun `default Terra plan keeps optional layers external and declares every verified Bedrock plane`() {
        val plan = UniverseWorldCreationDraft().toLevelStemPlan()

        assertTrue(plan.templates.containsKey(de.TeutonStudio.DynamicUniverse.dimension.DimensionId("dynamicuniverse:created/planet/0/0/0/core")))
        assertTrue(plan.templates.containsKey(de.TeutonStudio.DynamicUniverse.dimension.DimensionId("minecraft:the_nether")))
        assertTrue(plan.templates.containsKey(de.TeutonStudio.DynamicUniverse.dimension.DimensionId("undergarden:undergarden")))
        assertTrue(plan.templates.containsKey(de.TeutonStudio.DynamicUniverse.dimension.DimensionId("minecraft:overworld")))
        assertTrue(plan.templates.containsKey(de.TeutonStudio.DynamicUniverse.dimension.DimensionId("aether:the_aether")))
        assertTrue(plan.templates.containsKey(de.TeutonStudio.DynamicUniverse.dimension.DimensionId("dynamicuniverse:created/universe")))

        val corePlanes = plan.bedrockPlanes.filter { it.dimension.value == "dynamicuniverse:created/planet/0/0/0/core" }
        assertTrue(corePlanes.isEmpty())
        val netherPlanes = plan.bedrockPlanes.filter { it.dimension.value == "minecraft:the_nether" }
        assertEquals(setOf(DimensionBoundaryFace.LOWER, DimensionBoundaryFace.UPPER), netherPlanes.map { it.face }.toSet())
    }
}
