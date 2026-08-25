package de.TeutonStudio.DynamicUniverse.network

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.client.render.GlobeRenderBody
import de.TeutonStudio.DynamicUniverse.client.render.GlobeRenderContext
import de.TeutonStudio.DynamicUniverse.cosmos.GlobeAtlas
import de.TeutonStudio.DynamicUniverse.cosmos.GlobeVisualConfiguration
import de.TeutonStudio.DynamicUniverse.cosmos.Vector3
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.runtime.CelestialGlobeKind
import de.TeutonStudio.DynamicUniverse.runtime.UniverseGeometryManifest
import de.TeutonStudio.DynamicUniverse.runtime.UniverseRuntimeState
import de.TeutonStudio.DynamicUniverse.topology.HorizontalPeriod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext

/** Presentation-only body list. Terrain content is fetched through a separate authorized path. */
data class UniverseGlobeRenderPayload(val bodies: List<GlobeRenderBody>) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out UniverseGlobeRenderPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<UniverseGlobeRenderPayload>(
            ResourceLocation.fromNamespaceAndPath(DynamicUniverse.MOD_ID, "globe_render_context"),
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, UniverseGlobeRenderPayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeVarInt(payload.bodies.size)
                payload.bodies.forEach { body ->
                    buffer.writeUtf(body.bodyId)
                    buffer.writeVarInt(body.kind.ordinal)
                    buffer.writeUtf(body.sourceDimension.value)
                    buffer.writeLong(body.period.blocks)
                    buffer.writeDouble(body.visual.visualRadius)
                    buffer.writeVarInt(body.visual.atlas.ordinal)
                    buffer.writeDouble(body.visual.seamBlendFraction)
                    buffer.writeDouble(body.visual.curvatureStartHeight)
                    buffer.writeDouble(body.universePosition.x)
                    buffer.writeDouble(body.universePosition.y)
                    buffer.writeDouble(body.universePosition.z)
                }
            },
            { buffer ->
                UniverseGlobeRenderPayload(List(buffer.readVarInt()) {
                    GlobeRenderBody(
                        bodyId = buffer.readUtf(),
                        kind = CelestialGlobeKind.entries[buffer.readVarInt()],
                        sourceDimension = DimensionId(buffer.readUtf()),
                        period = HorizontalPeriod(buffer.readLong()),
                        visual = GlobeVisualConfiguration(
                            visualRadius = buffer.readDouble(),
                            atlas = GlobeAtlas.entries[buffer.readVarInt()],
                            seamBlendFraction = buffer.readDouble(),
                            curvatureStartHeight = buffer.readDouble(),
                        ),
                        universePosition = Vector3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    )
                })
            },
        )
    }
}

object UniverseGlobeRenderSync {
    @Volatile private var payload: UniverseGlobeRenderPayload? = null

    fun install(manifest: UniverseGeometryManifest, state: UniverseRuntimeState) {
        payload = UniverseGlobeRenderPayload(manifest.celestialGlobes.map { globe ->
            val position = state.kinematicsFor(globe.bodyId)?.position ?: Vector3.ZERO
            GlobeRenderBody(globe.bodyId, globe.kind, globe.sourceDimension, globe.period, globe.visual, position)
        })
    }

    fun clear() { payload = null }
    fun send(player: ServerPlayer) { payload?.let { PacketDistributor.sendToPlayer(player, it) } }
}

@EventBusSubscriber(modid = DynamicUniverse.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object UniverseGlobeRenderPayloadRegistration {
    @SubscribeEvent
    fun register(event: RegisterPayloadHandlersEvent) {
        event.registrar("1").playToClient(
            UniverseGlobeRenderPayload.TYPE,
            UniverseGlobeRenderPayload.STREAM_CODEC,
            ::handleClientPayload,
        )
    }

    private fun handleClientPayload(payload: UniverseGlobeRenderPayload, context: IPayloadContext) {
        context.enqueueWork { GlobeRenderContext.install(payload.bodies) }
    }
}

@EventBusSubscriber(modid = DynamicUniverse.MOD_ID)
object UniverseGlobeRenderSyncEvents {
    @SubscribeEvent fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        (event.entity as? ServerPlayer)?.let(UniverseGlobeRenderSync::send)
    }

    @SubscribeEvent fun onPlayerChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        (event.entity as? ServerPlayer)?.let(UniverseGlobeRenderSync::send)
    }
}
