package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import com.malevolentgods.fourtwenty.domain.BongFuel;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a budtender does to keep a dispensary bong going. Pure registry-id rules so they can be
 * unit tested without a game.
 */
public final class BongTendingRules {
    public static final int MAX_BUDS = 4;
    /** Blaze powder is topped up to this many lightings; flint and steel and lava buckets go in one at a time. */
    public static final int MAX_BLAZE_POWDER = 4;
    public static final String EMPTY_BUCKET = "minecraft:bucket";
    /** Best fuel first, so the bong burns as long as possible per visit. */
    public static final List<String> FUEL_PREFERENCE = List.of(BongFuel.BLAZE_POWDER, BongFuel.LAVA_BUCKET, BongFuel.FLINT_AND_STEEL);

    /** What the bong is missing. {@code fuelId} is whatever sits in the fuel slot ("minecraft:air" when empty). */
    public record Need(boolean buds, boolean water, boolean fuel, boolean clearBucket) {
        public boolean any() { return buds || water || fuel || clearBucket; }
    }

    /** A bong needs a visit when it would not (re)light by itself: no buds, no water, or no usable fuel. */
    public static Need need(int buds, boolean hasWater, String fuelId) {
        boolean usableFuel = BongFuel.isFuel(fuelId);
        return new Need(buds <= 0, !hasWater, !usableFuel, EMPTY_BUCKET.equals(fuelId));
    }

    /** The best fuel the budtender can get, given counts available by item id. */
    public static Optional<String> bestFuel(Map<String, Integer> available) {
        return FUEL_PREFERENCE.stream().filter(id -> available.getOrDefault(id, 0) > 0).findFirst();
    }

    /** How many of {@code fuelId} to put in one visit. */
    public static int fuelAmount(String fuelId) {
        return BongFuel.BLAZE_POWDER.equals(fuelId) ? MAX_BLAZE_POWDER : 1;
    }

    /**
     * Worth the walk only if, after this visit, the bong could light: every missing input must be
     * available (a full bong that is merely lit never needs tending).
     */
    public static boolean canFix(Need need, int budsAvailable, boolean waterAvailable, Optional<String> fuelAvailable) {
        if (!need.any()) return false;
        return (!need.buds() || budsAvailable > 0) && (!need.water() || waterAvailable) && (!need.fuel() || fuelAvailable.isPresent());
    }

    private BongTendingRules() {}
}
