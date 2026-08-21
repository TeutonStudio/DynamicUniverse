package de.TeutonStudio.GalactiCraft.runtime

import de.TeutonStudio.GalactiCraft.cosmos.CelestialBody
import de.TeutonStudio.GalactiCraft.cosmos.CollisionMaterial
import de.TeutonStudio.GalactiCraft.cosmos.Vector3
import de.TeutonStudio.GalactiCraft.planet.PlanetTemplate
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
        val body: CelestialBody,
    )

    fun remember(template: PlanetTemplate, dimensionIds: List<String>) {
        val body = planets[template.id]?.body ?: template.body
        planets[template.id] = SpawnedPlanet(template.id, dimensionIds, body)
        setDirty()
    }

    fun all(): Collection<SpawnedPlanet> = planets.values.toList()

    fun updateBodies(bodies: Collection<CelestialBody>) {
        bodies.forEach { body ->
            val saved = planets[body.id] ?: return@forEach
            planets[body.id] = saved.copy(body = body)
        }
        setDirty()
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val entries = ListTag()
        planets.values.forEach { planet ->
            entries.add(CompoundTag().apply {
                putString("template", planet.templateId)
                val dimensions = ListTag()
                planet.dimensionIds.forEach { dimensions.add(StringTag.valueOf(it)) }
                put("dimensions", dimensions)
                put("body", encodeBody(planet.body))
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
                    val body = entry.getCompound("body").takeIf { !it.isEmpty }?.let(::decodeBody)
                        ?: CelestialBody.earth(template)
                    SpawnedPlanet(template, dimensions, body)
                }
                .associateByTo(mutableMapOf()) { it.templateId }
            return PlanetManifestData(planets)
        }

        private fun encodeBody(body: CelestialBody): CompoundTag = CompoundTag().apply {
            putString("id", body.id)
            putDouble("mass", body.mass)
            putDouble("radius", body.radius)
            putString("material", body.material.id)
            putDouble("restitution", body.material.restitution)
            putVector("position", body.position)
            putVector("velocity", body.velocity)
        }

        private fun decodeBody(tag: CompoundTag): CelestialBody = CelestialBody(
            id = tag.getString("id"),
            mass = tag.getDouble("mass"),
            radius = tag.getDouble("radius"),
            material = CollisionMaterial(tag.getString("material"), tag.getDouble("restitution")),
            position = tag.getVector("position"),
            velocity = tag.getVector("velocity"),
        )

        private fun CompoundTag.putVector(key: String, vector: Vector3) {
            put(key, CompoundTag().apply {
                putDouble("x", vector.x)
                putDouble("y", vector.y)
                putDouble("z", vector.z)
            })
        }

        private fun CompoundTag.getVector(key: String): Vector3 = getCompound(key).let {
            Vector3(it.getDouble("x"), it.getDouble("y"), it.getDouble("z"))
        }
    }
}
