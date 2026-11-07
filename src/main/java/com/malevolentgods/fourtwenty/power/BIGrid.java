package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The Wick network's bookkeeping: the level's version, bumped when a Wick or BI block is placed or removed so {@link Circuits} walks its
 * networks again, and one-off draws from stored power for machines with no buffer of their own.
 */
public final class BIGrid {
    private static final Map<Level, int[]> VERSIONS = new WeakHashMap<>();

    private BIGrid() {}

    public static int version(Level level) {
        return VERSIONS.computeIfAbsent(level, l -> new int[1])[0];
    }

    /** A Wick or BI block was placed or removed. */
    public static void changed(Level level) {
        VERSIONS.computeIfAbsent(level, l -> new int[1])[0]++;
    }

    /** Levels where a chunk has loaded since the networks were last walked, and when they were last told to walk again. */
    private static final Map<Level, long[]> LOADED = new WeakHashMap<>();
    /** At most one re-walk a second for chunks loading, however fast someone flies. */
    private static final int LOAD_EVERY = 20;

    /**
     * A chunk loaded: a network that runs into it was walked without it (a walk stops at unloaded blocks), so every network walks again,
     * soon. Without this a build across a chunk border came up half-wired after a restart, or when its owner came back to it.
     */
    public static void chunkLoaded(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) LOADED.computeIfAbsent(level, l -> new long[]{0, Long.MIN_VALUE / 2})[0] = 1;
    }

    /** At the end of each level tick: walk again if a chunk has loaded and it's been a second since the last time. */
    public static void levelTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        Level level = event.getLevel();
        long[] state = LOADED.get(level);
        if (state == null || state[0] == 0 || level.getGameTime() - state[1] < LOAD_EVERY) return;
        state[0] = 0;
        state[1] = level.getGameTime();
        changed(level);
    }

    private record Step(BlockPos pos, Direction from, BlockPos parent) {}

    /** Takes {@code amount} from whatever stores power on the Wicks touching {@code start}, all or nothing, for machines with no buffer of their own. */
    public static boolean drain(Level level, BlockPos start, long amount) {
        List<BINode> stores = stores(level, start);
        if (stores.stream().mapToLong(BINode::stored).sum() < amount) return false;
        long left = amount;
        for (BINode node : stores) left -= node.extract(left);
        return left == 0;
    }

    /** How much power is stored on the Wicks touching {@code start}. */
    public static long stored(Level level, BlockPos start) {
        return stores(level, start).stream().mapToLong(BINode::stored).sum();
    }

    private static List<BINode> stores(Level level, BlockPos start) {
        List<BINode> stores = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        seen.add(start);
        ArrayDeque<Step> queue = new ArrayDeque<>();
        for (Direction face : Direction.values()) queue.add(new Step(start.relative(face), face, null));
        while (!queue.isEmpty()) {
            Step step = queue.poll();
            BlockPos at = step.pos();
            if (!level.isLoaded(at) || !seen.add(at)) continue;
            if (level.getBlockState(at).getBlock() instanceof WickBlock) {
                for (Direction next : Direction.values()) queue.add(new Step(at.relative(next), next, null));
                continue;
            }
            BINode node = level.getCapability(BINode.CAPABILITY, at, step.from().getOpposite());
            if (node != null && node.canExtract()) stores.add(node);
        }
        return stores;
    }
}
