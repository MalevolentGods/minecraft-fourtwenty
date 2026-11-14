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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * The Midnight Machine's state for one Builder (specs/014-midnight-machine contracts/assets-and-sync.md): whether she
 * wears it, whether the music plays, and where in the album, stamped with the server's game time so every client
 * starts at the same moment. Sent only where the channel exists.
 */
public record MidnightMachinePayload(int entity, boolean playing, boolean lifted, int track, long positionMs, long sentAt)
        implements CustomPacketPayload {
    public static final Type<MidnightMachinePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "midnight_machine"));
    public static final StreamCodec<ByteBuf, MidnightMachinePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MidnightMachinePayload::entity,
            ByteBufCodecs.BOOL, MidnightMachinePayload::playing,
            ByteBufCodecs.BOOL, MidnightMachinePayload::lifted,
            ByteBufCodecs.VAR_INT, MidnightMachinePayload::track,
            ByteBufCodecs.VAR_LONG, MidnightMachinePayload::positionMs,
            ByteBufCodecs.VAR_LONG, MidnightMachinePayload::sentAt,
            MidnightMachinePayload::new);

    @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Set on the client by MidnightMachineClient. */
    public static Consumer<MidnightMachinePayload> clientHandler = p -> {};

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional().playToClient(TYPE, CODEC,
                (payload, context) -> context.enqueueWork(() -> clientHandler.accept(payload))));
    }

    public static void send(Entity citizen, MidnightMachinePayload payload) {
        if (!(citizen.level() instanceof ServerLevel level)) return;
        for (ServerPlayer viewer : level.getChunkSource().chunkMap.getPlayers(citizen.chunkPosition(), false)) {
            send(viewer, payload);
        }
    }

    public static void send(ServerPlayer viewer, MidnightMachinePayload payload) {
        if (viewer.connection.hasChannel(TYPE)) viewer.connection.send(payload);
    }
}
