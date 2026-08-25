package de.TeutonStudio.DynamicUniverse.compat.aether.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Keeps the Aether-only mixin configuration inert in installations without Aether. */
public final class OptionalAetherCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String AETHER_DIMENSION_HOOKS = "com.aetherteam.aether.event.hooks.DimensionHooks";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !mixinClassName.endsWith("AetherDimensionHooksMixin") || classExists(AETHER_DIMENSION_HOOKS);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, OptionalAetherCompatMixinPlugin.class.getClassLoader());
            return true;
        }
        catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
