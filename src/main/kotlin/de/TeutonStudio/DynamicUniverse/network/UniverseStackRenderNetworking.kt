package de.TeutonStudio.DynamicUniverse.network

import de.TeutonStudio.DynamicUniverse.DynamicUniverse
import de.TeutonStudio.DynamicUniverse.dimension.DimensionId
import de.TeutonStudio.DynamicUniverse.runtime.StackRenderContext
import de.TeutonStudio.DynamicUniverse.runtime.StackRenderLayer
import de.TeutonStudio.DynamicUniverse.runtime.UniverseGeometryManifest
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

data class UniverseStackRenderPayload(val layers: List<StackRenderLayer>) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<UniverseStackRenderPayload>(
            ResourceLocation.fromNamespaceAndPath(DynamicUniverse.MOD_ID, "stack_render_context"),
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, UniverseStackRenderPayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeVarInt(payload.layers.size)
                payload.layers.forEach { layer ->
                    buffer.writeUtf(layer.dimension.value)
                    buffer.writeUtf(layer.stackId)
                    buffer.writeVarInt(layer.index)
                }
            },
            { buffer ->
                List(buffer.readVarInt()) {
                    StackRenderLayer(
                        dimension = DimensionId(buffer.readUtf()),
                        stackId = buffer.readUtf(),
                        index = buffer.readVarInt(),
                    )
                }.let(::UniverseStackRenderPayload)
            },
        )
    }
}

/** The server keeps the manifest and sends the minimal client rendering projection on join/change. */
object UniverseStackRenderSync {
    @Volatile
    private var payload: UniverseStackRenderPayload? = null

    fun install(manifest: UniverseGeometryManifest) {
        payload = UniverseStackRenderPayload(
            manifest.layers.mapNotNull { layer ->
                layer.renderStackId?.let { StackRenderLayer(layer.dimension, it, layer.renderStackIndex) }
            },
        )
    }

    fun clear() {
        payload = null
    }

    fun send(player: ServerPlayer) {
        payload?.let { PacketDistributor.sendToPlayer(player, it) }
    }
}

@EventBusSubscriber(modid = DynamicUniverse.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object UniverseStackRenderPayloadRegistration {
    @SubscribeEvent
    fun register(event: RegisterPayloadHandlersEvent) {
        event.registrar("1").playToClient(
            UniverseStackRenderPayload.TYPE,
            UniverseStackRenderPayload.STREAM_CODEC,
            ::handleClientPayload,
        )
    }

    private fun handleClientPayload(payload: UniverseStackRenderPayload, context: IPayloadContext) {
        context.enqueueWork { StackRenderContext.install(payload.layers) }
    }
}

@EventBusSubscriber(modid = DynamicUniverse.MOD_ID)
object UniverseStackRenderSyncEvents {
    @SubscribeEvent
    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        (event.entity as? ServerPlayer)?.let(UniverseStackRenderSync::send)
    }

    @SubscribeEvent
    fun onPlayerChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        (event.entity as? ServerPlayer)?.let(UniverseStackRenderSync::send)
    }
}
