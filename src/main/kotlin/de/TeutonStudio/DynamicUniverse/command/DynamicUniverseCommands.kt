package de.TeutonStudio.DynamicUniverse.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.TeutonStudio.DynamicUniverse.command.service.CommandFeedback
import de.TeutonStudio.DynamicUniverse.runtime.UniverseRuntime
import de.TeutonStudio.DynamicUniverse.runtime.UniverseTopology
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent

/** Thin Brigadier adapters for DynamicUniverse query and navigation services. */
object DynamicUniverseCommands {
    fun register() {
        NeoForge.EVENT_BUS.register(DynamicUniverseCommands::class.java)
    }

    @SubscribeEvent
    @JvmStatic
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        register(event.dispatcher)
    }

    internal fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("dynamicuniverse")
                .requires(CommandPermissions::mayInspect)
                .then(
                    Commands.literal("debug")
                        .executes { feedback(it.source, CommandFeedback(listOf("/dynamicuniverse debug inspect … | /dynamicuniverse debug goto layer <planet> <stack> <layer>"))) }
                        .then(
                            Commands.literal("inspect")
                                .then(Commands.literal("universe")
                                    .executes { feedback(it.source, DynamicUniverseCommandServices.universeQuery.inspectUniverse()) }
                                    // These values are intentionally vanilla string arguments. Custom Brigadier
                                    // argument types must have a registered network codec; otherwise Minecraft
                                    // rejects the complete command tree while placing a player in the world.
                                    .then(Commands.argument("universe", StringArgumentType.word()).suggests { _, builder ->
                                        suggest(builder, UniverseRuntime.api().universes().map { it.id })
                                    }
                                        .executes { feedback(it.source, DynamicUniverseCommandServices.universeQuery.inspectUniverse(StringArgumentType.getString(it, "universe"))) }))
                                .then(Commands.literal("planet")
                                    .then(Commands.argument("planet", StringArgumentType.word()).suggests { _, builder ->
                                        suggest(builder, UniverseTopology.planets(UniverseRuntime.api()).map(UniverseTopology::planetReference))
                                    }
                                        .executes { feedback(it.source, DynamicUniverseCommandServices.universeQuery.inspectPlanet(StringArgumentType.getString(it, "planet"))) }))
                                .then(Commands.literal("stack")
                                    .then(Commands.argument("planet", StringArgumentType.word()).suggests { _, builder ->
                                        suggest(builder, UniverseTopology.planets(UniverseRuntime.api()).map(UniverseTopology::planetReference))
                                    }
                                        .then(Commands.argument("stack", StringArgumentType.word()).suggests { context, builder ->
                                            val planet = StringArgumentType.getString(context, "planet")
                                            suggest(builder, UniverseTopology.resolvePlanet(UniverseRuntime.api(), planet)?.planet?.stacks?.map { it.id }.orEmpty())
                                        }
                                            .executes { feedback(it.source, DynamicUniverseCommandServices.universeQuery.inspectStack(
                                                StringArgumentType.getString(it, "planet"), StringArgumentType.getString(it, "stack"),
                                            )) })))
                                .then(Commands.literal("layer")
                                    .then(Commands.argument("planet", StringArgumentType.word())
                                        .then(Commands.argument("stack", StringArgumentType.word())
                                            .then(Commands.argument("layer", StringArgumentType.word()).suggests { context, builder ->
                                                val planet = StringArgumentType.getString(context, "planet")
                                                val stack = StringArgumentType.getString(context, "stack")
                                                suggest(builder, UniverseTopology.resolveStack(UniverseRuntime.api(), planet, stack)?.stack?.layersInnerToOuter?.map { it.id }.orEmpty())
                                            }
                                                .executes { feedback(it.source, DynamicUniverseCommandServices.universeQuery.inspectLayer(
                                                    StringArgumentType.getString(it, "planet"), StringArgumentType.getString(it, "stack"), StringArgumentType.getString(it, "layer"),
                                                )) }))))
                                .then(Commands.literal("position").executes {
                                    val player = it.source.entity as? ServerPlayer
                                        ?: return@executes feedback(it.source, CommandFeedback(listOf("This command requires a player."), false))
                                    feedback(it.source, DynamicUniverseCommandServices.universeQuery.inspectPosition(
                                        player.level().dimension().location().toString(), player.x, player.y, player.z,
                                    ))
                                }),
                        )
                        .then(
                            Commands.literal("goto")
                                .requires(CommandPermissions::mayNavigate)
                                .then(Commands.literal("layer")
                                    .then(Commands.argument("planet", StringArgumentType.word())
                                        .then(Commands.argument("stack", StringArgumentType.word())
                                            .then(Commands.argument("layer", StringArgumentType.word()).suggests { context, builder ->
                                                val planet = StringArgumentType.getString(context, "planet")
                                                val stack = StringArgumentType.getString(context, "stack")
                                                suggest(builder, UniverseTopology.resolveStack(UniverseRuntime.api(), planet, stack)?.stack?.layersInnerToOuter?.map { it.id }.orEmpty())
                                            }
                                                .executes {
                                                    val player = it.source.entity as? ServerPlayer
                                                        ?: return@executes feedback(it.source, CommandFeedback(listOf("This command requires a player."), false))
                                                    feedback(it.source, DynamicUniverseCommandServices.dimensionNavigation.moveToLayer(
                                                        player,
                                                        StringArgumentType.getString(it, "planet"),
                                                        StringArgumentType.getString(it, "stack"),
                                                        StringArgumentType.getString(it, "layer"),
                                                    ))
                                                })))),
                        ),
                ),
        )
    }

    private fun feedback(source: CommandSourceStack, result: CommandFeedback): Int {
        result.lines.forEach { line -> source.sendSuccess({ Component.literal(line) }, false) }
        return if (result.successful) 1 else 0
    }

    private fun suggest(
        builder: com.mojang.brigadier.suggestion.SuggestionsBuilder,
        values: Iterable<String>,
    ): java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> {
        val prefix = builder.remainingLowerCase
        values.filter { it.lowercase().startsWith(prefix) }.forEach(builder::suggest)
        return builder.buildFuture()
    }
}
