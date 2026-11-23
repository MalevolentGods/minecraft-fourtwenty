package com.malevolentgods.fourtwenty.handbook;

import com.malevolentgods.fourtwenty.domain.handbook.Scouting;
import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Deed;
import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Progress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The game tells the Handbook what a player did (specs/016-weedcraft-handbook, contracts/scouting-data.md): {@link #fire} ticks off
 * whatever requirements that completes, saves it with the player (so it survives death and restarts), and tells their client.
 * {@link #near} is for what a machine or a world event does with no player in hand: it counts for the nearest player in reach.
 */
public final class Deeds {
    private Deeds() {}

    private static final String KEY = "fourtwenty_handbook";
    /** How far from a machine or event a player still gets the credit. */
    public static final double REACH = 32;

    // ---- storage

    static CompoundTag root(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(KEY, Tag.TAG_COMPOUND)) persisted.put(KEY, new CompoundTag());
        return persisted.getCompound(KEY);
    }

    public static Progress progress(Player player) {
        CompoundTag r = root(player);
        Map<String, Integer> states = new LinkedHashMap<>(), counts = new LinkedHashMap<>();
        Map<String, Long> earned = new LinkedHashMap<>();
        CompoundTag s = r.getCompound("states"), c = r.getCompound("counts"), e = r.getCompound("earned");
        for (String k : s.getAllKeys()) states.put(k, s.getInt(k));
        for (String k : c.getAllKeys()) counts.put(k, c.getInt(k));
        for (String k : e.getAllKeys()) earned.put(k, e.getLong(k));
        Progress p = new Progress();
        p.restore(states, counts, earned);
        return p;
    }

    public static void save(Player player, Progress p) {
        CompoundTag s = new CompoundTag(), c = new CompoundTag(), e = new CompoundTag();
        p.states().forEach(s::putInt);
        p.countsAll().forEach(c::putInt);
        p.earnedAll().forEach(e::putLong);
        CompoundTag r = root(player);
        r.put("states", s);
        r.put("counts", c);
        r.put("earned", e);
    }

    // ---- firing

    public static void fire(ServerPlayer player, String deed, Map<String, String> subject, int amount) {
        if (player.isSpectator()) return;
        apply(player, new Deed(deed, subject, amount));
    }

    public static void fire(ServerPlayer player, String deed, String... kv) {
        fire(player, deed, Deed.of(deed, kv).subject(), 1);
    }

    /** For any entity that might be a player: what a block, an item or a click did. */
    public static void by(net.minecraft.world.entity.Entity who, String deed, String... kv) {
        if (who instanceof ServerPlayer p) fire(p, deed, kv);
    }

    public static String id(net.minecraft.world.level.block.Block block) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    public static String id(net.minecraft.world.item.Item item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** For the nearest player within reach of {@code pos}: what a machine finished, a plant came to, a wire burned out. */
    public static void near(Level level, BlockPos pos, String deed, int amount, String... kv) {
        if (!(level instanceof ServerLevel server)) return;
        ServerPlayer best = null;
        double bestSq = REACH * REACH;
        for (ServerPlayer p : server.players()) {
            if (p.isSpectator() || p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) continue;
            double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < bestSq) {
                bestSq = d;
                best = p;
            }
        }
        if (best != null) apply(best, new Deed(deed, Deed.of(deed, kv).subject(), amount));
    }

    private static void apply(ServerPlayer player, Deed deed) {
        Progress p = progress(player);
        var result = p.fire(ScoutingData.defs(), deed, player.level().getGameTime());
        if (!result.any()) {
            // counts moved even when nothing finished: keep them
            save(player, p);
            return;
        }
        save(player, p);
        sync(player, p, result);
        if (!result.earned().isEmpty()) HandbookNetwork.announce(player, result.earned());
    }

    /** A player signs an honour requirement (validated by the rules: it must be an open honour one). */
    public static void sign(ServerPlayer player, String key) {
        Progress p = progress(player);
        var result = p.sign(ScoutingData.defs(), key, player.level().getGameTime());
        if (!result.any()) return;
        save(player, p);
        sync(player, p, result);
        if (!result.earned().isEmpty()) HandbookNetwork.announce(player, result.earned());
    }

    // ---- sync

    public static void sync(ServerPlayer player, Progress p, @Nullable Scouting.Result result) {
        if (!player.connection.hasChannel(HandbookNetwork.ProgressPayload.TYPE)) return;
        PacketDistributor.sendToPlayer(player, new HandbookNetwork.ProgressPayload(result == null, p.states(), p.countsAll(), p.earnedAll(),
                result == null ? List.of() : result.ticked(), result == null ? List.of() : result.earned()));
        if (result != null && !result.earned().isEmpty()) HandbookNetwork.sashChanged(player, p);
    }

    public static void fullSync(ServerPlayer player) {
        Progress p = progress(player);
        sync(player, p, null);
    }
}
