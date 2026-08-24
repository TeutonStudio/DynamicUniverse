package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPosition
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

class BoundaryApertureSaveData private constructor(
    private var nextSequence: Long = 1L,
    private val paired: MutableList<PairedBoundaryAperture> = mutableListOf(),
    private val cores: MutableList<CoreBoundaryAperture> = mutableListOf(),
) : SavedData() {
    fun pairedApertures(): List<PairedBoundaryAperture> = paired.toList()
    fun coreApertures(): List<CoreBoundaryAperture> = cores.toList()

    fun allocateIdentity(): Pair<String, Long> {
        val sequence = nextSequence++
        setDirty()
        return "aperture-$sequence" to sequence
    }

    fun put(aperture: PairedBoundaryAperture, removeIds: Set<String> = emptySet()) {
        paired.removeAll { it.id in removeIds || it.id == aperture.id }
        paired += aperture
        setDirty()
    }

    fun put(aperture: CoreBoundaryAperture, removeIds: Set<String> = emptySet()) {
        cores.removeAll { it.id in removeIds || it.id == aperture.id }
        cores += aperture
        setDirty()
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag = tag.apply {
        putInt("version", FORMAT_VERSION)
        putLong("nextSequence", nextSequence)
        put("paired", paired.toTag { it.toTag() })
        put("cores", cores.toTag { it.toTag() })
    }

    companion object {
        private const val DATA_ID = "dynamicuniverse_apertures"
        private const val FORMAT_VERSION = 1
        private val FACTORY = SavedData.Factory(
            ::BoundaryApertureSaveData,
            { tag, _ ->
                require(tag.getInt("version") == FORMAT_VERSION) {
                    "Unsupported DynamicUniverse aperture save version: ${tag.getInt("version")}"
                }
                BoundaryApertureSaveData(
                    nextSequence = tag.getLong("nextSequence").coerceAtLeast(1L),
                    paired = tag.getList("paired", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toPairedAperture() }.toMutableList(),
                    cores = tag.getList("cores", Tag.TAG_COMPOUND.toInt()).map { it.asCompound().toCoreAperture() }.toMutableList(),
                )
            },
        )

        fun forServer(server: MinecraftServer): BoundaryApertureSaveData =
            server.overworld().dataStorage.computeIfAbsent(FACTORY, DATA_ID)

        fun find(server: MinecraftServer): BoundaryApertureSaveData? =
            server.overworld().dataStorage.get(FACTORY, DATA_ID)
    }
}

private fun PairedBoundaryAperture.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    putString("connectionId", this@toTag.connectionId)
    putLong("createdSequence", this@toTag.createdSequence)
    put("sourceAnchor", this@toTag.sourceAnchor.toTag())
    put("targetAnchor", this@toTag.targetAnchor.toTag())
    put("shape", this@toTag.shape.toTag())
}

private fun CoreBoundaryAperture.toTag(): CompoundTag = CompoundTag().apply {
    putString("id", this@toTag.id)
    putString("connectionId", this@toTag.connectionId)
    putLong("createdSequence", this@toTag.createdSequence)
    putString("planetId", this@toTag.planetId)
    putString("deepDimension", this@toTag.deepDimension.value)
    putString("deepFace", this@toTag.deepFace.name)
    put("deepAnchor", this@toTag.deepAnchor.toTag())
    coreAnchor?.let { anchor ->
        putString("coreFace", anchor.face.name)
        putInt("coreU", anchor.u)
        putInt("coreV", anchor.v)
        putInt("coreRotation", requireNotNull(coreRotationQuarterTurns))
    }
    put("shape", this@toTag.shape.toTag())
}

private fun CompoundTag.toPairedAperture() = PairedBoundaryAperture(
    id = getString("id"),
    connectionId = getString("connectionId"),
    createdSequence = getLong("createdSequence"),
    sourceAnchor = getCompound("sourceAnchor").toHorizontalPosition(),
    targetAnchor = getCompound("targetAnchor").toHorizontalPosition(),
    shape = getList("shape", Tag.TAG_COMPOUND.toInt()).toApertureShape(),
)

private fun CompoundTag.toCoreAperture() = CoreBoundaryAperture(
    id = getString("id"),
    connectionId = getString("connectionId"),
    createdSequence = getLong("createdSequence"),
    planetId = getString("planetId"),
    deepDimension = DimensionId(getString("deepDimension")),
    deepFace = DimensionBoundaryFace.valueOf(getString("deepFace")),
    deepAnchor = getCompound("deepAnchor").toHorizontalPosition(),
    coreAnchor = getString("coreFace").takeIf(String::isNotBlank)?.let { face ->
        CoreShellCell(CoreShellFace.valueOf(face), getInt("coreU"), getInt("coreV"))
    },
    coreRotationQuarterTurns = getString("coreFace").takeIf(String::isNotBlank)?.let { getInt("coreRotation") },
    shape = getList("shape", Tag.TAG_COMPOUND.toInt()).toApertureShape(),
)

private fun HorizontalPosition.toTag(): CompoundTag = CompoundTag().apply {
    putLong("x", this@toTag.x)
    putLong("z", this@toTag.z)
}

private fun CompoundTag.toHorizontalPosition() = HorizontalPosition(getLong("x"), getLong("z"))

private fun ApertureShape.toTag(): ListTag = cells
    .sortedWith(compareBy<ApertureCell> { it.dx }.thenBy { it.dz })
    .toTag { cell -> CompoundTag().apply { putInt("dx", cell.dx); putInt("dz", cell.dz) } }

private fun ListTag.toApertureShape() = ApertureShape(map { tag ->
    val cell = tag.asCompound()
    ApertureCell(cell.getInt("dx"), cell.getInt("dz"))
}.toSet())

private fun <T> Iterable<T>.toTag(encode: (T) -> CompoundTag): ListTag =
    ListTag().also { list -> forEach { list.add(encode(it)) } }

private fun Tag.asCompound(): CompoundTag = this as? CompoundTag ?: error("Expected an NBT compound")
