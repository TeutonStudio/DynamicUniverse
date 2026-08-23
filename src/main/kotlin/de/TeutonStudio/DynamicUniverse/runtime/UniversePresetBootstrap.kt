package de.TeutonStudio.DynamicUniverse.runtime

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer

/** Activates the static default Terra preset when it was created without opening Customize. */
object UniversePresetBootstrap {
    fun defaultPlanIfInstalled(server: MinecraftServer): UniverseLevelStemPlan? {
        val plan = UniverseDefaultWorldType.plan
        val allInstalled = plan.templates.keys.all { dimension ->
            val location = ResourceLocation.tryParse(dimension.value) ?: return null
            server.getLevel(ResourceKey.create(Registries.DIMENSION, location)) != null
        }
        return plan.takeIf { allInstalled }
    }
}
