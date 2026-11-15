package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Tells clients a Builder is Lifted (green shimmer and smoke) or isn't any more (contracts/config.md, "Payload").
 * Sent only to players tracking her, and only on connections that have the channel.
 */
public record LiftedPayload(int entity, boolean on) implements CustomPacketPayload {
    public static final Type<LiftedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "lifted"));
    public static final StreamCodec<ByteBuf, LiftedPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, LiftedPayload::entity, ByteBufCodecs.BOOL, LiftedPayload::on, LiftedPayload::new);

    @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Set on the client by LiftedClient. */
    public static Consumer<LiftedPayload> clientHandler = p -> {};

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional().playToClient(TYPE, CODEC,
                (payload, context) -> context.enqueueWork(() -> clientHandler.accept(payload))));
        NeoForge.EVENT_BUS.addListener(LiftedPayload::startTracking);
    }

    public static void send(Entity citizen, boolean on) {
        if (!(citizen.level() instanceof ServerLevel level)) return;
        var payload = new LiftedPayload(citizen.getId(), on);
        for (ServerPlayer viewer : level.getChunkSource().chunkMap.getPlayers(citizen.chunkPosition(), false)) {
            if (viewer.connection.hasChannel(TYPE)) viewer.connection.send(payload);
        }
    }

    /** A player who starts seeing a Lifted Builder (coming into range, or logging in) is told she shimmers. */
    private static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && viewer.connection.hasChannel(TYPE)
                && event.getTarget() instanceof com.minecolonies.core.entity.citizen.EntityCitizen citizen && LiftedService.isLifted(citizen)) {
            viewer.connection.send(new LiftedPayload(citizen.getId(), true));
        }
    }
}
