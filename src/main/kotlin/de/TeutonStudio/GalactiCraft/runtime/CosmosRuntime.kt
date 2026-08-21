package de.TeutonStudio.GalactiCraft.runtime

import de.TeutonStudio.GalactiCraft.cosmos.CelestialBody
import de.TeutonStudio.GalactiCraft.cosmos.DynamicCosmos
import net.minecraft.server.MinecraftServer
import java.util.IdentityHashMap

/** Server-owned simulation state; it never exists on the logical client. */
object CosmosRuntime {
    private val simulations = IdentityHashMap<MinecraftServer, DynamicCosmos>()
    private val saveTicks = IdentityHashMap<MinecraftServer, Int>()

    fun register(server: MinecraftServer, body: CelestialBody) {
        val cosmos = simulations.getOrPut(server, ::DynamicCosmos)
        if (cosmos.bodies().none { it.id == body.id }) cosmos.register(body)
    }

    fun tick(server: MinecraftServer) {
        val cosmos = simulations[server] ?: return
        cosmos.tick(1.0 / 20.0)
        val ticks = (saveTicks[server] ?: 0) + 1
        saveTicks[server] = ticks
        if (ticks >= 20) {
            PlanetManifestData.get(server).updateBodies(cosmos.bodies())
            saveTicks[server] = 0
        }
    }

    fun clear(server: MinecraftServer) {
        simulations.remove(server)
        saveTicks.remove(server)
    }
}
