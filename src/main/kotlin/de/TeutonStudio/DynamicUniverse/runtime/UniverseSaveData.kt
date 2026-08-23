package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.worldtype.CelestialGroup
import de.TeutonStudio.DynamicUniverse.worldtype.CelestialGroupKind
import de.TeutonStudio.DynamicUniverse.worldtype.Galaxy
import de.TeutonStudio.DynamicUniverse.worldtype.IsolatedUniverseDefinition
import de.TeutonStudio.DynamicUniverse.worldtype.Planet
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionLayer
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionRole
import de.TeutonStudio.DynamicUniverse.worldtype.PlanetDimensionStack
import de.TeutonStudio.DynamicUniverse.worldtype.Star
import de.TeutonStudio.DynamicUniverse.worldtype.UniverseWorldType
import de.TeutonStudio.DynamicUniverse.worldtype.VerticalLoop
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

/**
 * The per-save, server-authoritative Universe definition. It deliberately stores logical
 * ids and generator-observed bedrock planes, never client state or portal entity UUIDs.
 */
class UniverseSaveData private constructor(
    var definition: PersistedUniverseDefinition? = null,
) : SavedData() {
    fun install(definition: PersistedUniverseDefinition) {
        require(this.definition == null) { "This save already owns a DynamicUniverse definition." }
        this.definition = definition
        setDirty()
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag = tag.apply {
        definition?.let { put(DEFINITION_KEY, UniversePersistenceCodec.encode(it)) }
    }

    companion object {
        private const val DATA_ID = "dynamicuniverse_universe"
        private const val DEFINITION_KEY = "definition"
        private val FACTORY = SavedData.Factory(::UniverseSaveData, { tag, _ ->
            UniverseSaveData(tag.takeIf { it.contains(DEFINITION_KEY, Tag.TAG_COMPOUND.toInt()) }
                ?.getCompound(DEFINITION_KEY)
                ?.let(UniversePersistenceCodec::decode))
        })

        /** Creates storage only for the successful new-world creation transaction. */
        fun createForServer(server: MinecraftServer): UniverseSaveData =
            server.overworld().dataStorage.computeIfAbsent(FACTORY, DATA_ID)

        /** Normal and failed creations must not allocate Universe save data at server start. */
        fun findForServer(server: MinecraftServer): UniverseSaveData? =
            server.overworld().dataStorage.get(FACTORY, DATA_ID)
    }
}

data class PersistedUniverseDefinition(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val worldType: UniverseWorldType,
    val bedrockPlanes: List<BedrockBoundaryPlane> = emptyList(),
) {
    init {
        require(formatVersion == CURRENT_FORMAT_VERSION) { "Unsupported DynamicUniverse save format: $formatVersion" }
        require(bedrockPlanes.map { it.dimension to it.face }.distinct().size == bedrockPlanes.size) {
            "A dimension boundary may only declare one generated Bedrock plane."
        }
    }

    companion object { const val CURRENT_FORMAT_VERSION = 1 }
}

/** Compact NBT codec kept beside the save type so the persisted boundary remains explicit. */
object UniversePersistenceCodec {
    private const val VERSION = "version"
    private const val WORLD = "world"
    private const val PLANES = "planes"

    fun encode(definition: PersistedUniverseDefinition): CompoundTag = CompoundTag().apply {
        putInt(VERSION, definition.formatVersion)
        put(WORLD, definition.worldType.toTag())
        put(PLANES, definition.bedrockPlanes.toTag { it.toTag() })
    }

    fun decode(tag: CompoundTag): PersistedUniverseDefinition = PersistedUniverseDefinition(
        formatVersion = tag.getInt(VERSION),
        worldType = tag.getCompound(WORLD).toWorldType(),
        bedrockPlanes = tag.getList(PLANES, Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toBedrockPlane() },
    )
}

private fun UniverseWorldType.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    putString("universeDimension", this@toTag.universeDimension.value)
    put("galaxies", this@toTag.galaxies.toTag { it.toTag() })
    put("isolatedUniverses", this@toTag.isolatedUniverses.toTag { it.toTag() })
}

private fun CompoundTag.toWorldType(): UniverseWorldType = UniverseWorldType(
    id = getString("id"),
    universeDimension = DimensionId(getString("universeDimension")),
    galaxies = getList("galaxies", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toGalaxy() },
    isolatedUniverses = getList("isolatedUniverses", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toIsolatedUniverse() },
)

private fun Galaxy.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    put("groups", this@toTag.groups.toTag { it.toTag() })
}

private fun CompoundTag.toGalaxy(): Galaxy = Galaxy(getString("id"), getList("groups", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toGroup() })

private fun CelestialGroup.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    putString("kind", this@toTag.kind.name)
    this@toTag.star?.let { put("star", it.toTag()) }
    put("planets", this@toTag.planets.toTag { it.toTag() })
}

private fun CompoundTag.toGroup(): CelestialGroup = CelestialGroup(
    id = getString("id"),
    kind = CelestialGroupKind.valueOf(getString("kind")),
    star = takeIf { contains("star", Tag.TAG_COMPOUND.toInt()) }?.getCompound("star")?.toStar(),
    planets = getList("planets", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toPlanet() },
)

private fun Star.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    put("stacks", this@toTag.stacks.toTag { it.toTag() })
}

private fun CompoundTag.toStar(): Star = Star(getString("id"), getList("stacks", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toStack() })

private fun Planet.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    putDouble("coreSize", this@toTag.planetCoreSize)
    put("stacks", this@toTag.stacks.toTag { it.toTag() })
    put("moons", this@toTag.moons.toTag { it.toTag() })
}

private fun CompoundTag.toPlanet(): Planet = Planet(
    id = getString("id"),
    planetCoreSize = getDouble("coreSize"),
    stacks = getList("stacks", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toStack() },
    moons = getList("moons", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toPlanet() },
)

private fun PlanetDimensionStack.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    put("layers", this@toTag.layersInnerToOuter.toTag { it.toTag() })
    put("outerScale", this@toTag.outerToUniverseScale.toTag())
}

private fun CompoundTag.toStack(): PlanetDimensionStack = PlanetDimensionStack(
    id = getString("id"),
    layersInnerToOuter = getList("layers", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toLayer() },
    outerToUniverseScale = getCompound("outerScale").toScale(),
)

private fun PlanetDimensionLayer.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    putString("role", this@toTag.role.name)
    putString("dimension", this@toTag.dimension.value)
    this@toTag.toOuterScale?.let { put("toOuterScale", it.toTag()) }
    this@toTag.innerBoundarySurface?.let { putString("innerBoundary", it.name) }
    putString("outerBoundary", requireNotNull(this@toTag.outerBoundarySurface).name)
}

private fun CompoundTag.toLayer(): PlanetDimensionLayer = PlanetDimensionLayer(
    id = getString("id"),
    role = PlanetDimensionRole.valueOf(getString("role")),
    dimension = DimensionId(getString("dimension")),
    toOuterScale = takeIf { contains("toOuterScale", Tag.TAG_COMPOUND.toInt()) }?.getCompound("toOuterScale")?.toScale(),
    innerBoundarySurface = takeIf { contains("innerBoundary", Tag.TAG_STRING.toInt()) }?.getString("innerBoundary")?.let(BoundarySurface::valueOf),
    outerBoundarySurface = BoundarySurface.valueOf(getString("outerBoundary")),
)

private fun IsolatedUniverseDefinition.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    putString("dimension", this@toTag.dimension.value)
    putString("verticalLoop", this@toTag.verticalLoop.name)
}

private fun CompoundTag.toIsolatedUniverse(): IsolatedUniverseDefinition = IsolatedUniverseDefinition(
    id = getString("id"),
    dimension = DimensionId(getString("dimension")),
    verticalLoop = VerticalLoop.valueOf(getString("verticalLoop")),
)

private fun BedrockBoundaryPlane.toTag(): CompoundTag = CompoundTag().apply {
    putString("dimension", dimension.value)
    putString("face", face.name)
    putInt("y", y)
}

private fun CompoundTag.toBedrockPlane() = BedrockBoundaryPlane(
    dimension = DimensionId(getString("dimension")),
    face = DimensionBoundaryFace.valueOf(getString("face")),
    y = getInt("y"),
)

private fun DimensionScale.toTag(): CompoundTag = CompoundTag().apply {
    putLong("numerator", numerator)
    putLong("denominator", denominator)
}

private fun CompoundTag.toScale() = DimensionScale(getLong("numerator"), getLong("denominator"))

private fun <T> Iterable<T>.toTag(encode: (T) -> CompoundTag): ListTag = ListTag().also { list -> forEach { list.add(encode(it)) } }
private fun Tag.asCompound(): CompoundTag = this as? CompoundTag ?: error("Expected an NBT compound")
