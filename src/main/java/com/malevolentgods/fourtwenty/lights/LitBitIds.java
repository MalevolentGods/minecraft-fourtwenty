package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.domain.LitBitIdRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lit Bit IDs on one level. Live lamps register as they load and go as they unload (not saved: their block entities hold their IDs).
 * When a lamp is broken its ID is remembered at its position (saved), so a lamp placed there again gets it back.
 */
public final class LitBitIds extends SavedData {
    private static final String NAME = "fourtwenty_lit_bit_ids";
    /** A remembered ID is forgotten after 30 days of game time with nothing placed there. */
    private static final long FORGET_TICKS = 30L * 24_000;

    private final Map<Long, int[]> remembered = new HashMap<>();
    private final Map<BlockPos, Integer> live = new HashMap<>();

    public static LitBitIds of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(LitBitIds::new, LitBitIds::load), NAME);
    }

    private static LitBitIds load(CompoundTag tag, HolderLookup.Provider registries) {
        LitBitIds ids = new LitBitIds();
        for (Tag t : tag.getList("remembered", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            ids.remembered.put(e.getLong("pos"), new int[]{e.getInt("id"), (int) (e.getLong("at") >> 0)});
        }
        return ids;
    }

    @Override public @Nonnull CompoundTag save(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        remembered.forEach((pos, e) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("pos", pos);
            c.putInt("id", e[0]);
            c.putLong("at", e[1]);
            list.add(c);
        });
        tag.put("remembered", list);
        return tag;
    }

    public void live(BlockPos pos, int id) { live.put(pos.immutable(), id); }

    public void gone(BlockPos pos) { live.remove(pos); }

    /** A lamp was broken: keep its ID for this spot. */
    public void remember(BlockPos pos, int id, long now) {
        remembered.put(pos.asLong(), new int[]{id, (int) Math.min(Integer.MAX_VALUE, now / 20)});
        setDirty();
    }

    /** The ID last broken here, taken back (it's in use again), or -1. Old ones are forgotten. */
    public int takeBack(BlockPos pos, long now) {
        int[] e = remembered.remove(pos.asLong());
        if (e == null) return -1;
        setDirty();
        return now / 20 - e[1] > FORGET_TICKS / 20 ? -1 : e[0];
    }

    /** The IDs of live lamps within {@code range} of {@code centre}. */
    public Set<Integer> idsNear(BlockPos centre, int range) {
        Set<Integer> out = new TreeSet<>();
        long r2 = (long) range * range;
        live.forEach((pos, id) -> { if (pos.distSqr(centre) <= r2) out.add(id); });
        return out;
    }

    /** The positions of live lamps with {@code id} within {@code range} of {@code centre}. */
    public List<BlockPos> lampsNear(BlockPos centre, int range, int id) {
        List<BlockPos> out = new ArrayList<>();
        long r2 = (long) range * range;
        live.forEach((pos, lamp) -> { if (lamp == id && pos.distSqr(centre) <= r2) out.add(pos); });
        return out;
    }

    /** Every live lamp within {@code range}, by position. */
    public Map<BlockPos, Integer> near(BlockPos centre, int range) {
        Map<BlockPos, Integer> out = new HashMap<>();
        long r2 = (long) range * range;
        live.forEach((pos, id) -> { if (pos.distSqr(centre) <= r2) out.put(pos, id); });
        return out;
    }

    /** The lowest ID not live within {@code range} of {@code centre}. */
    public int lowestFree(BlockPos centre, int range) {
        return LitBitIdRules.nextFree(new HashSet<>(idsNear(centre, range)), LitBitIdRules.MIN);
    }
}
