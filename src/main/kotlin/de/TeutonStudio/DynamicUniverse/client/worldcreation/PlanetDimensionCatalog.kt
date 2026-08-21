package de.TeutonStudio.DynamicUniverse.client.worldcreation

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface

/**
 * The evidence behind a dimension's vertical boundary declaration. A real mod adapter
 * may inspect generator settings or provide this information directly. Guesses are never
 * allowed to become part of a created planet stack.
 */
enum class BoundaryEvidence { VERIFIED, UNRESOLVED, ISOLATED }

enum class DimensionCatalogStatus { VERIFIED, DISCOVERED, ISOLATED }

enum class RegisteredDimensionKind { CORE, SHELL, SURFACE, SKY, ISOLATED }

data class DimensionBoundaryInspection(
    val lower: BoundarySurface?,
    val upper: BoundarySurface?,
    val evidence: BoundaryEvidence,
) {
    val isResolved: Boolean get() = evidence == BoundaryEvidence.VERIFIED
}

/** Bridge implemented by a dimension generator or compatibility module. */
fun interface DimensionBoundaryInspectionAdapter {
    fun inspect(templateId: String): DimensionBoundaryInspection
}

data class EnterableDimensionTemplate(val id: String, val displayName: String)

/** A selectable technical template, never a shared ServerLevel instance. */
data class RegisteredDimensionDescriptor(
    val id: String,
    val displayName: String,
    val catalogStatus: DimensionCatalogStatus,
    val inspection: DimensionBoundaryInspection,
) {
    val kind: RegisteredDimensionKind = classify(inspection)
    val isSelectable: Boolean get() = catalogStatus != DimensionCatalogStatus.ISOLATED
    val blocksWorldCreation: Boolean get() = !inspection.isResolved

    init {
        require(id.matches(Regex("[a-z0-9_.-]+:[a-z0-9_./-]+"))) { "Dimension descriptor id must be namespaced." }
        require(kind != RegisteredDimensionKind.ISOLATED || catalogStatus == DimensionCatalogStatus.ISOLATED) {
            "Only isolated dimensions may use an isolated boundary inspection."
        }
    }

    private fun classify(inspection: DimensionBoundaryInspection): RegisteredDimensionKind = when {
        inspection.evidence == BoundaryEvidence.ISOLATED -> RegisteredDimensionKind.ISOLATED
        inspection.lower == null && inspection.upper == BoundarySurface.BEDROCK -> RegisteredDimensionKind.CORE
        inspection.lower == BoundarySurface.BEDROCK && inspection.upper == BoundarySurface.BEDROCK -> RegisteredDimensionKind.SHELL
        inspection.lower == BoundarySurface.BEDROCK && inspection.upper == BoundarySurface.AIR -> RegisteredDimensionKind.SURFACE
        inspection.lower == BoundarySurface.AIR && inspection.upper == BoundarySurface.AIR -> RegisteredDimensionKind.SKY
        inspection.lower == BoundarySurface.AIR && inspection.upper == BoundarySurface.BEDROCK ->
            throw IllegalArgumentException("AIR-to-BEDROCK dimensions are forbidden in alpha0.sol.terra.")
        else -> throw IllegalArgumentException("A planet dimension needs a complete, supported boundary inspection.")
    }
}

/**
 * Catalogs are supplied by the loaded game and its compatibility adapters. The built-ins
 * make a standalone Universe world usable; discovered mod dimensions may be selected but
 * require verified boundary evidence before Create World may finish.
 */
class PlanetDimensionCatalog(descriptors: Collection<RegisteredDimensionDescriptor>) {
    private val byId = descriptors.associateBy(RegisteredDimensionDescriptor::id)

    init {
        require(byId.size == descriptors.size) { "Dimension descriptor ids must be unique." }
        require(byId.values.count { it.kind == RegisteredDimensionKind.CORE } >= 1) { "A catalog needs a core template." }
        require(byId.values.count { it.kind == RegisteredDimensionKind.SURFACE } >= 1) { "A catalog needs a surface template." }
        require(byId.values.count { it.kind == RegisteredDimensionKind.SKY } >= 1) { "A catalog needs a sky template." }
    }

    fun require(id: String): RegisteredDimensionDescriptor =
        requireNotNull(byId[id]) { "Unknown registered dimension descriptor: $id" }

    fun selectable(): List<RegisteredDimensionDescriptor> = byId.values.filter(RegisteredDimensionDescriptor::isSelectable)

    fun selectableFor(index: Int, layerCount: Int): List<RegisteredDimensionDescriptor> = selectable().filter { descriptor ->
        when (index) {
            0 -> descriptor.kind == RegisteredDimensionKind.CORE
            layerCount - 1 -> descriptor.kind == RegisteredDimensionKind.SKY
            else -> descriptor.kind != RegisteredDimensionKind.CORE
        }
    }

    fun insertionCandidates(lower: EditableDimension, upper: EditableDimension): List<RegisteredDimensionDescriptor> =
        selectable().filter { candidate ->
            candidate.inspection.lower == lower.outerBoundarySurface && candidate.inspection.upper == upper.innerBoundarySurface
        }

    companion object {
        const val CORE_ID = "dynamicuniverse:planet_core"
        const val DEEP_NETHER_ID = "dynamicuniverse:deep_nether"
        const val NETHER_ID = "minecraft:the_nether"
        const val OVERWORLD_ID = "minecraft:overworld"
        const val SKY_ID = "dynamicuniverse:sky"
        const val END_ID = "minecraft:the_end"

        val standard = PlanetDimensionCatalog(
            listOf(
                descriptor(CORE_ID, "Planetenkern", DimensionCatalogStatus.VERIFIED, null, BoundarySurface.BEDROCK),
                descriptor(DEEP_NETHER_ID, "Tiefer Nether", DimensionCatalogStatus.VERIFIED, BoundarySurface.BEDROCK, BoundarySurface.BEDROCK),
                descriptor(NETHER_ID, "Nether", DimensionCatalogStatus.VERIFIED, BoundarySurface.BEDROCK, BoundarySurface.BEDROCK),
                descriptor(OVERWORLD_ID, "Oberwelt", DimensionCatalogStatus.VERIFIED, BoundarySurface.BEDROCK, BoundarySurface.AIR),
                descriptor(SKY_ID, "Himmel", DimensionCatalogStatus.VERIFIED, BoundarySurface.AIR, BoundarySurface.AIR),
                RegisteredDimensionDescriptor(
                    END_ID,
                    "End – eigenes Universum",
                    DimensionCatalogStatus.ISOLATED,
                    DimensionBoundaryInspection(null, null, BoundaryEvidence.ISOLATED),
                ),
            ),
        )

        fun discovered(id: String, displayName: String): RegisteredDimensionDescriptor =
            RegisteredDimensionDescriptor(
                id,
                displayName,
                DimensionCatalogStatus.DISCOVERED,
                DimensionBoundaryInspection(BoundarySurface.AIR, BoundarySurface.AIR, BoundaryEvidence.UNRESOLVED),
            )

        private fun descriptor(
            id: String,
            name: String,
            status: DimensionCatalogStatus,
            lower: BoundarySurface?,
            upper: BoundarySurface?,
        ) = RegisteredDimensionDescriptor(id, name, status, DimensionBoundaryInspection(lower, upper, BoundaryEvidence.VERIFIED))
    }
}

/**
 * Loader-owned catalog snapshot. Compatibility code replaces it once the loaded game's
 * dimension registry and generator adapters have been inspected; a creation draft then
 * uses only that snapshot until it is frozen.
 */
object PlanetDimensionCatalogs {
    @Volatile
    var current: PlanetDimensionCatalog = PlanetDimensionCatalog.standard
        private set

    fun install(inspectedCatalog: PlanetDimensionCatalog) {
        current = inspectedCatalog
    }

    fun resetForTests() {
        current = PlanetDimensionCatalog.standard
    }
}

/** Turns the loaded game's enterable dimensions into one immutable creation-time catalog. */
object PlanetDimensionCatalogBuilder {
    fun inspect(
        templates: Collection<EnterableDimensionTemplate>,
        adapter: DimensionBoundaryInspectionAdapter,
        isolatedTemplateIds: Set<String> = setOf(PlanetDimensionCatalog.END_ID),
    ): PlanetDimensionCatalog = PlanetDimensionCatalog(templates.map { template ->
        if (template.id in isolatedTemplateIds) {
            RegisteredDimensionDescriptor(
                template.id,
                "${template.displayName} – eigenes Universum",
                DimensionCatalogStatus.ISOLATED,
                DimensionBoundaryInspection(null, null, BoundaryEvidence.ISOLATED),
            )
        } else {
            val inspection = adapter.inspect(template.id)
            RegisteredDimensionDescriptor(
                template.id,
                template.displayName,
                if (inspection.isResolved) DimensionCatalogStatus.VERIFIED else DimensionCatalogStatus.DISCOVERED,
                inspection,
            )
        }
    })
}
