package com.malevolentgods.fourtwenty.infuser;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Tint;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The server side of the Inspiration Infuser: each player's state (in their persisted data, so it survives death),
 * inspiration from smoking, and catches. Mixing happens on the client (it's free); a catch is only recorded after the
 * server has mixed the same pool with the same model and found it within {@link Tint#MATCH} of a colour the player is
 * inspired by. Catching a colour also catches its twins.
 */
public final class Infuser {
    private Infuser() {}

    private static final String KEY = "fourtwenty_infuser";

    // ------------------------------------------------------------------ state

    private static CompoundTag persisted(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static InfuserState get(Player player) {
        int n = Tint.get().resins().size();
        CompoundTag p = persisted(player);
        if (!p.contains(KEY)) { InfuserState s = InfuserState.fresh(n); p.put(KEY, s.save()); return s; }
        return InfuserState.load(p.getCompound(KEY), n);
    }

    public static void put(ServerPlayer player, InfuserState s) {
        persisted(player).put(KEY, s.save());
        PacketDistributor.sendToPlayer(player, new Sync(s.save()));
    }

    /** Colours this player has caught (their terpenes are unlocked for the Terpinator). */
    public static boolean caught(Player player, String id) { return get(player).caught.containsKey(id); }

    // ------------------------------------------------------------------ inspiration

    /** The colours a strain can inspire: Landrace the 16 Minecraft colours; a bred strain its hue row's four tones. */
    public static List<String> colours(Strain strain) {
        List<String> out = new ArrayList<>();
        for (Tint.Target t : Tint.get().targets())
            if (strain == Strain.LANDRACE ? t.minecraft() : t.id().startsWith(strain.id() + "_")) out.add(t.id());
        return out;
    }

    /** A hit: one or two of the strain's colours the player isn't yet fully inspired by gain a level. */
    public static void inspire(ServerPlayer player, Strain strain) {
        InfuserState s = get(player);
        List<String> open = new ArrayList<>(colours(strain).stream().filter(id -> s.level(id) < InfuserState.MAX).toList());
        if (open.isEmpty()) return;
        var random = player.getRandom();
        int n = Math.min(open.size(), random.nextFloat() < 0.35f ? 2 : 1);
        List<String> picked = new ArrayList<>();
        for (int i = 0; i < n; i++) picked.add(open.remove(random.nextInt(open.size())));
        int top = 0;
        for (String id : picked) { int l = s.level(id) + 1; s.inspiration.put(id, l); top = Math.max(top, l); }
        if (s.chasing == null) s.chasing = picked.get(0);
        put(player, s);
        PacketDistributor.sendToPlayer(player, new Inspired(picked, top));
    }

    // ------------------------------------------------------------------ messages

    /** Server to client: the player's whole Infuser state. */
    public record Sync(CompoundTag state) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "infuser_sync"));
        public static final StreamCodec<ByteBuf, Sync> CODEC = ByteBufCodecs.COMPOUND_TAG.map(Sync::new, Sync::state);
        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server to client: a hit inspired these colours (the screen washes with them). */
    public record Inspired(List<String> ids, int level) implements CustomPacketPayload {
        public static final Type<Inspired> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "infuser_inspired"));
        public static final StreamCodec<ByteBuf, Inspired> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)), Inspired::ids, ByteBufCodecs.VAR_INT, Inspired::level, Inspired::new);
        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client to server: the pool, the colour being chased, and whether it's sealed (saved so it's there next visit). */
    public record Pool(List<Integer> pool, String chasing, String sealed) implements CustomPacketPayload {
        public static final Type<Pool> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "infuser_pool"));
        public static final StreamCodec<ByteBuf, Pool> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), Pool::pool, ByteBufCodecs.STRING_UTF8, Pool::chasing,
                ByteBufCodecs.STRING_UTF8, Pool::sealed, Pool::new);
        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client to server: "this pool catches that colour" (checked before it counts). */
    public record Catch(String id, List<Integer> pool) implements CustomPacketPayload {
        public static final Type<Catch> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "infuser_catch"));
        public static final StreamCodec<ByteBuf, Catch> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Catch::id, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), Catch::pool, Catch::new);
        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Set by the client setup. */
    public static Consumer<CompoundTag> clientSync = t -> {};
    public static Consumer<Inspired> clientInspired = i -> {};

    private static int[] validPool(List<Integer> pool) {
        Tint tint = Tint.get();
        if (pool.size() != tint.resins().size()) return null;
        int[] q = new int[pool.size()];
        long tot = 0;
        for (int i = 0; i < q.length; i++) { q[i] = pool.get(i); if (q[i] < 0) return null; tot += q[i]; }
        return tot <= tint.capSpecks() ? q : null;
    }

    private static void onPool(Pool msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        int[] q = validPool(msg.pool());
        if (q == null) return;
        InfuserState s = get(player);
        s.pool = q;
        s.chasing = Tint.get().target(msg.chasing()) != null && s.level(msg.chasing()) > 0 ? msg.chasing() : s.chasing;
        s.sealed = msg.sealed().isEmpty() || !s.caught.containsKey(msg.sealed()) ? null : msg.sealed();
        persisted(player).put(KEY, s.save());
    }

    private static void onCatch(Catch msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        Tint tint = Tint.get();
        Tint.Target t = tint.target(msg.id());
        int[] q = validPool(msg.pool());
        InfuserState s = get(player);
        if (t == null || q == null || s.level(t.id()) <= 0) return;
        double d = tint.distance(Tint.specks(q), t);
        if (d > Tint.MATCH) return;
        int grade = d <= Tint.SPOT_ON ? 2 : 1;
        s.caught.merge(t.id(), grade, Math::max);
        for (String twin : t.twins()) s.caught.merge(twin, grade, Math::max);
        s.pool = q;
        s.sealed = t.id();
        put(player, s);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> {
            var r = event.registrar("1");
            r.playToClient(Sync.TYPE, Sync.CODEC, (m, ctx) -> ctx.enqueueWork(() -> clientSync.accept(m.state())));
            r.playToClient(Inspired.TYPE, Inspired.CODEC, (m, ctx) -> ctx.enqueueWork(() -> clientInspired.accept(m)));
            r.playToServer(Pool.TYPE, Pool.CODEC, (m, ctx) -> ctx.enqueueWork(() -> onPool(m, ctx)));
            r.playToServer(Catch.TYPE, Catch.CODEC, (m, ctx) -> ctx.enqueueWork(() -> onCatch(m, ctx)));
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) PacketDistributor.sendToPlayer(p, new Sync(get(p).save()));
        });
    }
}
