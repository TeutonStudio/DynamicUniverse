package de.TeutonStudio.DynamicUniverse.compat.aether.mixin;

import de.TeutonStudio.DynamicUniverse.compat.aether.AetherVerticalSeamTravelGuard;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Aether compatibility; the target class is never loaded without Aether. */
@Mixin(targets = "com.aetherteam.aether.event.hooks.DimensionHooks", remap = false)
abstract class AetherDimensionHooksMixin {
    @Inject(method = "dimensionTravel", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dynamicuniverse$suppressSeamTravelScreen(
        Entity entity,
        ResourceKey<Level> destination,
        CallbackInfo callback
    ) {
        if (AetherVerticalSeamTravelGuard.suppressStandardTravel(entity, destination)) {
            callback.cancel();
        }
    }
}
