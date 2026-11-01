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
 * The Wick network. Wicks are plain blocks with nothing ticking, so a long run costs nothing: a source walks the Wicks once, keeps the
 * route, and walks again when anything on any network changes (the level's version) and now and then for chunks loading and unloading.
 * What a Wick carries is added up over each second, across every source on it.
 */
public final class BIGrid {
    private static final Map<Level, int[]> VERSIONS = new WeakHashMap<>();
    private static final Map<Level, Flow> FLOWS = new WeakHashMap<>();

    private BIGrid() {}

    private static final class Flow {
        long second = -1;
        final Map<BlockPos, Long> sent = new HashMap<>();
    }

    public static int version(Level level) {
        return VERSIONS.computeIfAbsent(level, l -> new int[1])[0];
    }

    /** A Wick or BI block was placed or removed. */
    public static void changed(Level level) {
        VERSIONS.computeIfAbsent(level, l -> new int[1])[0]++;
    }

    /** A sink, and the Wicks power takes to reach it. */
    public record Sink(BINode node, List<BlockPos> path) {}

    /** What a source reaches: the Wicks it lights, the nodes it feeds, and the blocks too low a tier to take it. */
    public record Route(List<BlockPos> wicks, List<Sink> sinks, List<BINode> overloaded) {}

    private record Step(BlockPos pos, Direction from, BlockPos parent) {}

    private static List<BlockPos> path(Map<BlockPos, BlockPos> parents, BlockPos wick) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos at = wick; at != null; at = parents.get(at)) out.add(at);
        return out;
    }

    public static Route route(Level level, BlockPos start, Collection<Direction> faces, BIRules.Tier tier) {
        List<BlockPos> wicks = new ArrayList<>();
        List<Sink> sinks = new ArrayList<>();
        List<BINode> overloaded = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Map<BlockPos, BlockPos> parents = new HashMap<>();
        seen.add(start);
        ArrayDeque<Step> queue = new ArrayDeque<>();
        for (Direction face : faces) queue.add(new Step(start.relative(face), face, null));
        while (!queue.isEmpty()) {
            Step step = queue.poll();
            BlockPos at = step.pos();
            if (!level.isLoaded(at)) continue;
            if (level.getBlockState(at).getBlock() instanceof WickBlock) {
                if (!seen.add(at)) continue;
                if (step.parent() != null) parents.put(at, step.parent());
                wicks.add(at);
                for (Direction next : Direction.values()) queue.add(new Step(at.relative(next), next, at));
                continue;
            }
            BINode node = level.getCapability(BINode.CAPABILITY, at, step.from().getOpposite());
            if (node == null || !node.canReceive()) continue;
            if (BIRules.overloads(node.tier(), tier)) {
                if (!overloaded.contains(node)) overloaded.add(node);
            } else if (sinks.stream().noneMatch(s -> s.node() == node)) {
                sinks.add(new Sink(node, step.parent() == null ? List.of() : path(parents, step.parent())));
            }
        }
        return new Route(wicks, sinks, overloaded);
    }

    /** Adds {@code amount} to what these Wicks have carried this second, and burns out any that's over its limit. */
    public static void carried(Level level, List<BlockPos> wicks, long amount) {
        Flow flow = FLOWS.computeIfAbsent(level, l -> new Flow());
        long second = level.getGameTime() / BIRules.TICKS_PER_SECOND;
        if (flow.second != second) {
            flow.second = second;
            flow.sent.clear();
        }
        for (BlockPos pos : wicks) {
            long sent = flow.sent.merge(pos, amount, Long::sum);
            if (level.getBlockState(pos).getBlock() instanceof WickBlock wick && BIRules.wireBurns(wick.tier(), sent)) BurnOut.burn(level, pos);
        }
    }

    /** Takes {@code amount} from whatever stores power on the Wicks touching {@code start}, all or nothing, for machines with no buffer of their own. */
    public static boolean drain(Level level, BlockPos start, long amount) {
        List<BINode> stores = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        seen.add(start);
        ArrayDeque<Step> queue = new ArrayDeque<>();
        for (Direction face : Direction.values()) queue.add(new Step(start.relative(face), face, null));
        long total = 0;
        while (!queue.isEmpty()) {
            Step step = queue.poll();
            BlockPos at = step.pos();
            if (!level.isLoaded(at) || !seen.add(at)) continue;
            if (level.getBlockState(at).getBlock() instanceof WickBlock) {
                for (Direction next : Direction.values()) queue.add(new Step(at.relative(next), next, null));
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
