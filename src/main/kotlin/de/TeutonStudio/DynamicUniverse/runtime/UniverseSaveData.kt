package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionScale
import de.TeutonStudio.DynamicUniverse.cosmos.CelestialSpatialObject
import de.TeutonStudio.DynamicUniverse.cosmos.PlanetSpaceBinding
import de.TeutonStudio.DynamicUniverse.cosmos.SpatialRotation
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseKinematicState
import de.TeutonStudio.DynamicUniverse.cosmos.UniverseSpace
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
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
import de.TeutonStudio.DynamicUniverse.worldtype.VerticalDimensionSeam
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
    val runtimeState: UniverseRuntimeState = defaultRuntimeState(worldType),
) {
    init {
        require(formatVersion == CURRENT_FORMAT_VERSION) { "Unsupported DynamicUniverse save format: $formatVersion" }
        require(bedrockPlanes.map { it.dimension to it.face }.distinct().size == bedrockPlanes.size) {
            "A dimension boundary may only declare one generated Bedrock plane."
        }
    }

    companion object { const val CURRENT_FORMAT_VERSION = 3 }
}

private fun defaultRuntimeState(worldType: UniverseWorldType): UniverseRuntimeState {
    val geometry = UniverseGeometryCompiler.compile(worldType)
    val space = UniverseSpace(worldType.universeDimension.value)
    return UniverseRuntimeState(
        hostId = "${worldType.universeDimension.value}/host",
        universeSpaceId = space.id,
        // The persisted field keeps its historical name for compatibility, but now binds every
        // visible celestial body, including stars and moons.
        planetBindings = geometry.celestialGlobes.map { PlanetSpaceBinding(it.bodyId, "${it.bodyId}:local", space) },
        planetKinematics = geometry.celestialGlobes.map { PlanetKinematicState(it.bodyId, UniverseKinematicState(Vector3.ZERO)) },
    )
}

/** Compact NBT codec kept beside the save type so the persisted boundary remains explicit. */
object UniversePersistenceCodec {
    private const val VERSION = "version"
    private const val WORLD = "world"
    private const val PLANES = "planes"
    private const val RUNTIME = "runtime"

    fun encode(definition: PersistedUniverseDefinition): CompoundTag = CompoundTag().apply {
        putInt(VERSION, definition.formatVersion)
        put(WORLD, definition.worldType.toTag())
        put(PLANES, definition.bedrockPlanes.toTag { it.toTag() })
        put(RUNTIME, UniverseRuntimeStateCodec.encode(definition.runtimeState))
    }

    fun decode(tag: CompoundTag): PersistedUniverseDefinition {
        val worldType = tag.getCompound(WORLD).toWorldType()
        val version = tag.getInt(VERSION)
        require(version in 1..PersistedUniverseDefinition.CURRENT_FORMAT_VERSION) { "Unsupported DynamicUniverse save format: $version" }
        return PersistedUniverseDefinition(
            formatVersion = PersistedUniverseDefinition.CURRENT_FORMAT_VERSION,
            worldType = worldType,
            bedrockPlanes = tag.getList(PLANES, Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toBedrockPlane() },
            runtimeState = if (version >= 2 && tag.contains(RUNTIME, Tag.TAG_COMPOUND.toInt())) {
                UniverseRuntimeStateCodec.decode(tag.getCompound(RUNTIME))
            } else defaultRuntimeState(worldType),
        )
    }
}

/** NBT boundary for state that changes independently from immutable dimension geometry. */
object UniverseRuntimeStateCodec {
    private const val VERSION = "version"
    private const val HOST = "host"
    private const val SPACE = "space"
    private const val OBJECTS = "objects"
    private const val BINDINGS = "bindings"
    private const val KINEMATICS = "kinematics"
    private const val LAYOUT = "layout"

    fun encode(state: UniverseRuntimeState): CompoundTag = CompoundTag().apply {
        putInt(VERSION, state.formatVersion)
        putString(HOST, state.hostId)
        putString(SPACE, state.universeSpaceId)
        put(OBJECTS, state.objects.toTag { it.toTag() })
        put(BINDINGS, state.planetBindings.toTag { it.toTag() })
        put(KINEMATICS, state.planetKinematics.toTag { it.toTag() })
        put(LAYOUT, state.hostLayout.toTag())
    }

    fun decode(tag: CompoundTag): UniverseRuntimeState = UniverseRuntimeState(
        formatVersion = tag.getInt(VERSION), hostId = tag.getString(HOST), universeSpaceId = tag.getString(SPACE),
        objects = tag.getList(OBJECTS, Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toObject() },
        planetBindings = tag.getList(BINDINGS, Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toBinding() },
        planetKinematics = tag.getList(KINEMATICS, Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toPlanetKinematics() },
        hostLayout = tag.getCompound(LAYOUT).toHostLayout(),
    )
}

private fun Vector3.toTag() = CompoundTag().apply { putDouble("x", x); putDouble("y", y); putDouble("z", z) }
private fun CompoundTag.toVector3() = Vector3(getDouble("x"), getDouble("y"), getDouble("z"))
private fun UniverseKinematicState.toTag() = CompoundTag().apply {
    put("position", position.toTag()); put("velocity", velocity.toTag())
    putDouble("qx", orientation.x); putDouble("qy", orientation.y); putDouble("qz", orientation.z); putDouble("qw", orientation.w)
}
private fun CompoundTag.toKinematicState() = UniverseKinematicState(
    getCompound("position").toVector3(), getCompound("velocity").toVector3(),
    SpatialRotation.of(getDouble("qx"), getDouble("qy"), getDouble("qz"), getDouble("qw")),
)
private fun CelestialSpatialObject.toTag() = CompoundTag().apply {
    putString("id", this@toTag.id); putDouble("mass", mass); putDouble("radius", radius); putDouble("restitution", restitution); put("kinematics", kinematics.toTag())
}
private fun CompoundTag.toObject() = CelestialSpatialObject(getString("id"), getDouble("mass"), getCompound("kinematics").toKinematicState(), getDouble("radius"), getDouble("restitution"))
private fun PlanetSpaceBinding.toTag() = CompoundTag().apply {
    putString("planet", planetId); putString("local", localSpaceId); putString("space", universeSpace.id); putDouble("scale", localUnitsPerUniverseUnit)
}
private fun CompoundTag.toBinding() = PlanetSpaceBinding(getString("planet"), getString("local"), UniverseSpace(getString("space")), getDouble("scale"))
private fun PlanetKinematicState.toTag() = CompoundTag().apply { putString("planet", planetId); put("state", kinematics.toTag()) }
private fun CompoundTag.toPlanetKinematics() = PlanetKinematicState(getString("planet"), getCompound("state").toKinematicState())
private fun UniverseHostLayout.toTag() = CompoundTag().apply {
    put("bubbles", bubbles.toTag { hosted -> CompoundTag().apply { putString("slot", hosted.slot.value); put("origin", hosted.bubble.origin.toTag()); putDouble("radius", hosted.bubble.radius); putDouble("threshold", hosted.bubble.rebaseThreshold); putDouble("grid", hosted.bubble.rebaseGridSize) } })
    put("memberships", memberships.toTag { membership -> CompoundTag().apply { putString("object", membership.objectId); putString("slot", membership.slot.value) } })
    sablePlotyard?.let { put("plotyard", CompoundTag().apply { put("center", it.center.toTag()); putDouble("radius", it.radius) }) }
}
private fun CompoundTag.toHostLayout() = UniverseHostLayout(
    bubbles = getList("bubbles", Tag.TAG_COMPOUND.toInt()).map { tag -> tag.asCompound().let { HostedSimulationBubble(UniverseHostSlot(it.getString("slot")), SimulationBubble(it.getCompound("origin").toVector3(), it.getDouble("radius"), it.getDouble("threshold"), it.getDouble("grid"))) } },
    memberships = getList("memberships", Tag.TAG_COMPOUND.toInt()).map { tag -> tag.asCompound().let { BubbleMembership(it.getString("object"), UniverseHostSlot(it.getString("slot"))) } },
    sablePlotyard = takeIf { contains("plotyard", Tag.TAG_COMPOUND.toInt()) }?.getCompound("plotyard")?.let { SablePlotyardReservation(it.getCompound("center").toVector3(), it.getDouble("radius")) },
)

private fun UniverseWorldType.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    putString("universeDimension", this@toTag.universeDimension.value)
    put("galaxies", this@toTag.galaxies.toTag { it.toTag() })
    put("isolatedUniverses", this@toTag.isolatedUniverses.toTag { it.toTag() })
    put("verticalSeams", this@toTag.verticalSeams.toTag { it.toTag() })
}

private fun CompoundTag.toWorldType(): UniverseWorldType = UniverseWorldType(
    id = getString("id"),
    universeDimension = DimensionId(getString("universeDimension")),
    galaxies = getList("galaxies", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toGalaxy() },
    isolatedUniverses = getList("isolatedUniverses", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toIsolatedUniverse() },
    verticalSeams = takeIf { contains("verticalSeams", Tag.TAG_LIST.toInt()) }
        ?.getList("verticalSeams", Tag.TAG_COMPOUND.toInt())
        ?.map { it.asCompound().toVerticalSeam() }
        ?: listOf(VerticalDimensionSeam.overworldToAether()),
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

private fun VerticalDimensionSeam.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    putString("lower", lowerDimension.value)
    putString("upper", upperDimension.value)
    put("scale", coordinateScale.toTag())
}

private fun CompoundTag.toVerticalSeam() = VerticalDimensionSeam(
    id = getString("id"),
    lowerDimension = DimensionId(getString("lower")),
    upperDimension = DimensionId(getString("upper")),
    coordinateScale = getCompound("scale").toScale(),
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
