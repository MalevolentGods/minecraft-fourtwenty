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

    /**
     * What a visit would add. Water and fuel are kept ready at all times (set-up); buds only while
     * {@code serving} customers, since a bong with all three lights at once.
     */
    public static Need need(int buds, boolean hasWater, String fuelId, boolean serving) {
        boolean usableFuel = BongFuel.isFuel(fuelId);
        return new Need(serving && buds <= 0, !hasWater, !usableFuel, EMPTY_BUCKET.equals(fuelId));
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
     * Worth the walk when the budtender can supply something the bong is missing. Water is free;
     * missing buds or fuel must be in stock (otherwise they are requested instead).
     */
    public static boolean canFix(Need need, int budsAvailable, Optional<String> fuelAvailable) {
        return (need.buds() && budsAvailable > 0) || need.water() || need.clearBucket()
                || (need.fuel() && fuelAvailable.isPresent());
    }

    private BongTendingRules() {}
}
