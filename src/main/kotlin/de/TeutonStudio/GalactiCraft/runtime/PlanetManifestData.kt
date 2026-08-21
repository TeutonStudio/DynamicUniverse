package de.TeutonStudio.GalactiCraft.runtime

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

/**
 * Application-owned persistence required by DynamicDimensions. The dependency persists level
 * files but deliberately cannot know which dynamic levels form a GalactiCraft planet.
 */
class PlanetManifestData private constructor(
    private val planets: MutableMap<String, SpawnedPlanet>,
) : SavedData() {
    data class SpawnedPlanet(
        val templateId: String,
        val dimensionIds: List<String>,
    )

    fun remember(templateId: String, dimensionIds: List<String>) {
        planets[templateId] = SpawnedPlanet(templateId, dimensionIds)
        setDirty()
    }

    fun all(): Collection<SpawnedPlanet> = planets.values.toList()

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val entries = ListTag()
        planets.values.forEach { planet ->
            entries.add(CompoundTag().apply {
                putString("template", planet.templateId)
                val dimensions = ListTag()
                planet.dimensionIds.forEach { dimensions.add(StringTag.valueOf(it)) }
                put("dimensions", dimensions)
            })
        }
        tag.put("planets", entries)
        return tag
    }

    companion object {
        private const val SAVE_KEY = "galacticraft_planet_manifest"
        private val FACTORY = SavedData.Factory(
            { PlanetManifestData(mutableMapOf()) },
            { tag, _ -> load(tag) },
        )

        fun get(server: MinecraftServer): PlanetManifestData =
            server.overworld().dataStorage.computeIfAbsent(FACTORY, SAVE_KEY)

        private fun load(tag: CompoundTag): PlanetManifestData {
            val planets = tag.getList("planets", Tag.TAG_COMPOUND.toInt())
                .map { entry -> entry as CompoundTag }
                .mapNotNull { entry ->
                    val template = entry.getString("template")
                    if (template.isBlank()) return@mapNotNull null
                    val dimensions = entry.getList("dimensions", Tag.TAG_STRING.toInt()).map { it.asString }
                    SpawnedPlanet(template, dimensions)
                }
                .associateByTo(mutableMapOf()) { it.templateId }
            return PlanetManifestData(planets)
        }
    }
}
