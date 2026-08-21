package de.TeutonStudio.GalactiCraft.runtime

import de.TeutonStudio.GalactiCraft.dimension.DimensionLayer
import de.TeutonStudio.GalactiCraft.planet.PlanetTemplate
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.LevelStem

/**
 * Maps a validated domain stack to live ServerLevels. We intentionally use load rather than
 * create: reloads preserve regions created during a previous server session.
 */
class DynamicDimensionStackSpawner {
    fun loadPlanet(server: MinecraftServer, planet: PlanetTemplate): List<ResourceLocation> {
        val registry = DynamicDimensionRegistry.from(server)
        val stems = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM)
        val prepared = planet.stacks.flatMap { stack ->
            stack.layersInnerToOuter.map { layer ->
                val target = ResourceLocation.fromNamespaceAndPath(
                    "galacticraft",
                    stack.runtimeDimensionPath(planet.id, layer),
                )
                require(registry.canCreateDimension(target) || registry.dynamicDimensionExists(target)) {
                    "Dimension id is already owned outside GalactiCraft: $target"
                }
                PreparedLayer(target, resolveStem(stems, layer))
            }
        }
        // No level is created until all ids, generation templates, and stack invariants validate.
        val loaded = prepared.map { preparedLayer ->
            if (!registry.dynamicDimensionExists(preparedLayer.id)) {
                registry.loadDynamicDimension(
                    preparedLayer.id,
                    preparedLayer.stem.generator(),
                    preparedLayer.stem.type().value(),
                ) ?: error("DynamicDimensions rejected ${preparedLayer.id}")
            }
            preparedLayer.id
        }
        PlanetManifestData.get(server).remember(planet.id, loaded.map(ResourceLocation::toString))
        return loaded
    }

    private data class PreparedLayer(val id: ResourceLocation, val stem: LevelStem)

    private fun resolveStem(
        stems: net.minecraft.core.Registry<LevelStem>,
        layer: DimensionLayer,
    ): LevelStem {
        val id = ResourceLocation.parse(layer.generationTemplate)
        val key = ResourceKey.create(Registries.LEVEL_STEM, id)
        return requireNotNull(stems.get(key)) { "Unknown generation template $id for layer ${layer.id}" }
    }
}
