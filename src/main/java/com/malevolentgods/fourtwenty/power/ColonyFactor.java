package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.college.ColonyEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The one place the shared machine rules ask what a colony's Weed College studies do at a position (specs/017-weed-college research R7):
 * Terpinator and Grow Op BI, Hookah burns, Stash capacity, trimmer and curer speed, Rosin Rig and Pheno Chamber speed. Never the
 * Terpinator's or the Infuser's speed (constitution Principle II). Outside a colony, on the client, or without MineColonies, every answer
 * is the neutral one, so nothing changes.
 */
public final class ColonyFactor {
    private ColonyFactor() {}

    /** Answers for a position; the MineColonies integration sets it (cached per colony there). */
    public interface Lookup {
        double factor(Level level, BlockPos pos, ColonyEffects.Effect effect);
    }

    public static volatile Lookup lookup = (level, pos, effect) -> effect.neutral;

    public static double of(Level level, BlockPos pos, ColonyEffects.Effect effect) {
        if (level == null || level.isClientSide()) return effect.neutral;
        return lookup.factor(level, pos, effect);
    }

    /** A work time at the colony's speed for {@code effect}. */
    public static int ticks(Level level, BlockPos pos, ColonyEffects.Effect effect, int base) {
        return ColonyEffects.scaledTicks(base, of(level, pos, effect));
    }

    /** A BI cost at the colony's factor for {@code effect}. */
    public static double cost(Level level, BlockPos pos, ColonyEffects.Effect effect, double base) {
        return ColonyEffects.scaledCost(base, of(level, pos, effect));
    }

    /** A burn or a capacity at the colony's factor for {@code effect}. */
    public static long length(Level level, BlockPos pos, ColonyEffects.Effect effect, long base) {
        return ColonyEffects.scaledLength(base, of(level, pos, effect));
    }
}
