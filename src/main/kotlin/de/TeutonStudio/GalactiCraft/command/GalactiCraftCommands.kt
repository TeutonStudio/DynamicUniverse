package de.TeutonStudio.GalactiCraft.command

import com.mojang.brigadier.CommandDispatcher
import de.TeutonStudio.GalactiCraft.planet.PlanetSpawnPolicy
import de.TeutonStudio.GalactiCraft.planet.StandardPlanetTemplates
import de.TeutonStudio.GalactiCraft.runtime.DynamicDimensionStackSpawner
import de.TeutonStudio.GalactiCraft.runtime.CosmosRuntime
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object GalactiCraftCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("galacticraft")
                .requires { it.hasPermission(2) }
                .then(Commands.literal("planet")
                    .then(Commands.literal("list")
                        .executes { context ->
                            context.source.sendSuccess(
                                { Component.literal("Available planet template: ${StandardPlanetTemplates.earth.id}") },
                                false,
                            )
                            1
                        },
                    )
                    .then(Commands.literal("spawn-earth")
                        .executes { context -> spawnEarth(context.source) },
                    ),
                ),
        )
    }

    private fun spawnEarth(source: CommandSourceStack): Int {
        check(!PlanetSpawnPolicy.ADMINISTRATIVE_ONLY.permitsSurvivalCreation())
        val template = StandardPlanetTemplates.earth
        val dimensions = DynamicDimensionStackSpawner().loadPlanet(source.server, template)
        CosmosRuntime.register(source.server, template.body)
        source.sendSuccess(
            { Component.literal("Earth stack loaded: ${dimensions.joinToString()}") },
            true,
        )
        return dimensions.size
    }
}
