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
        if (player.connection.hasChannel(Sync.TYPE)) PacketDistributor.sendToPlayer(player, new Sync(s.save()));
    }

    /** A Think Tank study's reward: the colour counts as caught, the plainer of the two grades. */
    public static void research(ServerPlayer player, String id) {
        InfuserState s = get(player);
        s.caught.merge(id, 1, Math::max);
        Tint.Target t = Tint.get().target(id);
        if (t != null) for (String twin : t.twins()) s.caught.merge(twin, 1, Math::max);
        put(player, s);
    }

    /** Colours this player has caught (their terpenes are unlocked for the Terpinator). */
    public static boolean caught(Player player, String id) { return get(player).caught.containsKey(id); }

    // ------------------------------------------------------------------ inspiration

    /** The colours a strain can inspire: Landraces the 16 Minecraft colours; a bred strain its hue row's four tones; a hybrid all of its parents'. */
    public static List<String> colours(Strain strain) {
        var parents = com.malevolentgods.fourtwenty.domain.Crosses.parents(strain);
        if (!parents.isEmpty()) return parents.stream().flatMap(p -> colours(p).stream()).distinct().toList();
        boolean landrace = strain == Strain.LANDRACE || strain.family() == Strain.Family.BIOME;
        List<String> out = new ArrayList<>();
        for (Tint.Target t : Tint.get().targets())
            if (landrace ? t.minecraft() : t.id().startsWith(strain.id() + "_")) out.add(t.id());
        return out;
    }

    /** A hit: one or two of the strain's colours the player isn't yet fully inspired by gain a level. */
    public static boolean inspire(ServerPlayer player, Strain strain) {
        InfuserState s = get(player);
        // a strain inspires its own four colours first; every weed, strains included, also inspires the Minecraft colours
        List<String> own = open(s, colours(strain));
        List<String> basic = strain == Strain.LANDRACE ? new ArrayList<>() : open(s, colours(Strain.LANDRACE));
        if (own.isEmpty() && basic.isEmpty()) {
            // nothing left for this weed to show: say so, so it doesn't feel broken
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(strain == Strain.LANDRACE
                    ? "Plain weed has shown you all it can. A strain might show you something new."
                    : "You've seen all " + strain.displayName() + " has to show you. Another strain might show you something new.")
                    .withStyle(net.minecraft.ChatFormatting.ITALIC, net.minecraft.ChatFormatting.GRAY), true);
            return false;
        }
        var random = player.getRandom();
        List<String> picked = new ArrayList<>();
        if (!own.isEmpty()) {
            picked.add(own.remove(random.nextInt(own.size())));
            // Landrace: sometimes a second of its own; a strain: often a Minecraft colour alongside
            List<String> extra = strain == Strain.LANDRACE ? own : basic;
            if (!extra.isEmpty() && random.nextFloat() < (strain == Strain.LANDRACE ? 0.35f : 0.5f)) picked.add(extra.remove(random.nextInt(extra.size())));
        } else {
            picked.add(basic.remove(random.nextInt(basic.size())));
            if (!basic.isEmpty() && random.nextFloat() < 0.35f) picked.add(basic.remove(random.nextInt(basic.size())));
        }
        int top = 0;
        for (String id : picked) { int l = s.level(id) + 1; s.inspiration.put(id, l); top = Math.max(top, l); }
        if (s.chasing == null) s.chasing = picked.get(0);
        put(player, s);
        PacketDistributor.sendToPlayer(player, new Inspired(picked, top));
        return true;
    }

    /** The colours among these that aren't fully inspired yet. */
    private static List<String> open(InfuserState s, List<String> ids) {
        return new ArrayList<>(ids.stream().filter(id -> s.level(id) < InfuserState.MAX).toList());
    }

    /** The drifting moment without new inspiration: the colours this player is most inspired by (level 0: no whisper). */
    public static void woozy(ServerPlayer player) {
        List<String> ids = get(player).inspiration.entrySet().stream().filter(e -> e.getValue() > 0)
                .sorted((a, b) -> b.getValue() - a.getValue()).limit(3).map(java.util.Map.Entry::getKey).toList();
        PacketDistributor.sendToPlayer(player, new Inspired(ids, 0));
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
        if (d > Tint.MATCH || !Infusion.paid(player)) return;
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
            r.playToServer(Infusion.Infuse.TYPE, Infusion.Infuse.CODEC, (m, ctx) -> ctx.enqueueWork(() -> Infusion.onInfuse(m, ctx)));
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.connection.hasChannel(Sync.TYPE)) PacketDistributor.sendToPlayer(p, new Sync(get(p).save()));
        });
    }
}
