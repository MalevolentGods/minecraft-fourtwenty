package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
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
 * A Builder's high, for the line on her citizen window (the user, 2026-11-14: "a buff indicator and timer"): whether
 * she's Lifted and on what, how long is left and whether it's counting down, or what she's doing about her next joint.
 * Sent every couple of seconds to players near her, only on connections that have the channel.
 */
public record HighStatusPayload(int entity, int state, String strainKey, int ticksLeft, boolean counting, int joints, boolean asking,
                                boolean spare) implements CustomPacketPayload {
    public static final int OFF = 0, LIFTED = 1, SMOKE_BREAK = 2, FLYING_HOME = 3, SOBER = 4;
    /** How far from her a player hears about it. */
    static final double RANGE = 128;

    public static final Type<HighStatusPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "high_status"));
    public static final StreamCodec<ByteBuf, HighStatusPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                var b = new FriendlyByteBuf(buf);
                b.writeVarInt(p.entity);
                b.writeVarInt(p.state);
                b.writeUtf(p.strainKey, 128);
                b.writeVarInt(p.ticksLeft);
                b.writeBoolean(p.counting);
                b.writeVarInt(p.joints);
                b.writeBoolean(p.asking);
                b.writeBoolean(p.spare);
            },
            buf -> {
                var b = new FriendlyByteBuf(buf);
                return new HighStatusPayload(b.readVarInt(), b.readVarInt(), b.readUtf(128), b.readVarInt(), b.readBoolean(),
                        b.readVarInt(), b.readBoolean(), b.readBoolean());
            });

    @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Set on the client by HighStatusClient. */
    public static Consumer<HighStatusPayload> clientHandler = p -> {};

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional().playToClient(TYPE, CODEC,
                (payload, context) -> context.enqueueWork(() -> clientHandler.accept(payload))));
    }

    public static void send(Entity citizen, HighStatusPayload payload) {
        if (!(citizen.level() instanceof ServerLevel level)) return;
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(citizen) <= RANGE * RANGE && viewer.connection.hasChannel(TYPE)) viewer.connection.send(payload);
        }
    }
}
