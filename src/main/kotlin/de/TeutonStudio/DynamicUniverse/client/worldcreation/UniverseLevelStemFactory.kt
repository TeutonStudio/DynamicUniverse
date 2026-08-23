package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.runtime.UniverseLevelStemPlan
import de.TeutonStudio.DynamicUniverse.runtime.UniverseStemTemplate
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.dimension.LevelStem
import net.minecraft.world.level.levelgen.WorldDimensions

/** Installs all dynamic level stems while the Create World screen can still alter WorldDimensions. */
object UniverseLevelStemFactory {
    fun install(base: WorldDimensions, plan: UniverseLevelStemPlan): WorldDimensions {
        val dimensions = base.dimensions.toMutableMap()
        val overworld = requireNotNull(dimensions[LevelStem.OVERWORLD]) {
            "The selected world preset has no Overworld generator."
        }
        val nether = requireNotNull(dimensions[LevelStem.NETHER]) {
            "The selected world preset has no Nether generator."
        }
        val core = requireNotNull(dimensions[levelStemKey(DimensionId(DEFAULT_CORE_TEMPLATE))]) {
            "The selected Universe preset has no planet-core generator template."
        }
        val void = requireNotNull(dimensions[levelStemKey(DimensionId(DEFAULT_VOID_TEMPLATE))]) {
            "The selected Universe preset has no void generator template."
        }

        // Replace the preset's default generated layers when the editor produced a different
        // universe. Leaving them in would create unreferenced, permanently loaded levels.
        val plannedKeys = plan.templates.keys.mapTo(mutableSetOf(), ::levelStemKey)
        dimensions.keys.removeIf { key ->
            val id = key.location()
            id.namespace == DynamicUniverse.MOD_ID && id.path.startsWith("created/") && key !in plannedKeys
        }

        plan.templates.forEach { (dimension, template) ->
            dimensions[levelStemKey(dimension)] = when (template) {
                UniverseStemTemplate.OVERWORLD -> overworld
                UniverseStemTemplate.NETHER -> nether
                UniverseStemTemplate.CORE -> core
                UniverseStemTemplate.VOID -> void
            }
        }
        return WorldDimensions(dimensions)
    }

    private fun levelStemKey(dimension: DimensionId): ResourceKey<LevelStem> = ResourceKey.create(
        Registries.LEVEL_STEM,
        requireNotNull(ResourceLocation.tryParse(dimension.value)) { "Invalid dynamic dimension id: ${dimension.value}" },
    )

    private const val DEFAULT_CORE_TEMPLATE = "dynamicuniverse:created/planet/0/0/0/core"
    private const val DEFAULT_VOID_TEMPLATE = "dynamicuniverse:created/planet/0/0/0/sky"
}
