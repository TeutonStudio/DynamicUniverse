package de.TeutonStudio.GalactiCraft

import de.TeutonStudio.GalactiCraft.command.GalactiCraftCommands
import de.TeutonStudio.GalactiCraft.planet.PlanetTemplateCatalog
import de.TeutonStudio.GalactiCraft.runtime.DynamicDimensionStackSpawner
import de.TeutonStudio.GalactiCraft.runtime.CosmosRuntime
import de.TeutonStudio.GalactiCraft.runtime.PlanetManifestData
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

@Mod(GalactiCraft.MOD_ID)
class GalactiCraft(modBus: IEventBus) {
    init {
        NeoForge.EVENT_BUS.addListener(::registerCommands)
        NeoForge.EVENT_BUS.addListener(::reloadPersistedPlanets)
        NeoForge.EVENT_BUS.addListener(::tickCosmos)
        NeoForge.EVENT_BUS.addListener(::clearCosmos)
    }

    private fun registerCommands(event: RegisterCommandsEvent) {
        GalactiCraftCommands.register(event.dispatcher)
    }

    private fun reloadPersistedPlanets(event: ServerStartedEvent) {
        val manifest = PlanetManifestData.get(event.server)
        manifest.all().forEach { saved ->
            val template = PlanetTemplateCatalog.resolve(saved.templateId)
                ?: error("Saved planet template is unavailable: ${saved.templateId}")
            DynamicDimensionStackSpawner().loadPlanet(event.server, template, recordManifest = false)
            CosmosRuntime.register(event.server, saved.body)
        }
    }

    private fun tickCosmos(event: ServerTickEvent.Post) {
        CosmosRuntime.tick(event.server)
    }

    private fun clearCosmos(event: ServerStoppedEvent) {
        CosmosRuntime.clear(event.server)
    }

    companion object {
        const val MOD_ID = "galacticraft"
    }
}
