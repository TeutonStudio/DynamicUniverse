package de.TeutonStudio.DynamicUniverse.runtime

import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource

/**
 * One-shot in-process hand-off from the integrated Create World UI to the server that it
 * starts. The actual dimension set is persisted by Minecraft in level.dat; this request only
 * survives until all of those ServerLevels are available and can be saved authoritatively.
 */
object UniverseWorldCreationSession {
    @Volatile
    private var pending: PendingUniverseCreation? = null

    fun arm(worldFolder: String, plan: UniverseLevelStemPlan) {
        require(worldFolder.isNotBlank()) { "A Universe creation needs a world folder." }
        pending = PendingUniverseCreation(worldFolder, plan)
    }

    fun consumeFor(server: MinecraftServer): UniverseLevelStemPlan? {
        val candidate = pending ?: return null
        val actualFolder = server.getWorldPath(LevelResource.ROOT).fileName?.toString() ?: return null
        if (actualFolder != candidate.worldFolder) return null
        pending = null
        return candidate.plan
    }

    fun clear() {
        pending = null
    }

    private data class PendingUniverseCreation(val worldFolder: String, val plan: UniverseLevelStemPlan)
}
