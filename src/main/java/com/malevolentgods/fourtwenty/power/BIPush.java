package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.function.Predicate;

/** One source's push onto its network, keeping its route between ticks. */
public final class BIPush {
    private static final int REWALK = 100, GLOW = 10;

    private BIGrid.Route route;
    private int version = -1;
    private long walked;

    /** Whether there's anything on this route to give power to, as of the last push. */
    public boolean hasSinks() {
        return route != null && !route.sinks().isEmpty();
    }

    /** Pushes up to the tier's rate from {@code from}, shared between the sinks {@code accept} allows. Returns what moved. */
    public long push(Level level, BlockPos pos, Collection<Direction> faces, BIRules.Tier tier, BINode from, Predicate<BINode> accept) {
        long now = level.getGameTime();
        if (route == null || version != BIGrid.version(level) || now - walked >= REWALK) {
            version = BIGrid.version(level);
            walked = now;
            route = BIGrid.route(level, pos, faces, tier);
        }
        if (from.stored() <= 0) return 0;
        if (!route.overloaded().isEmpty()) {
            route.overloaded().forEach(p -> BurnOut.burn(level, p));
            route = null;
            return 0;
        }
        var sinks = route.sinks().stream().filter(accept).toList();
        long left = Math.min(from.stored(), tier.rate());
        long moved = 0;
        for (int i = 0; i < sinks.size() && left > 0; i++) {
            long taken = sinks.get(i).receive(BIRules.share(left, sinks.size() - i));
            left -= taken;
            moved += taken;
        }
        if (moved > 0) {
            from.extract(moved);
            if (now % GLOW == 0) route.wicks().forEach(p -> WickBlock.carry(level, p));
        }
        return moved;
    }
}
