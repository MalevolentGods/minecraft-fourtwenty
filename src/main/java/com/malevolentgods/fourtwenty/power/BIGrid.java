package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The Wick network. Wicks are plain blocks with nothing ticking, so a long run costs nothing: a source walks the Wicks once, keeps the
 * route, and walks again when anything on any network changes (the level's version) and now and then for chunks loading and unloading.
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

    /** What a source reaches: the Wicks it lights, the nodes it feeds, and what it would burn out. */
    public record Route(List<BlockPos> wicks, List<BINode> sinks, List<BlockPos> overloaded) {}

    private record Step(BlockPos pos, Direction from) {}

    public static Route route(Level level, BlockPos start, Collection<Direction> faces, BIRules.Tier tier) {
        List<BlockPos> wicks = new ArrayList<>(), overloaded = new ArrayList<>();
        List<BINode> sinks = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        seen.add(start);
        ArrayDeque<Step> queue = new ArrayDeque<>();
        for (Direction face : faces) queue.add(new Step(start.relative(face), face));
        while (!queue.isEmpty()) {
            Step step = queue.poll();
            BlockPos at = step.pos();
            if (!level.isLoaded(at)) continue;
            if (level.getBlockState(at).getBlock() instanceof WickBlock wick) {
                if (!seen.add(at)) continue;
                if (BIRules.overloads(wick.tier(), tier)) {
                    overloaded.add(at);
                    continue;
                }
                wicks.add(at);
                for (Direction next : Direction.values()) queue.add(new Step(at.relative(next), next));
                continue;
            }
            BINode node = level.getCapability(BINode.CAPABILITY, at, step.from().getOpposite());
            if (node == null || !node.canReceive()) continue;
            if (BIRules.overloads(node.tier(), tier)) {
                if (seen.add(at)) overloaded.add(at);
            } else if (!sinks.contains(node)) {
                sinks.add(node);
            }
        }
        return new Route(wicks, sinks, overloaded);
    }

    /** Takes {@code amount} from whatever stores power on the Wicks touching {@code start}, all or nothing, for machines with no buffer of their own. */
    public static boolean drain(Level level, BlockPos start, long amount) {
        List<BINode> stores = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        seen.add(start);
        ArrayDeque<Step> queue = new ArrayDeque<>();
        for (Direction face : Direction.values()) queue.add(new Step(start.relative(face), face));
        long total = 0;
        while (!queue.isEmpty()) {
            Step step = queue.poll();
            BlockPos at = step.pos();
            if (!level.isLoaded(at) || !seen.add(at)) continue;
            if (level.getBlockState(at).getBlock() instanceof WickBlock) {
                for (Direction next : Direction.values()) queue.add(new Step(at.relative(next), next));
                continue;
            }
            BINode node = level.getCapability(BINode.CAPABILITY, at, step.from().getOpposite());
            if (node != null && node.canExtract()) {
                stores.add(node);
                total += node.stored();
            }
        }
        if (total < amount) return false;
        long left = amount;
        for (BINode node : stores) left -= node.extract(left);
        return left == 0;
    }
}
