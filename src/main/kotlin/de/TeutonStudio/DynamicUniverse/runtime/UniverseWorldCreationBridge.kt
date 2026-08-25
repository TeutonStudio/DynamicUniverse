package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldType
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer

/**
 * The only server entry point for a newly created Universe world.
 *
 * A world-creation adapter must call [create] after its local dimensions have been installed
 * and their generator adapters have reported Bedrock planes. Normal server startup calls
 * [restore] instead. This keeps the client editor out of save and runtime ownership.
 */
object UniverseWorldCreationBridge {
    fun create(
        server: MinecraftServer,
        worldType: UniverseWorldType,
        bedrockPlanes: Collection<BedrockBoundaryPlane>,
    ): PersistedUniverseDefinition {
        val definition = PersistedUniverseDefinition(worldType = worldType, bedrockPlanes = bedrockPlanes.toList())
        requireInstalledLevels(server, definition.worldType)
        val saveData = UniverseSaveData.createForServer(server)
        saveData.install(definition)
        activate(server, definition)
        return definition
    }

    /** Restores a previously created Universe save; returns false for ordinary worlds. */
    fun restore(server: MinecraftServer): Boolean {
        val definition = UniverseSaveData.findForServer(server)?.definition ?: run {
            UniverseRuntime.clear()
            BedrockApertureRuntime.clear()
            UniverseTransitionRuntime.clear()
            return false
        }
        requireInstalledLevels(server, definition.worldType)
        activate(server, definition)
        return true
    }

    fun clear() {
        UniverseRuntime.clear()
        BedrockApertureRuntime.clear()
        UniverseTransitionRuntime.clear()
    }

    private fun activate(server: MinecraftServer, definition: PersistedUniverseDefinition) {
        val manifest = UniverseGeometryCompiler.compile(definition.worldType)
        UniverseRuntime.clear()
        UniverseRuntime.api().register(definition.worldType)
        // Geometry remains immutable; this separately persisted state owns positions, bubbles and compat reservations.
        UniverseRuntime.installState(definition.runtimeState)
        BedrockApertureRuntime.install(manifest, definition.bedrockPlanes)
        BedrockApertureRuntime.reconcile(server)
        UniverseTransitionRuntime.install(manifest)
    }

    /** A persisted plan may never be activated against a partially registered dimension set. */
    private fun requireInstalledLevels(server: MinecraftServer, worldType: UniverseWorldType) {
        val required = UniverseGeometryCompiler.compile(worldType).layers.map { it.dimension } + worldType.universeDimension
        val missing = required.distinct().filter { dimension ->
            val id = ResourceLocation.tryParse(dimension.value)
            id == null || server.getLevel(ResourceKey.create(Registries.DIMENSION, id)) == null
        }
        require(missing.isEmpty()) { "Universe save is missing registered dimensions: ${missing.joinToString { it.value }}" }
    }
}
