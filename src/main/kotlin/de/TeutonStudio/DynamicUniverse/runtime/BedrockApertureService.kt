package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.dimension.BoundarySurface
import com.mojang.logging.LogUtils
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnection
import de.TeutonStudio.DynamicUniverse.dimension.DimensionConnectionKind
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.dimension.DimensionPosition
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPosition
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

sealed interface BedrockBreakPreparation {
    data object Ignored : BedrockBreakPreparation
    data class Rejected(val reason: String) : BedrockBreakPreparation
    class Accepted(private val operation: () -> Boolean) : BedrockBreakPreparation {
        fun commit(): Boolean = operation()
    }
}

class ServerBedrockApertureBridge(
    private val manifest: UniverseGeometryManifest,
    planes: Collection<BedrockBoundaryPlane>,
    private val coreResolver: PlanetCoreProjectionResolver = PlanetCoreProjectionResolver(),
) {
    private val planeByEndpoint = planes.associateBy { it.dimension to it.face }
    private val periodByDimension = manifest.layers.associate { it.dimension to it.period }
    private val coreByConnection = manifest.planetCores.associateBy(PlanetCoreGeometry::connectionId)

    init {
        require(planeByEndpoint.size == planes.size) { "A dimension boundary may only declare one Bedrock plane." }
    }

    fun prepareBedrockBreak(sourceLevel: ServerLevel, sourcePos: BlockPos): BedrockBreakPreparation {
        if (!sourceLevel.getBlockState(sourcePos).`is`(Blocks.BEDROCK)) return BedrockBreakPreparation.Ignored
        val dimension = DimensionId(sourceLevel.dimension().location().toString())
        val resolved = resolveBoundary(dimension, sourcePos) ?: return BedrockBreakPreparation.Ignored
        val core = coreByConnection[resolved.connection.id]
        return if (core != null) {
            when (dimension) {
                core.deepDimension -> prepareCoreBreak(sourceLevel, sourcePos, resolved, core)
                core.coreDimension -> prepareCoreBreakFromCore(sourceLevel, sourcePos, resolved, core)
                else -> BedrockBreakPreparation.Ignored
            }
        } else {
            preparePairedBreak(sourceLevel, sourcePos, resolved)
        }
    }

    fun reconcileCoreProjections(server: MinecraftServer) {
        val save = BoundaryApertureSaveData.find(server) ?: return
        manifest.planetCores.forEach { geometry ->
            val apertures = save.coreApertures().filter { it.connectionId == geometry.connectionId }
            if (apertures.isEmpty()) return@forEach
            val projections = coreResolver.resolve(geometry, apertures) ?: return@forEach
            val level = server.level(geometry.coreDimension) ?: return@forEach
            projections.values.flatMap { projection ->
                coreResolver.blockPositions(geometry, projection).orEmpty()
            }.forEach { position ->
                level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)
            }
        }
    }

    private fun preparePairedBreak(
        sourceLevel: ServerLevel,
        sourcePos: BlockPos,
        resolved: ResolvedBoundary,
    ): BedrockBreakPreparation {
        val connection = resolved.connection
        val sourcePeriod = periodByDimension[connection.source]
            ?: return BedrockBreakPreparation.Rejected("Missing source toroidal period.")
        val targetPeriod = periodByDimension[connection.target]
            ?: return BedrockBreakPreparation.Rejected("Missing target toroidal period.")
        val oppositePlane = planeByEndpoint[
            (if (resolved.atConnectionSource) connection.target else connection.source) to
                (if (resolved.atConnectionSource) connection.targetBoundaryFace else connection.sourceBoundaryFace)
        ] ?: return BedrockBreakPreparation.Rejected("Missing opposite Bedrock plane.")

        val brokenPeriod = if (resolved.atConnectionSource) sourcePeriod else targetPeriod
        val broken = brokenPeriod.canonical(HorizontalPosition(sourcePos.x.toLong(), sourcePos.z.toLong()))
        val save = BoundaryApertureSaveData.forServer(sourceLevel.server)
        val existing = save.pairedApertures().filter { it.connectionId == connection.id }
        val touching = existing.filter { aperture ->
            val anchor = if (resolved.atConnectionSource) aperture.sourceAnchor else aperture.targetAnchor
            val offset = brokenPeriod.offset(anchor, broken)
            aperture.shape.touches(offset) || aperture.shape.contains(offset)
        }

        val planned = when {
            touching.isEmpty() -> newPaired(save, connection, sourcePeriod, targetPeriod, broken, resolved.atConnectionSource)
            touching.size == 1 -> extendPaired(touching.single(), sourcePeriod, targetPeriod, broken, resolved.atConnectionSource)
            else -> mergePaired(touching, sourcePeriod, targetPeriod, broken, resolved.atConnectionSource)
                ?: return BedrockBreakPreparation.Rejected("This block would merge apertures with incompatible local mappings.")
        }

        val counterpart = counterpartFor(planned.aperture, sourcePeriod, targetPeriod, broken, resolved.atConnectionSource)
        val targetLevel = sourceLevel.server.level(
            if (resolved.atConnectionSource) connection.target else connection.source
        ) ?: return BedrockBreakPreparation.Rejected("Target dimension is not loaded.")
        val targetPos = counterpart.toBlockPos(oppositePlane.y)
            ?: return BedrockBreakPreparation.Rejected("Mapped target is outside Minecraft BlockPos range.")
        if (!targetLevel.getBlockState(targetPos).`is`(Blocks.BEDROCK)) {
            return BedrockBreakPreparation.Rejected("Mapped counterpart is not Bedrock.")
        }

        val sourceState = sourceLevel.getBlockState(sourcePos)
        val targetState = targetLevel.getBlockState(targetPos)
        return BedrockBreakPreparation.Accepted {
            atomicBlockChange(
                listOf(
                    BlockMutation(sourceLevel, sourcePos, sourceState, Blocks.AIR.defaultBlockState()),
                    BlockMutation(targetLevel, targetPos, targetState, Blocks.AIR.defaultBlockState()),
                ),
            ) {
                save.put(planned.aperture, planned.removeIds)
                AperturePortalRuntime.rebuildPaired(sourceLevel.server, manifest, planned.aperture)
            }
        }
    }

    private fun prepareCoreBreak(
        deepLevel: ServerLevel,
        sourcePos: BlockPos,
        resolved: ResolvedBoundary,
        geometry: PlanetCoreGeometry,
    ): BedrockBreakPreparation {
        val deepPeriod = periodByDimension[geometry.deepDimension]
            ?: return BedrockBreakPreparation.Rejected("Missing deep-layer toroidal period.")
        val broken = deepPeriod.canonical(HorizontalPosition(sourcePos.x.toLong(), sourcePos.z.toLong()))
        val save = BoundaryApertureSaveData.forServer(deepLevel.server)
        val oldApertures = save.coreApertures().filter { it.connectionId == geometry.connectionId }
        val touching = oldApertures.filter { aperture ->
            val offset = deepPeriod.offset(aperture.deepAnchor, broken)
            aperture.shape.touches(offset) || aperture.shape.contains(offset)
        }

        val planned = when {
            touching.isEmpty() -> {
                val (id, sequence) = save.allocateIdentity()
                PlannedCore(
                    CoreBoundaryAperture(
                        id = id,
                        connectionId = resolved.connection.id,
                        createdSequence = sequence,
                        planetId = geometry.planetId,
                        deepDimension = geometry.deepDimension,
                        deepFace = resolved.deepFace,
                        deepAnchor = broken,
                    ),
                )
            }
            touching.size == 1 -> {
                val aperture = touching.single()
                val offset = deepPeriod.offset(aperture.deepAnchor, broken)
                PlannedCore(aperture.copy(shape = aperture.shape.with(offset)))
            }
            else -> mergeCore(touching, deepPeriod, broken)
        }

        val nextApertures = oldApertures.filterNot { it.id in planned.removeIds || it.id == planned.aperture.id } + planned.aperture
        val oldProjection = coreResolver.resolve(geometry, oldApertures)
            ?: return BedrockBreakPreparation.Rejected("Existing core aperture projection is invalid.")
        val nextProjection = coreResolver.resolve(geometry, nextApertures)
            ?: return BedrockBreakPreparation.Rejected("No edge-safe planet-core projection is available.")
        val persistedAperture = planned.aperture.copy(
            corePlacement = requireNotNull(nextProjection[planned.aperture.id]).placement,
        )

        val coreLevel = deepLevel.server.level(geometry.coreDimension)
            ?: return BedrockBreakPreparation.Rejected("Planet-core dimension is not loaded.")
        val oldPositions = projectionBlocks(geometry, oldProjection)
            ?: return BedrockBreakPreparation.Rejected("Existing core projection exceeds BlockPos range.")
        val nextPositions = projectionBlocks(geometry, nextProjection)
            ?: return BedrockBreakPreparation.Rejected("New core projection exceeds BlockPos range.")

        val toClose = oldPositions - nextPositions
        val toOpen = nextPositions - oldPositions
        // The core is a void level with a generated Bedrock shell. Loading an untouched shell
        // chunk must therefore materialize its target face before checking the transaction.
        PlanetCoreShellMaterializer.ensurePositions(coreLevel, toOpen)
        if (toOpen.any { pos ->
                val state = coreLevel.getBlockState(pos)
                !state.`is`(Blocks.BEDROCK) && !state.isAir
            }) {
            return BedrockBreakPreparation.Rejected("New core projection intersects player-modified blocks.")
        }
        if (toClose.any { pos ->
                val state = coreLevel.getBlockState(pos)
                !state.isAir && !state.`is`(Blocks.BEDROCK)
            }) {
            return BedrockBreakPreparation.Rejected("Old core projection cannot be safely restored.")
        }

        val mutations = buildList {
            add(BlockMutation(deepLevel, sourcePos, deepLevel.getBlockState(sourcePos), Blocks.AIR.defaultBlockState()))
            toClose.forEach { pos ->
                add(BlockMutation(coreLevel, pos, coreLevel.getBlockState(pos), Blocks.BEDROCK.defaultBlockState()))
            }
            toOpen.forEach { pos ->
                add(BlockMutation(coreLevel, pos, coreLevel.getBlockState(pos), Blocks.AIR.defaultBlockState()))
            }
        }
        return BedrockBreakPreparation.Accepted {
            atomicBlockChange(mutations) {
                save.put(persistedAperture, planned.removeIds)
                AperturePortalRuntime.rebuildCore(
                    deepLevel.server,
                    manifest,
                    geometry,
                    persistedAperture,
                    requireNotNull(nextProjection[persistedAperture.id]),
                )
            }
        }
    }

    private fun prepareCoreBreakFromCore(
        coreLevel: ServerLevel,
        sourcePos: BlockPos,
        resolved: ResolvedBoundary,
        geometry: PlanetCoreGeometry,
    ): BedrockBreakPreparation {
        val coreCell = coreResolver.shellCellAt(geometry, sourcePos)
            ?: return BedrockBreakPreparation.Rejected("Planet-core apertures must start away from cube edges and corners.")
        val connection = resolved.connection
        val deepPeriod = periodByDimension[geometry.deepDimension]
            ?: return BedrockBreakPreparation.Rejected("Missing deep-layer toroidal period.")
        val deepPlane = planeByEndpoint[geometry.deepDimension to connection.targetBoundaryFace]
            ?: return BedrockBreakPreparation.Rejected("Missing deep-layer Bedrock plane.")
        val save = BoundaryApertureSaveData.forServer(coreLevel.server)
        val oldApertures = save.coreApertures().filter { it.connectionId == geometry.connectionId }
        val oldProjection = coreResolver.resolve(geometry, oldApertures)
            ?: return BedrockBreakPreparation.Rejected("Existing core aperture projection is invalid.")

        val touching = oldApertures.mapNotNull { aperture ->
            val placement = oldProjection[aperture.id]?.placement ?: return@mapNotNull null
            val cell = placement.unproject(coreCell) ?: return@mapNotNull null
            if (aperture.shape.contains(cell) || aperture.shape.touches(cell)) aperture to cell else null
        }
        if (touching.size > 1) {
            return BedrockBreakPreparation.Rejected("This core block would merge incompatible apertures.")
        }

        val planned = touching.singleOrNull()?.let { (aperture, cell) ->
            val placement = requireNotNull(oldProjection[aperture.id]).placement
            PlannedCore(aperture.copy(shape = aperture.shape.with(cell), corePlacement = placement)) to cell
        } ?: run {
            val (id, sequence) = save.allocateIdentity()
            val deepAnchor = freeDeepAnchor(deepPeriod, sourcePos, oldApertures)
                ?: return BedrockBreakPreparation.Rejected("No free deep-layer counterpart is available for this core aperture.")
            PlannedCore(
                CoreBoundaryAperture(
                    id = id,
                    connectionId = connection.id,
                    createdSequence = sequence,
                    planetId = geometry.planetId,
                    deepDimension = geometry.deepDimension,
                    deepFace = connection.targetBoundaryFace,
                    deepAnchor = deepAnchor,
                    corePlacement = CoreAperturePlacement(coreCell.face, coreCell.u, coreCell.v, 0),
                ),
            ) to ApertureCell(0, 0)
        }
        val (plannedCore, addedCell) = planned
        val nextApertures = oldApertures.filterNot { it.id in plannedCore.removeIds || it.id == plannedCore.aperture.id } + plannedCore.aperture
        val nextProjection = coreResolver.resolve(geometry, nextApertures)
            ?: return BedrockBreakPreparation.Rejected("No edge-safe planet-core projection is available.")
        val persistedAperture = plannedCore.aperture.copy(
            corePlacement = requireNotNull(nextProjection[plannedCore.aperture.id]).placement,
        )
        val counterpart = deepPeriod.apply(persistedAperture.deepAnchor, addedCell).toBlockPos(deepPlane.y)
            ?: return BedrockBreakPreparation.Rejected("Mapped deep-layer counterpart is outside Minecraft BlockPos range.")
        val deepLevel = coreLevel.server.level(geometry.deepDimension)
            ?: return BedrockBreakPreparation.Rejected("Deep-layer dimension is not loaded.")
        if (!deepLevel.getBlockState(counterpart).`is`(Blocks.BEDROCK)) {
            return BedrockBreakPreparation.Rejected("Mapped deep-layer counterpart is not Bedrock.")
        }

        val oldPositions = projectionBlocks(geometry, oldProjection)
            ?: return BedrockBreakPreparation.Rejected("Existing core projection exceeds BlockPos range.")
        val nextPositions = projectionBlocks(geometry, nextProjection)
            ?: return BedrockBreakPreparation.Rejected("New core projection exceeds BlockPos range.")
        val toClose = oldPositions - nextPositions
        val toOpen = nextPositions - oldPositions
        PlanetCoreShellMaterializer.ensurePositions(coreLevel, toOpen)
        if (toOpen.any { pos ->
                val state = coreLevel.getBlockState(pos)
                !state.`is`(Blocks.BEDROCK) && !state.isAir
            }) {
            return BedrockBreakPreparation.Rejected("New core projection intersects player-modified blocks.")
        }

        val mutations = buildList {
            add(BlockMutation(deepLevel, counterpart, deepLevel.getBlockState(counterpart), Blocks.AIR.defaultBlockState()))
            toClose.forEach { pos -> add(BlockMutation(coreLevel, pos, coreLevel.getBlockState(pos), Blocks.BEDROCK.defaultBlockState())) }
            toOpen.forEach { pos -> add(BlockMutation(coreLevel, pos, coreLevel.getBlockState(pos), Blocks.AIR.defaultBlockState())) }
        }
        return BedrockBreakPreparation.Accepted {
            atomicBlockChange(mutations) {
                save.put(persistedAperture, plannedCore.removeIds)
                AperturePortalRuntime.rebuildCore(
                    coreLevel.server,
                    manifest,
                    geometry,
                    persistedAperture,
                    requireNotNull(nextProjection[persistedAperture.id]),
                )
            }
        }
    }

    private fun freeDeepAnchor(
        period: HorizontalPeriod,
        corePosition: BlockPos,
        apertures: Collection<CoreBoundaryAperture>,
    ): HorizontalPosition? = generateSequence(0) { it + 1 }.take(4096).map { attempt ->
        HorizontalPosition(
            period.canonical(corePosition.x.toLong() + attempt.toLong() * 7_919L),
            period.canonical(corePosition.z.toLong() + attempt.toLong() * 10_007L),
        )
    }.firstOrNull { candidate ->
        apertures.none { aperture ->
            val offset = period.offset(aperture.deepAnchor, candidate)
            aperture.shape.contains(offset) || aperture.shape.touches(offset)
        }
    }

    private fun resolveBoundary(dimension: DimensionId, position: BlockPos): ResolvedBoundary? {
        val matches = manifest.links.filter { connection ->
            connection.kind == DimensionConnectionKind.RADIAL_BOUNDARY &&
                connection.boundarySurface == BoundarySurface.BEDROCK &&
                when (dimension) {
                    connection.source -> (
                        coreByConnection[connection.id] == null &&
                            planeByEndpoint[dimension to connection.sourceBoundaryFace]?.y == position.y
                        ) || coreByConnection[connection.id]?.coreDimension == dimension
                    connection.target -> planeByEndpoint[dimension to connection.targetBoundaryFace]?.y == position.y
                    else -> false
                }
        }.mapNotNull { connection ->
            when (dimension) {
                connection.source -> ResolvedBoundary(connection, true, connection.sourceBoundaryFace)
                connection.target -> ResolvedBoundary(connection, false, connection.targetBoundaryFace)
                else -> null
            }
        }
        return matches.singleOrNull()
    }

    private fun newPaired(
        save: BoundaryApertureSaveData,
        connection: DimensionConnection,
        sourcePeriod: HorizontalPeriod,
        targetPeriod: HorizontalPeriod,
        broken: HorizontalPosition,
        atSource: Boolean,
    ): PlannedPaired {
        val (id, sequence) = save.allocateIdentity()
        return if (atSource) {
            val mapped = connection.targetPosition(DimensionPosition(broken.x, 0, broken.z))
            PlannedPaired(
                PairedBoundaryAperture(
                    id, connection.id, sequence,
                    sourceAnchor = sourcePeriod.canonical(broken),
                    targetAnchor = targetPeriod.canonical(HorizontalPosition(mapped.x, mapped.z)),
                ),
            )
        } else {
            val mapped = connection.inverse().targetPosition(DimensionPosition(broken.x, 0, broken.z))
            PlannedPaired(
                PairedBoundaryAperture(
                    id, connection.id, sequence,
                    sourceAnchor = sourcePeriod.canonical(HorizontalPosition(mapped.x, mapped.z)),
                    targetAnchor = targetPeriod.canonical(broken),
                ),
            )
        }
    }

    private fun extendPaired(
        aperture: PairedBoundaryAperture,
        sourcePeriod: HorizontalPeriod,
        targetPeriod: HorizontalPeriod,
        broken: HorizontalPosition,
        atSource: Boolean,
    ): PlannedPaired {
        val period = if (atSource) sourcePeriod else targetPeriod
        val anchor = if (atSource) aperture.sourceAnchor else aperture.targetAnchor
        return PlannedPaired(aperture.copy(shape = aperture.shape.with(period.offset(anchor, broken))))
    }

    private fun mergePaired(
        touching: List<PairedBoundaryAperture>,
        sourcePeriod: HorizontalPeriod,
        targetPeriod: HorizontalPeriod,
        broken: HorizontalPosition,
        atSource: Boolean,
    ): PlannedPaired? {
        val counterparts = touching.map {
            counterpartFor(it, sourcePeriod, targetPeriod, broken, atSource)
        }.distinct()
        if (counterparts.size != 1) return null
        val survivor = touching.minWith(compareBy<PairedBoundaryAperture> { it.createdSequence }.thenBy { it.id })
        val mergedCells = linkedSetOf<ApertureCell>()
        touching.forEach { aperture ->
            aperture.shape.cells.forEach { cell ->
                val sourceWorld = sourcePeriod.apply(aperture.sourceAnchor, cell)
                mergedCells += sourcePeriod.offset(survivor.sourceAnchor, sourceWorld)
            }
        }
        val sourceWorldForBreak = if (atSource) broken else counterparts.single()
        mergedCells += sourcePeriod.offset(survivor.sourceAnchor, sourceWorldForBreak)
        return PlannedPaired(
            survivor.copy(shape = ApertureShape(mergedCells)),
            touching.map { it.id }.filterNot { it == survivor.id }.toSet(),
        )
    }

    private fun counterpartFor(
        aperture: PairedBoundaryAperture,
        sourcePeriod: HorizontalPeriod,
        targetPeriod: HorizontalPeriod,
        broken: HorizontalPosition,
        atSource: Boolean,
    ): HorizontalPosition {
        return if (atSource) {
            val offset = sourcePeriod.offset(aperture.sourceAnchor, broken)
            targetPeriod.apply(aperture.targetAnchor, offset)
        } else {
            val offset = targetPeriod.offset(aperture.targetAnchor, broken)
            sourcePeriod.apply(aperture.sourceAnchor, offset)
        }
    }

    private fun mergeCore(
        touching: List<CoreBoundaryAperture>,
        period: HorizontalPeriod,
        broken: HorizontalPosition,
    ): PlannedCore {
        val survivor = touching.minWith(compareBy<CoreBoundaryAperture> { it.createdSequence }.thenBy { it.id })
        val mergedCells = linkedSetOf<ApertureCell>()
        touching.forEach { aperture ->
            aperture.shape.cells.forEach { cell ->
                val world = period.apply(aperture.deepAnchor, cell)
                mergedCells += period.offset(survivor.deepAnchor, world)
            }
        }
        mergedCells += period.offset(survivor.deepAnchor, broken)
        return PlannedCore(
            survivor.copy(shape = ApertureShape(mergedCells)),
            touching.map { it.id }.filterNot { it == survivor.id }.toSet(),
        )
    }

    private fun projectionBlocks(
        geometry: PlanetCoreGeometry,
        projections: Map<String, CoreApertureProjection>,
    ): Set<BlockPos>? {
        val result = linkedSetOf<BlockPos>()
        projections.values.forEach { projection ->
            result += coreResolver.blockPositions(geometry, projection) ?: return null
        }
        return result
    }
}

private data class ResolvedBoundary(
    val connection: DimensionConnection,
    val atConnectionSource: Boolean,
    val deepFace: de.TeutonStudio.DynamicUniverse.dimension.DimensionBoundaryFace,
)

private data class PlannedPaired(
    val aperture: PairedBoundaryAperture,
    val removeIds: Set<String> = emptySet(),
)

private data class PlannedCore(
    val aperture: CoreBoundaryAperture,
    val removeIds: Set<String> = emptySet(),
)

private data class BlockMutation(
    val level: ServerLevel,
    val position: BlockPos,
    val before: BlockState,
    val after: BlockState,
)

private val APERTURE_LOGGER = LogUtils.getLogger()

private fun atomicBlockChange(mutations: List<BlockMutation>, afterCommit: () -> Unit): Boolean {
    val applied = mutableListOf<BlockMutation>()
    return try {
        for (mutation in mutations) {
            if (!mutation.level.setBlock(mutation.position, mutation.after, Block.UPDATE_ALL)) {
                error("Failed to mutate ${mutation.level.dimension().location()} at ${mutation.position}")
            }
            applied += mutation
        }
        afterCommit()
        true
    } catch (error: Throwable) {
        APERTURE_LOGGER.warn(
            "Rolling back DynamicUniverse aperture transaction: {}",
            mutations.joinToString { "${it.level.dimension().location()}@${it.position}" },
            error,
        )
        applied.asReversed().forEach { mutation ->
            mutation.level.setBlock(mutation.position, mutation.before, Block.UPDATE_ALL)
        }
        false
    }
}

private fun HorizontalPosition.toBlockPos(y: Int): BlockPos? {
    if (x !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return null
    if (z !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return null
    return BlockPos(x.toInt(), y, z.toInt())
}

private fun MinecraftServer.level(dimension: DimensionId): ServerLevel? {
    val location = ResourceLocation.tryParse(dimension.value) ?: return null
    return getLevel(ResourceKey.create(Registries.DIMENSION, location))
}
