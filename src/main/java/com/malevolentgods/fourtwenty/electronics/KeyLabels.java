package com.malevolentgods.fourtwenty.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;

/**
 * Key labels: a short text on the face of a Puff Button or Toggle Joint mounted on a Rolling Tray, and the colour it glows (or none).
 * Set in its screen (sneak and use it with an empty hand); the client asks, the server checks and keeps it.
 */
public final class KeyLabels {
    private KeyLabels() {}

    /** At most this many characters: what fits on a key's face. */
    public static final int MAX = 4;
    /** How near the player has to be, squared. */
    private static final double REACH = 64;

    /** A label and its glow: a dye's id, or -1 for none. */
    public record SetLabel(BlockPos pos, String label, int glow) implements CustomPacketPayload {
        public static final Type<SetLabel> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "key_label"));
        public static final StreamCodec<FriendlyByteBuf, SetLabel> CODEC = StreamCodec.ofMember((m, buf) -> {
            buf.writeBlockPos(m.pos);
            buf.writeUtf(m.label, 32);
            buf.writeVarInt(m.glow);
        }, buf -> new SetLabel(buf.readBlockPos(), buf.readUtf(32), buf.readVarInt()));

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** What may be shown: no formatting codes or control characters, trimmed, at most {@link #MAX} characters. */
    public static String clean(String text) {
        String s = net.minecraft.util.StringUtil.filterText(text == null ? "" : text).replace("§", "").strip();
        return s.codePointCount(0, s.length()) <= MAX ? s : s.substring(0, s.offsetByCodePoints(0, MAX));
    }

    public static int glow(int dye) { return dye < 0 || dye > 15 ? -1 : dye; }

    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").playToServer(SetLabel.TYPE, SetLabel.CODEC,
                (m, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer p) || !p.level().isLoaded(m.pos) || p.distanceToSqr(m.pos.getCenter()) > REACH) return;
                    var state = p.level().getBlockState(m.pos);
                    if (!(state.getBlock() instanceof ComponentBlock cb) || !cb.type().key() || ComponentBlock.mountedOn(state) == null) return;
                    if (!(p.level().getBlockEntity(m.pos) instanceof ComponentBlockEntity be)) return;
                    be.label(clean(m.label), glow(m.glow));
                    p.level().sendBlockUpdated(m.pos, state, state, 2);
                })));
    }
}
