package de.TeutonStudio.DynamicUniverse.client.worldcreation

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import java.util.WeakHashMap

/** Keeps the draft tied to exactly one open Minecraft Create World screen. */
object UniverseWorldCreationDraftStore {
    private val drafts = WeakHashMap<CreateWorldScreen, UniverseWorldCreationDraft>()

    fun get(screen: CreateWorldScreen): UniverseWorldCreationDraft =
        drafts.getOrPut(screen, ::UniverseWorldCreationDraft)

    fun update(screen: CreateWorldScreen, transform: (UniverseWorldCreationDraft) -> UniverseWorldCreationDraft) {
        drafts[screen] = transform(get(screen))
    }
}
