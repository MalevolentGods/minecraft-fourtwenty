package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.MachinePowerRule;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.util.StrainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** The one rule for every powered machine, old and new. Machines ask here; they never test for a high player themselves. */
public final class MachinePower {
    private MachinePower() {}

    /** Holds the fraction of a BI a machine owes between ticks, since a cost per tick can be under one. */
    public static final class Meter {
        private double owed;
    }

    public static boolean highNear(Level level, BlockPos pos) {
        return level.players().stream().anyMatch(p -> !p.isSpectator() && StrainEffects.isHigh(p)
                && p.blockPosition().closerThan(pos, MachinePowerRule.RADIUS));
    }

    /** How far a machine with its own buffer gets this tick, paying its {@code cost} if it has to; 0 means it's paused. */
    public static int steps(Level level, BlockPos pos, BINode buffer, double cost, Meter meter) {
        long due = (long) Math.ceil(meter.owed + cost);
        return switch (MachinePowerRule.state(highNear(level, pos), buffer.stored(), due)) {
            case RUN_FREE -> MachinePowerRule.freeSteps(ProcessingConfig.highSpeedPercent(), level.random.nextInt(100));
            case RUN_PAID -> {
                meter.owed += cost;
                long whole = (long) meter.owed;
                buffer.extract(whole);
                meter.owed -= whole;
                yield 1;
            }
            case PAUSE -> 0;
        };
    }

    /** For a machine with no running job, like the Infuser: is this one action paid for? Free with a high player nearby. */
    public static boolean spendOnce(Level level, BlockPos pos, long cost) {
        return highNear(level, pos) || BIGrid.drain(level, pos, cost);
    }
}
