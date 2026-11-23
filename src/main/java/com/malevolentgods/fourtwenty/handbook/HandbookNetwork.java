package com.malevolentgods.fourtwenty.handbook;

import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Def;
import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Progress;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The Handbook's payloads (specs/016-weedcraft-handbook, contracts/payloads-and-commands.md), all optional channels, sent only to
 * connections that have them: a player's progress to them, their signing an honour requirement back, and a wearer's earned badges to
 * those who can see them (the Stoner Badge Sash).
 */
@EventBusSubscriber(modid = "fourtwenty")
public final class HandbookNetwork {
    private HandbookNetwork() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("fourtwenty", path); }

    /** Progress to its owner: everything on login ({@code full}), or the changes with the requirements ticked and ranks or badges earned now. */
    public record ProgressPayload(boolean full, Map<String, Integer> states, Map<String, Integer> counts, Map<String, Long> earned,
                                  List<String> ticked, List<String> earnedNow) implements CustomPacketPayload {
        public static final Type<ProgressPayload> TYPE = new Type<>(id("handbook_progress"));
        public static final StreamCodec<ByteBuf, ProgressPayload> CODEC = StreamCodec.of((buf, p) -> {
            var b = new FriendlyByteBuf(buf);
            b.writeBoolean(p.full);
            b.writeMap(p.states, (x, k) -> x.writeUtf(k, 128), FriendlyByteBuf::writeVarInt);
            b.writeMap(p.counts, (x, k) -> x.writeUtf(k, 128), FriendlyByteBuf::writeVarInt);
            b.writeMap(p.earned, (x, k) -> x.writeUtf(k, 128), FriendlyByteBuf::writeLong);
            b.writeCollection(p.ticked, (x, k) -> x.writeUtf(k, 128));
            b.writeCollection(p.earnedNow, (x, k) -> x.writeUtf(k, 128));
        }, buf -> {
            var b = new FriendlyByteBuf(buf);
            return new ProgressPayload(b.readBoolean(), b.readMap(x -> x.readUtf(128), FriendlyByteBuf::readVarInt),
                    b.readMap(x -> x.readUtf(128), FriendlyByteBuf::readVarInt), b.readMap(x -> x.readUtf(128), FriendlyByteBuf::readLong),
                    b.readList(x -> x.readUtf(128)), b.readList(x -> x.readUtf(128)));
        });

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** A player signs an honour requirement on their honour. */
    public record SignPayload(String key) implements CustomPacketPayload {
        public static final Type<SignPayload> TYPE = new Type<>(id("handbook_sign"));
        public static final StreamCodec<ByteBuf, SignPayload> CODEC = StreamCodec.of(
                (buf, p) -> new FriendlyByteBuf(buf).writeUtf(p.key, 128), buf -> new SignPayload(new FriendlyByteBuf(buf).readUtf(128)));

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** A wearer's earned Stoner Badges, in the order earned, for the sash others see on them. */
    public record SashPayload(int entity, List<String> badges) implements CustomPacketPayload {
        public static final Type<SashPayload> TYPE = new Type<>(id("handbook_sash"));
        public static final StreamCodec<ByteBuf, SashPayload> CODEC = StreamCodec.of((buf, p) -> {
            var b = new FriendlyByteBuf(buf);
            b.writeVarInt(p.entity);
            b.writeCollection(p.badges, (x, k) -> x.writeUtf(k, 64));
        }, buf -> {
            var b = new FriendlyByteBuf(buf);
            return new SashPayload(b.readVarInt(), b.readList(x -> x.readUtf(64)));
        });

        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Set by the client. */
    public static Consumer<ProgressPayload> clientProgress = p -> {};
    public static Consumer<SashPayload> clientSash = p -> {};

    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event) -> {
            var reg = event.registrar("1").optional();
            reg.playToClient(ProgressPayload.TYPE, ProgressPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> clientProgress.accept(p)));
            reg.playToClient(SashPayload.TYPE, SashPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> clientSash.accept(p)));
            reg.playToServer(SignPayload.TYPE, SignPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) Deeds.sign(sp, p.key());
            }));
        });
    }

    /** A player's earned Stoner Badge ids, oldest first. */
    public static List<String> badges(Progress p) {
        List<Map.Entry<String, Long>> e = new ArrayList<>();
        for (var en : p.earnedAll().entrySet()) if (en.getKey().startsWith("badge:")) e.add(en);
        e.sort(Comparator.comparingLong(Map.Entry<String, Long>::getValue).thenComparing(Map.Entry::getKey));
        List<String> out = new ArrayList<>();
        for (var en : e) out.add(en.getKey().substring("badge:".length()));
        return out;
    }

    /** Tells the wearer and everyone who can see them their badges. */
    static void sashChanged(ServerPlayer player, Progress p) {
        var payload = new SashPayload(player.getId(), badges(p));
        if (player.connection.hasChannel(SashPayload.TYPE)) PacketDistributor.sendToPlayer(player, payload);
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && other.distanceToSqr(player) <= 128 * 128 && other.connection.hasChannel(SashPayload.TYPE)) PacketDistributor.sendToPlayer(other, payload);
        }
    }

    /** Chat: "X earned the Y badge!" (a server setting, on by default). */
    static void announce(ServerPlayer player, List<String> earnedNow) {
        if (!com.malevolentgods.fourtwenty.processing.ProcessingConfig.handbookAnnounce()) return;
        for (String key : earnedNow) {
            Def d = ScoutingData.byEarnKey(key);
            if (d == null) continue;
            String what = d.rank() ? "rank" : "badge";
            player.server.getPlayerList().broadcastSystemMessage(Component.translatable("handbook.fourtwenty.announce." + what, player.getDisplayName(),
                    Component.translatable("handbook.fourtwenty." + what + "." + d.id())), false);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Deeds.fullSync(player);
        sashChanged(player, Deeds.progress(player));
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof ServerPlayer target) || !(event.getEntity() instanceof ServerPlayer viewer)) return;
        if (viewer.connection.hasChannel(SashPayload.TYPE)) PacketDistributor.sendToPlayer(viewer, new SashPayload(target.getId(), badges(Deeds.progress(target))));
    }

    /** Test and tool helper: the earned map as keys in order. */
    static Map<String, Long> earnedOf(Progress p) { return new LinkedHashMap<>(p.earnedAll()); }
}
