package com.malevolentgods.fourtwenty.paving;

import com.malevolentgods.fourtwenty.domain.PaveRules;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;

/** The settings screen tells the server which road and shape the held pen lays. */
public final class PaveNetwork {
    private PaveNetwork() {}

    public record SetMode(int target, int shape) implements CustomPacketPayload {
        public static final Type<SetMode> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "pave_pen_mode"));
        public static final StreamCodec<ByteBuf, SetMode> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SetMode::target, ByteBufCodecs.VAR_INT, SetMode::shape, SetMode::new);

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").playToServer(SetMode.TYPE, SetMode.CODEC,
                (m, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer player) apply(player, m);
                })));
    }

    /** Sets the mode on the pen in hand, main hand first. A plain pen only lays whole blocks. */
    public static void apply(ServerPlayer player, SetMode m) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof PavePenItem pen)) continue;
            int target = Math.max(1, Math.min(PaveRules.TIERS, m.target()));
            int shape = pen.pro() ? Math.floorMod(m.shape(), PaveRules.Shape.values().length) : 0;
            PavePenItem.set(stack, PavePenItem.state(stack).withMode(target, shape));
            return;
        }
    }

    public static void send(int target, int shape) {
        PacketDistributor.sendToServer(new SetMode(target, shape));
    }
}
