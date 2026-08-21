package de.TeutonStudio.DynamicUniverse.worldtype

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.levelgen.presets.WorldPreset

/** Resource keys for DynamicUniverse world presets supplied through data resources. */
object UniverseWorldPresets {
    val UNIVERSE: ResourceKey<WorldPreset> = ResourceKey.create(
        Registries.WORLD_PRESET,
        ResourceLocation.fromNamespaceAndPath(DynamicUniverse.MOD_ID, "universe"),
    )
}
