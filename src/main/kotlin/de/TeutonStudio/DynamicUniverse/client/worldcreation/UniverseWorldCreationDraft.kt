package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaries
import de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryMismatch
import de.TeutonStudio.DynamicUniverse.dimension.PlanetDimensionStackValidator

/** Client-side draft used while the Create World screen is open. */
data class UniverseWorldCreationDraft(
    val universe: EditableUniverse = EditableUniverse.default(),
)

data class EditableUniverse(val galaxies: List<EditableGalaxy>) {
    init { require(galaxies.isNotEmpty()) { "Universe needs at least one galaxy" } }

    companion object {
        fun default() = EditableUniverse(listOf(
            EditableGalaxy("Lokale Gruppe", listOf(
                EditableSolarSystem("Sol", EditableStar("Sol"), listOf(EditablePlanet.default())),
                EditableCloud("Orion-Wolke"),
            )),
        ))
    }
}

data class EditableGalaxy(val name: String, val entries: List<EditableGalaxyEntry>)
sealed interface EditableGalaxyEntry { val name: String }
data class EditableSolarSystem(override val name: String, val star: EditableStar, val planets: List<EditablePlanet>) : EditableGalaxyEntry {
    init { require(planets.isNotEmpty()) { "A solar system needs at least one planet" } }
}
data class EditableCloud(override val name: String) : EditableGalaxyEntry
data class EditableStar(val name: String)

/** A celestial body which can own moons but never dimensions as child objects. */
data class EditablePlanet(
    val name: String,
    val settings: EditablePlanetSettings,
    val moons: List<EditableMoon> = emptyList(),
) {
    companion object {
        fun default() = EditablePlanet(
            name = "Terra",
            settings = PlanetPrefabRegistry.require(PlanetPrefabRegistry.EARTH_ID).createSettings(),
            moons = listOf(EditableMoon.default()),
        )
    }
}

/** A moon shares planet topology/settings, but cannot own further celestial bodies. */
data class EditableMoon(val name: String, val settings: EditablePlanetSettings) {
    companion object {
        fun default() = EditableMoon(
            name = "Luna",
            settings = PlanetPrefabRegistry.require(PlanetPrefabRegistry.MOON_ID).createSettings(),
        )
    }
}

/** Geometry and dimension order for a planet or moon; it is not cosmic hierarchy. */
data class EditablePlanetSettings(
    val dimensionTransitionFactor: Int,
    val coreSize: Int,
    val dimensions: List<EditablePlanetDimension>,
    val sourcePrefabId: String? = null,
) {
    init {
        require(dimensionTransitionFactor in MIN_TRANSITION_FACTOR..MAX_TRANSITION_FACTOR)
        require(coreSize in MIN_CORE_SIZE..MAX_CORE_SIZE)
        require(dimensions.count { it.kind == EditablePlanetDimensionKind.CORE } == 1) { "A body needs one core dimension." }
        require(dimensions.count { it.kind == EditablePlanetDimensionKind.SURFACE } == 1) { "A body needs one surface dimension." }
        require(dimensions.count { it.kind == EditablePlanetDimensionKind.SKY } == 1) { "A body needs one sky dimension." }
        require(dimensions.first().kind == EditablePlanetDimensionKind.CORE) { "The core must be the innermost dimension." }
        require(dimensions.last().kind == EditablePlanetDimensionKind.SKY) { "The sky must be the outermost dimension." }
        require(intermediateDimensionCount in MIN_INTERMEDIATE_DIMENSIONS..MAX_INTERMEDIATE_DIMENSIONS)
    }

    val intermediateDimensionCount: Int get() = dimensions.count { it.kind == EditablePlanetDimensionKind.INTERMEDIATE }
    /** Uses the shared runtime validator; descriptors own their boundary definitions. */
    val incompatibleDimensionTransitions: List<DimensionBoundaryMismatch>
        get() = PlanetDimensionStackValidator.incompatibleTransitions(dimensions.map(EditablePlanetDimension::boundaries))

    fun withIntermediateDimensionCount(count: Int) = copy(
        dimensions = defaultDimensions(count.coerceIn(MIN_INTERMEDIATE_DIMENSIONS, MAX_INTERMEDIATE_DIMENSIONS)),
    )
    fun withTransitionFactor(factor: Int) = copy(
        dimensionTransitionFactor = factor.coerceIn(MIN_TRANSITION_FACTOR, MAX_TRANSITION_FACTOR),
    )
    fun withCoreSize(size: Int) = copy(
        coreSize = size.coerceIn(MIN_CORE_SIZE, MAX_CORE_SIZE),
    )
    fun applyPrefab(prefab: PlanetPrefabDefinition): EditablePlanetSettings = prefab.createSettings()

    companion object {
        const val MIN_INTERMEDIATE_DIMENSIONS = 0
        const val MAX_INTERMEDIATE_DIMENSIONS = 8
        const val MIN_TRANSITION_FACTOR = 4
        const val MAX_TRANSITION_FACTOR = 64
        const val MIN_CORE_SIZE = 8
        const val MAX_CORE_SIZE = 128
        const val CORE_SIZE_STEP = 8

        fun default() = PlanetPrefabRegistry.require(PlanetPrefabRegistry.EARTH_ID).createSettings()
        fun defaultDimensions(intermediateDimensionCount: Int): List<EditablePlanetDimension> = buildList {
            add(EditablePlanetDimension("core", PlanetDimensionRegistry.CORE_ID))
            repeat(intermediateDimensionCount) { index -> add(EditablePlanetDimension("layer_${index + 1}", PlanetDimensionRegistry.INTERMEDIATE_ID)) }
            add(EditablePlanetDimension("surface", PlanetDimensionRegistry.SURFACE_ID))
            add(EditablePlanetDimension("sky", PlanetDimensionRegistry.SKY_ID))
        }
    }
}

enum class EditablePlanetDimensionKind { CORE, INTERMEDIATE, SURFACE, SKY }

/** An ordered stack entry references a registered descriptor rather than storing manual edge switches. */
data class EditablePlanetDimension(val id: String, val descriptorId: String) {
    private val descriptor: PlanetDimensionDescriptor get() = PlanetDimensionRegistry.require(descriptorId)
    val name: String get() = descriptor.name
    val kind: EditablePlanetDimensionKind get() = descriptor.kind
    val boundaries: DimensionBoundaries get() = descriptor.boundaries
}

data class PlanetDimensionDescriptor(
    val id: String,
    val name: String,
    val kind: EditablePlanetDimensionKind,
    val boundaries: DimensionBoundaries,
) {
    init {
        require((kind == EditablePlanetDimensionKind.SURFACE) == (boundaries.inner != boundaries.outer)) {
            "Only a surface descriptor may have BEDROCK on one edge and AIR on the other."
        }
    }
}

/** Stable dimension IDs offered by the world-creation editor. */
object PlanetDimensionRegistry {
    const val CORE_ID = "dynamicuniverse:planet_core"
    const val INTERMEDIATE_ID = "dynamicuniverse:underground"
    const val SURFACE_ID = "minecraft:overworld"
    const val SKY_ID = "dynamicuniverse:sky"

    private val descriptors = listOf(
        PlanetDimensionDescriptor(CORE_ID, "Planetenkern", EditablePlanetDimensionKind.CORE, DimensionBoundaries.BEDROCK_TO_BEDROCK),
        PlanetDimensionDescriptor(INTERMEDIATE_ID, "Untergrund", EditablePlanetDimensionKind.INTERMEDIATE, DimensionBoundaries.BEDROCK_TO_BEDROCK),
        PlanetDimensionDescriptor(SURFACE_ID, "Oberfläche", EditablePlanetDimensionKind.SURFACE, DimensionBoundaries.BEDROCK_TO_AIR),
        PlanetDimensionDescriptor(SKY_ID, "Himmel", EditablePlanetDimensionKind.SKY, DimensionBoundaries.AIR_TO_AIR),
    ).associateBy(PlanetDimensionDescriptor::id)

    fun require(id: String): PlanetDimensionDescriptor =
        requireNotNull(descriptors[id]) { "Unknown planet dimension descriptor: $id" }
}

/** A preset entry is copied into an editable stack whenever a preset is applied. */
data class PlanetPrefabDimension(val id: String, val descriptorId: String) {
    fun createEditableDimension() = EditablePlanetDimension(id, descriptorId)
}

/** Immutable preset data, intentionally copied rather than live-inherited by bodies. */
data class PlanetPrefabDefinition(
    val id: String,
    val name: String,
    val dimensionTransitionFactor: Int,
    val coreSize: Int,
    val dimensions: List<PlanetPrefabDimension>,
) {
    fun createSettings() = EditablePlanetSettings(
        dimensionTransitionFactor, coreSize, dimensions.map(PlanetPrefabDimension::createEditableDimension), id,
    )
}

/** Built-in starting points, comparable to Vanilla's Flat World presets. */
object PlanetPrefabRegistry {
    const val EARTH_ID = "dynamicuniverse:earth"
    const val ROCKY_ID = "dynamicuniverse:rocky"
    const val GAS_GIANT_ID = "dynamicuniverse:gas_giant"
    const val MOON_ID = "dynamicuniverse:moon"

    val all: List<PlanetPrefabDefinition> = listOf(
        preset(EARTH_ID, "Erde", 4, 32, 2),
        preset(ROCKY_ID, "Gesteinsplanet", 4, 24, 1),
        preset(GAS_GIANT_ID, "Gasriese", 8, 128, 4),
        preset(MOON_ID, "Mond", 4, 16, 0),
    )
    fun require(id: String): PlanetPrefabDefinition =
        requireNotNull(all.firstOrNull { it.id == id }) { "Unknown planet prefab: $id" }

    private fun preset(id: String, name: String, transitionFactor: Int, coreSize: Int, intermediateDimensions: Int) =
        PlanetPrefabDefinition(
            id, name, transitionFactor, coreSize,
            EditablePlanetSettings.defaultDimensions(intermediateDimensions).map { PlanetPrefabDimension(it.id, it.descriptorId) },
        )
}
