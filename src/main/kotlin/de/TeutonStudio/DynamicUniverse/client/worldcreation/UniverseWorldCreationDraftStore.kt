package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import de.TeutonStudio.DynamicUniverse.runtime.UniverseWorldCreationSession
import java.util.WeakHashMap

/** Keeps the draft tied to exactly one open Minecraft Create World screen. */
object UniverseWorldCreationDraftStore {
    private val drafts = WeakHashMap<CreateWorldScreen, UniverseWorldCreationDraft>()
    private val frozenWorldTypes = WeakHashMap<CreateWorldScreen, de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldType>()

    fun get(screen: CreateWorldScreen): UniverseWorldCreationDraft =
        drafts.getOrPut(screen, ::UniverseWorldCreationDraft)

    fun update(screen: CreateWorldScreen, transform: (UniverseWorldCreationDraft) -> UniverseWorldCreationDraft) {
        drafts[screen] = transform(get(screen))
        frozenWorldTypes.remove(screen)
    }

    /** The complete vertical stack is frozen before the Create World flow continues. */
    fun freezeWorldType(screen: CreateWorldScreen) = frozenWorldTypes.getOrPut(screen) { get(screen).toWorldType() }

    /**
     * The only point at which Minecraft still accepts additional level stems for a new world.
     * The request is consumed on the integrated server's first start and persisted there.
     */
    fun prepareServerCreation(screen: CreateWorldScreen) {
        val plan = get(screen).toLevelStemPlan()
        screen.uiState.updateDimensions { registries, dimensions -> UniverseLevelStemFactory.install(registries, dimensions, plan) }
        UniverseWorldCreationSession.arm(screen.uiState.targetFolder, plan)
    }
}
