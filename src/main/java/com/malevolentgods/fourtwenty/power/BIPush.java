package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.function.Predicate;

/** One source's push onto its network, keeping its route between ticks, and the part of a BI left over from a fractional rate. */
public final class BIPush {
    private static final int REWALK = 100, GLOW = 10;

    private BIGrid.Route route;
    private int version = -1;
    private long walked;
    private double credit;
    private boolean carrying;

    /** Whether there's anything on this route to give power to, as of the last push. */
    public boolean hasSinks() {
        return route != null && !route.sinks().isEmpty();
    }

    /** Pushes {@code rate} BI per tick from {@code from}, as power of {@code tier}, shared between the sinks {@code accept} allows. Returns what moved. */
    public long push(Level level, BlockPos pos, Collection<Direction> faces, BIRules.Tier tier, double rate, BINode from, Predicate<BINode> accept) {
        long now = level.getGameTime();
        if (route == null || version != BIGrid.version(level) || now - walked >= REWALK) {
            version = BIGrid.version(level);
            walked = now;
            route = BIGrid.route(level, pos, faces, tier);
        }
        if (from.stored() <= 0) return 0;
        route.overloaded().forEach(node -> node.overload(now));
        credit = Math.min(credit + rate, rate + 1);
        var sinks = route.sinks().stream().filter(s -> accept.test(s.node())).toList();
        long left = Math.min(from.stored(), (long) credit);
        long moved = 0;
        for (int i = 0; i < sinks.size() && left > 0; i++) {
            BIGrid.Sink sink = sinks.get(i);
            long taken = sink.node().receive(BIRules.share(left, sinks.size() - i));
            if (taken <= 0) continue;
            left -= taken;
            moved += taken;
            BIGrid.carried(level, sink.path(), taken);
        }
        if (moved > 0) {
            credit -= moved;
            from.extract(moved);
            carrying = true;
        }
        if (carrying && now % GLOW == 0) {
            route.wicks().forEach(p -> WickBlock.carry(level, p));
            carrying = false;
        }
        return moved;
    }
}
