package de.TeutonStudio.DynamicUniverse

import de.TeutonStudio.DynamicUniverse.command.DynamicUniverseCommands
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod

@Mod(DynamicUniverse.MOD_ID)
class DynamicUniverse(modBus: IEventBus) {
    init {
        DynamicUniverseCommands.register()
    }

    companion object {
        const val MOD_ID = "dynamicuniverse"
    }
}
