package com.malevolentgods.fourtwenty.gear;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;

/** The gear key: the client asks the server to open the terpene slots for the gear in hand. */
public final class GearNetwork {
    private GearNetwork() {}

    public record OpenSlots() implements CustomPacketPayload {
        public static final Type<OpenSlots> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "gear_slots"));
        public static final StreamCodec<ByteBuf, OpenSlots> CODEC = StreamCodec.unit(new OpenSlots());

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").playToServer(OpenSlots.TYPE, OpenSlots.CODEC,
                (m, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer player) {
                        for (InteractionHand hand : InteractionHand.values()) {
                            if (Gear.piece(player.getItemInHand(hand)).isPresent()) {
                                GearMenu.open(player, hand);
                                return;
                            }
                        }
                    }
                })));
    }

    public static void openSlots() { PacketDistributor.sendToServer(new OpenSlots()); }
}
