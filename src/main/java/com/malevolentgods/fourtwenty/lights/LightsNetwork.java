package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.domain.Programs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Lit Bit and Weedweeno messages. To the client: lamp colours, and a Weedweeno's state for its screen. To the server: Weedweeno edits,
 * a lamp's new ID, the Sticky Finger's next ID, and closing the screen. Everything the server is sent is checked: the player is near,
 * may program, and the program fits. Nothing is sent to a client without our channel.
 */
public final class LightsNetwork {
    private LightsNetwork() {}

    private static final double REACH = 8 * 8;

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("fourtwenty", path); }

    public record LampColours(long[] at, int[] rgb) implements CustomPacketPayload {
        public static final Type<LampColours> TYPE = new Type<>(id("lamp_colours"));
        public static final StreamCodec<FriendlyByteBuf, LampColours> CODEC = StreamCodec.ofMember((m, buf) -> {
            buf.writeVarInt(m.at.length);
            for (int i = 0; i < m.at.length; i++) {
                buf.writeLong(m.at[i]);
                buf.writeInt(m.rgb[i]);
            }
        }, buf -> {
            int n = Math.min(buf.readVarInt(), 1 << 16);
            long[] at = new long[n];
            int[] rgb = new int[n];
            for (int i = 0; i < n; i++) {
                at[i] = buf.readLong();
                rgb[i] = buf.readInt();
            }
            return new LampColours(at, rgb);
        });

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record State(BlockPos pos, boolean open, List<String> names, int selected, String code, int running, int state, String message, int line,
                        List<String> console) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(id("weedweeno_state"));
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = StreamCodec.ofMember((m, buf) -> {
            buf.writeBlockPos(m.pos);
            buf.writeBoolean(m.open);
            buf.writeVarInt(m.names.size());
            for (String n : m.names) buf.writeUtf(n, 64);
            buf.writeVarInt(m.selected);
            buf.writeUtf(m.code, Programs.MAX_CODE);
            buf.writeVarInt(m.running + 1);
            buf.writeVarInt(m.state);
            buf.writeUtf(m.message, 512);
            buf.writeVarInt(m.line);
            buf.writeVarInt(m.console.size());
            for (String c : m.console) buf.writeUtf(c, 256);
        }, buf -> {
            BlockPos pos = buf.readBlockPos();
            boolean open = buf.readBoolean();
            int n = Math.min(buf.readVarInt(), 4096);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < n; i++) names.add(buf.readUtf(64));
            int selected = buf.readVarInt();
            String code = buf.readUtf(Programs.MAX_CODE);
            int running = buf.readVarInt() - 1, state = buf.readVarInt();
            String message = buf.readUtf(512);
            int line = buf.readVarInt();
            int c = Math.min(buf.readVarInt(), 64);
            List<String> console = new ArrayList<>();
            for (int i = 0; i < c; i++) console.add(buf.readUtf(256));
            return new State(pos, open, names, selected, code, running, state, message, line, console);
        });

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public enum Action { SELECT, SAVE, NEW, DELETE, RENAME, DUPLICATE, RUN, STOP, EXPORT, IMPORT, PRESETS, CLOSE }

    public record Edit(BlockPos pos, int action, int index, String name, String code) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(id("weedweeno_edit"));
        public static final StreamCodec<FriendlyByteBuf, Edit> CODEC = StreamCodec.ofMember((m, buf) -> {
            buf.writeBlockPos(m.pos);
            buf.writeVarInt(m.action);
            buf.writeVarInt(m.index + 1);
            buf.writeUtf(m.name, 64);
            buf.writeUtf(m.code, Programs.MAX_CODE);
        }, buf -> new Edit(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt() - 1, buf.readUtf(64), buf.readUtf(Programs.MAX_CODE)));

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SetLampId(BlockPos pos, int lamp) implements CustomPacketPayload {
        public static final Type<SetLampId> TYPE = new Type<>(id("lit_bit_id"));
        public static final StreamCodec<FriendlyByteBuf, SetLampId> CODEC = StreamCodec.ofMember((m, buf) -> {
            buf.writeBlockPos(m.pos);
            buf.writeVarInt(m.lamp);
        }, buf -> new SetLampId(buf.readBlockPos(), buf.readVarInt()));

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SetFingerId(boolean offhand, int next) implements CustomPacketPayload {
        public static final Type<SetFingerId> TYPE = new Type<>(id("sticky_finger_id"));
        public static final StreamCodec<FriendlyByteBuf, SetFingerId> CODEC = StreamCodec.ofMember((m, buf) -> {
            buf.writeBoolean(m.offhand);
            buf.writeVarInt(m.next);
        }, buf -> new SetFingerId(buf.readBoolean(), buf.readVarInt()));

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event) -> {
            var r = event.registrar("1");
            r.playToClient(LampColours.TYPE, LampColours.CODEC, (m, ctx) -> ctx.enqueueWork(() -> com.malevolentgods.fourtwenty.client.LightsClient.colours(m)));
            r.playToClient(State.TYPE, State.CODEC, (m, ctx) -> ctx.enqueueWork(() -> com.malevolentgods.fourtwenty.client.LightsClient.state(m)));
            r.playToServer(Edit.TYPE, Edit.CODEC, (m, ctx) -> ctx.enqueueWork(() -> edit(m, ctx)));
            r.playToServer(SetLampId.TYPE, SetLampId.CODEC, (m, ctx) -> ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer p) || !near(p, m.pos)) return;
                if (p.level().getBlockEntity(m.pos) instanceof LitBitBlockEntity lamp) lamp.setId(m.lamp);
            }));
            r.playToServer(SetFingerId.TYPE, SetFingerId.CODEC, (m, ctx) -> ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer p)) return;
                ItemStack glove = p.getItemInHand(m.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                if (glove.is(ModLights.STICKY_FINGER.get())) StickyFingerItem.setNext(glove, m.next);
            }));
        });
    }

    private static boolean near(ServerPlayer p, BlockPos pos) {
        return p.level().isLoaded(pos) && p.distanceToSqr(pos.getCenter()) <= REACH;
    }

    public static void sendState(ServerPlayer player, WeedweenoBlockEntity w, boolean open) {
        if (!player.connection.hasChannel(State.TYPE)) return;
        var programs = w.programs();
        List<String> names = programs.stream().map(Programs.Program::name).toList();
        int selected = Math.max(0, Math.min(w.selected(), programs.size() - 1));
        String code = programs.isEmpty() ? "" : programs.get(selected).code();
        PacketDistributor.sendToPlayer(player, new State(w.getBlockPos(), open, names, selected, code, w.running(), w.state().ordinal(),
                w.message(), w.line(), w.console()));
    }

    /** Sends a Weedweeno edit; the client screen calls this. */
    public static void send(BlockPos pos, Action action, int index, String name, String code) {
        PacketDistributor.sendToServer(new Edit(pos, action.ordinal(), index, name == null ? "" : name, code == null ? "" : code));
    }

    private static void edit(Edit m, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer p) || !near(p, m.pos) || !(p.level().getBlockEntity(m.pos) instanceof WeedweenoBlockEntity w)) return;
        Action[] actions = Action.values();
        if (m.action < 0 || m.action >= actions.length) return;
        Action action = actions[m.action];
        if (action == Action.CLOSE) {
            w.unwatch(p);
            return;
        }
        if (!WeedweenoBlock.mayProgram(p)) return;
        w.watch(p);
        switch (action) {
            case SELECT -> w.select(m.index);
            case SAVE, RENAME -> {
                String code = action == Action.RENAME && m.index >= 0 && m.index < w.programs().size() ? w.programs().get(m.index).code() : m.code;
                if (!w.save(m.index, m.name, code)) p.displayClientMessage(Component.translatable("message.fourtwenty.weedweeno.too_long"), true);
            }
            case NEW -> w.create();
            case DELETE -> w.delete(m.index);
            case DUPLICATE -> w.duplicate(m.index);
            case RUN -> {
                if (w.save(m.index, m.name, m.code)) w.run(m.index);
                else p.displayClientMessage(Component.translatable("message.fourtwenty.weedweeno.too_long"), true);
            }
            case STOP -> w.stop();
            case PRESETS -> w.restorePresets();
            case EXPORT -> export(p, w, m.index);
            case IMPORT -> load(p, w);
            default -> {}
        }
        sendState(p, w, false);
    }

    /** Writes a program onto a blank Bit Chit from the player's inventory. */
    private static void export(ServerPlayer p, WeedweenoBlockEntity w, int index) {
        if (index < 0 || index >= w.programs().size()) return;
        for (ItemStack stack : p.getInventory().items) {
            if (!BitChitItem.blank(stack)) continue;
            stack.shrink(1);
            ItemStack written = BitChitItem.written(w.programs().get(index));
            if (!p.getInventory().add(written)) p.drop(written, false);
            p.displayClientMessage(Component.translatable("message.fourtwenty.weedweeno.exported", w.programs().get(index).name()), true);
            return;
        }
        p.displayClientMessage(Component.translatable("message.fourtwenty.weedweeno.no_chit"), true);
    }

    /** Loads the written Bit Chit in hand (or the first in the inventory) as a new program. */
    private static void load(ServerPlayer p, WeedweenoBlockEntity w) {
        List<ItemStack> look = new ArrayList<>(List.of(p.getMainHandItem(), p.getOffhandItem()));
        look.addAll(p.getInventory().items);
        for (ItemStack stack : look) {
            var program = stack.is(ModLights.BIT_CHIT.get()) ? BitChitItem.program(stack) : null;
            if (program == null) continue;
            w.add(program);
            p.displayClientMessage(Component.translatable("message.fourtwenty.weedweeno.imported", program.name()), true);
            return;
        }
        p.displayClientMessage(Component.translatable("message.fourtwenty.weedweeno.no_written"), true);
    }
}
