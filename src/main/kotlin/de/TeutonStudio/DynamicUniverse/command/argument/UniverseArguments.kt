package de.TeutonStudio.DynamicUniverse.command.argument

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import de.TeutonStudio.DynamicUniverse.runtime.UniverseRuntime
import de.TeutonStudio.DynamicUniverse.runtime.UniverseTopology
import java.util.concurrent.CompletableFuture

/** Runtime-registry arguments. Their suggestions always reflect the active save. */
abstract class RuntimeIdArgument : ArgumentType<String> {
    override fun parse(reader: StringReader): String = reader.readUnquotedString()

    protected fun <S> suggest(builder: SuggestionsBuilder, values: Iterable<String>): CompletableFuture<Suggestions> {
        val prefix = builder.remainingLowerCase
        values.filter { it.lowercase().startsWith(prefix) }.forEach(builder::suggest)
        return builder.buildFuture()
    }
}

class UniverseArgument private constructor() : RuntimeIdArgument() {
    override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> =
        suggest<S>(builder, UniverseRuntime.api().universes().map { it.id })

    companion object { fun universe(): UniverseArgument = UniverseArgument() }
}

class PlanetArgument private constructor() : RuntimeIdArgument() {
    override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> =
        suggest<S>(builder, UniverseTopology.planets(UniverseRuntime.api()).map(UniverseTopology::planetReference))

    companion object { fun planet(): PlanetArgument = PlanetArgument() }
}

class DimensionStackArgument private constructor() : RuntimeIdArgument() {
    override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val planet = runCatching { context.getArgument("planet", String::class.java) }.getOrNull()
            ?: return builder.buildFuture()
        return suggest<S>(builder, UniverseTopology.resolvePlanet(UniverseRuntime.api(), planet)?.planet?.stacks?.map { it.id }.orEmpty())
    }

    companion object { fun stack(): DimensionStackArgument = DimensionStackArgument() }
}

class DimensionLayerArgument private constructor() : RuntimeIdArgument() {
    override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val planet = runCatching { context.getArgument("planet", String::class.java) }.getOrNull()
            ?: return builder.buildFuture()
        val stack = runCatching { context.getArgument("stack", String::class.java) }.getOrNull()
            ?: return builder.buildFuture()
        return suggest<S>(
            builder,
            UniverseTopology.resolveStack(UniverseRuntime.api(), planet, stack)?.stack?.layersInnerToOuter?.map { it.id }.orEmpty(),
        )
    }

    companion object { fun layer(): DimensionLayerArgument = DimensionLayerArgument() }
}
