package com.malevolentgods.fourtwenty.terpene;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;
import java.util.List;

/** Sends a player's discoveries to their client: the full list on login, then each new one. */
public final class TerpeneNetwork {
    private TerpeneNetwork() {}

    /** {@code replace}: the whole list (login); otherwise newly learned keys, each shown as a toast. */
    public record Discoveries(List<String> keys, boolean replace) implements CustomPacketPayload {
        public static final Type<Discoveries> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "terpene_discoveries"));
        public static final StreamCodec<ByteBuf, Discoveries> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), Discoveries::keys,
                ByteBufCodecs.BOOL, Discoveries::replace,
                Discoveries::new);

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").playToClient(Discoveries.TYPE, Discoveries.CODEC,
                (payload, context) -> context.enqueueWork(() -> TerpeneKnowledge.receive(payload.keys(), payload.replace()))));
    }

    static void send(ServerPlayer player, List<String> keys, boolean replace) {
        if (player.connection.hasChannel(Discoveries.TYPE)) PacketDistributor.sendToPlayer(player, new Discoveries(keys, replace));
    }
}
