package de.TeutonStudio.DynamicUniverse.runtime

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.level.BlockEvent

/** Runtime owner for dynamic Bedrock apertures in the active Universe save. */
object BedrockApertureRuntime {
    @Volatile
    private var bridge: ServerBedrockApertureBridge? = null

    @Volatile
    private var manifest: UniverseGeometryManifest? = null

    fun install(manifest: UniverseGeometryManifest, planes: Collection<BedrockBoundaryPlane>) {
        this.manifest = manifest
        bridge = ServerBedrockApertureBridge(manifest, planes)
        AperturePortalRuntime.install(planes)
    }

    fun reconcile(server: MinecraftServer) {
        bridge?.reconcileCoreProjections(server)
        val activeManifest = manifest ?: return
        val save = BoundaryApertureSaveData.find(server) ?: return
        save.pairedApertures().forEach { aperture ->
            AperturePortalRuntime.rebuildPaired(server, activeManifest, aperture)
        }
        val resolver = PlanetCoreProjectionResolver()
        activeManifest.planetCores.forEach { geometry ->
            val apertures = save.coreApertures().filter { it.connectionId == geometry.connectionId }
            val projections = resolver.resolve(geometry, apertures) ?: return@forEach
            apertures.forEach { aperture ->
                projections[aperture.id]?.let { projection ->
                    AperturePortalRuntime.rebuildCore(server, activeManifest, geometry, aperture, projection)
                }
            }
        }
    }

    fun clear() {
        bridge = null
        manifest = null
        AperturePortalRuntime.clear()
    }

    internal fun prepareBedrockBreak(sourceLevel: ServerLevel, sourcePos: BlockPos): BedrockBreakPreparation =
        bridge?.prepareBedrockBreak(sourceLevel, sourcePos) ?: BedrockBreakPreparation.Ignored
}

/**
 * Boundary Bedrock is handled as one server-side transaction. The vanilla break is cancelled
 * after a valid plan has been built so source and counterpart can never diverge halfway through.
 */
@EventBusSubscriber(modid = DynamicUniverse.MOD_ID)
object BedrockApertureBreakListener {
    @SubscribeEvent
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        if (event.isCanceled || !event.state.`is`(Blocks.BEDROCK)) return
        val level = event.level as? ServerLevel ?: return
        when (val preparation = BedrockApertureRuntime.prepareBedrockBreak(level, event.pos)) {
            BedrockBreakPreparation.Ignored -> Unit
            is BedrockBreakPreparation.Rejected -> {
                event.isCanceled = true
                event.player.sendSystemMessage(Component.literal("DynamicUniverse: ${preparation.reason}"))
            }
            is BedrockBreakPreparation.Accepted -> {
                event.isCanceled = true
                if (!preparation.commit()) {
                    event.player.sendSystemMessage(Component.literal("DynamicUniverse: Aperture transaction rolled back."))
                }
            }
        }
    }
}
