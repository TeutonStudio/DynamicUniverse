package de.TeutonStudio.DynamicUniverse.client.worldcreation

import com.mojang.serialization.JsonOps
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.runtime.UniverseLevelStemPlan
import de.TeutonStudio.DynamicUniverse.runtime.UniverseStemTemplate
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.resources.RegistryOps
import net.minecraft.world.level.dimension.LevelStem
import net.minecraft.world.level.levelgen.WorldDimensions

/** Installs all dynamic level stems while the Create World screen can still alter WorldDimensions. */
object UniverseLevelStemFactory {
    fun install(registries: RegistryAccess.Frozen, base: WorldDimensions, plan: UniverseLevelStemPlan): WorldDimensions {
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

        val templateSources = mapOf(
            UniverseStemTemplate.OVERWORLD to overworld,
            UniverseStemTemplate.NETHER to nether,
            UniverseStemTemplate.CORE to core,
            UniverseStemTemplate.VOID to void,
        )
        val registeredStems = registries.registryOrThrow(Registries.LEVEL_STEM)

        // Replace the preset's default generated layers when the editor produced a different
        // universe. Leaving them in would create unreferenced, permanently loaded levels.
        val plannedKeys = plan.templates.keys.mapTo(mutableSetOf(), ::levelStemKey)
        dimensions.keys.removeIf { key ->
            val id = key.location()
            id.namespace == DynamicUniverse.MOD_ID && id.path.startsWith("created/") && key !in plannedKeys
        }

        plan.templates.forEach { (dimension, template) ->
            val key = levelStemKey(dimension)
            // The data preset already supplies stems for the default Universe. Preserve those
            // distinct instances: assigning one LevelStem to multiple keys makes bake() reject
            // the registry. New dimensions receive a registry-aware decoded copy instead.
            if (key !in dimensions) {
                dimensions[key] = when (template) {
                    UniverseStemTemplate.EXTERNAL -> requireNotNull(registeredStems.get(key)) {
                        "The selected Universe stack requires the external dimension ${dimension.value}, but its level stem is not registered."
                    }
                    else -> copyStem(registries, requireNotNull(templateSources[template]))
                }
            }
        }
        check(dimensions.values.toSet().size == dimensions.size) {
            "Universe level-stem installation produced duplicate LevelStem values."
        }
        return WorldDimensions(dimensions)
    }

    /** Decodes through registry-aware codecs to obtain a generator instance independent of its template. */
    private fun copyStem(registries: RegistryAccess.Frozen, template: LevelStem): LevelStem {
        val ops = RegistryOps.create(JsonOps.INSTANCE, registries)
        val encoded = LevelStem.CODEC.encodeStart(ops, template).getOrThrow { message ->
            throw IllegalStateException("Cannot encode a Universe level-stem template: $message")
        }
        return LevelStem.CODEC.parse(ops, encoded).getOrThrow { message ->
            throw IllegalStateException("Cannot decode a Universe level-stem template: $message")
        }
    }

    private fun levelStemKey(dimension: DimensionId): ResourceKey<LevelStem> = ResourceKey.create(
        Registries.LEVEL_STEM,
        requireNotNull(ResourceLocation.tryParse(dimension.value)) { "Invalid dynamic dimension id: ${dimension.value}" },
    )

    private const val DEFAULT_CORE_TEMPLATE = "dynamicuniverse:created/planet/0/0/0/core"
    private const val DEFAULT_VOID_TEMPLATE = "dynamicuniverse:created/planet/0/0/0/sky"
}
