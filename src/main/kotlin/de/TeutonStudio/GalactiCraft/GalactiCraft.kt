package de.TeutonStudio.GalactiCraft

import de.TeutonStudio.GalactiCraft.command.GalactiCraftCommands
import de.TeutonStudio.GalactiCraft.planet.PlanetTemplateCatalog
import de.TeutonStudio.GalactiCraft.runtime.DynamicDimensionStackSpawner
import de.TeutonStudio.GalactiCraft.runtime.PlanetManifestData
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent

@Mod(GalactiCraft.MOD_ID)
class GalactiCraft(modBus: IEventBus) {
    init {
        NeoForge.EVENT_BUS.addListener(::registerCommands)
        NeoForge.EVENT_BUS.addListener(::reloadPersistedPlanets)
    }

    private fun registerCommands(event: RegisterCommandsEvent) {
        GalactiCraftCommands.register(event.dispatcher)
    }

    private fun reloadPersistedPlanets(event: ServerStartedEvent) {
        val manifest = PlanetManifestData.get(event.server)
        manifest.all().forEach { saved ->
            val template = PlanetTemplateCatalog.resolve(saved.templateId)
                ?: error("Saved planet template is unavailable: ${saved.templateId}")
            DynamicDimensionStackSpawner().loadPlanet(event.server, template)
        }
    }

    companion object {
        const val MOD_ID = "galacticraft"
    }
}
