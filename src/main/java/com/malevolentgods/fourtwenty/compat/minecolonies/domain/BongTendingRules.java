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
    /** Charcoal is topped up to this many lightings. */
    public static final int MAX_CHARCOAL = 4;
    public static final String EMPTY = "minecraft:air";
    public static final List<String> FUEL_PREFERENCE = List.of(BongFuel.CHARCOAL);
    /** The budtender loads one bud, one session, per this many ticks: 10 minutes. */
    public static final int SESSION_EVERY = 12_000;

    /** What the bong is missing. {@code fuelId} is whatever sits in the fuel slot ("minecraft:air" when empty). */
    public record Need(boolean buds, boolean water, boolean fuel, boolean clearBucket, boolean light) {
        public boolean any() { return buds || water || fuel || clearBucket || light; }
    }

    /**
     * What a visit would add. Water and fuel are kept ready at all times (set-up); buds only while
     * {@code serving} customers. With everything in place and nothing burning, serving also means lighting it: a bong only lights itself when something needs its power.
     */
    public static Need need(int buds, boolean hasWater, String fuelId, boolean serving, boolean lit) {
        boolean usableFuel = BongFuel.isFuel(fuelId);
        return new Need(serving && buds <= 0, !hasWater, !usableFuel, !usableFuel && !EMPTY.equals(fuelId), serving && !lit && buds > 0 && hasWater && usableFuel);
    }

    /** What a bong that's already burning is missing. */
    public static Need need(int buds, boolean hasWater, String fuelId, boolean serving) {
        return need(buds, hasWater, fuelId, serving, true);
    }

    /** The best fuel the budtender can get, given counts available by item id. */
    public static Optional<String> bestFuel(Map<String, Integer> available) {
        return FUEL_PREFERENCE.stream().filter(id -> available.getOrDefault(id, 0) > 0).findFirst();
    }

    /** How many of {@code fuelId} to put in one visit. */
    public static int fuelAmount(String fuelId) {
        return MAX_CHARCOAL;
    }

    /**
     * Worth the walk when the budtender can supply something the bong is missing. Water is free;
     * missing buds or fuel must be in stock (otherwise they are requested instead).
     */
    public static boolean canFix(Need need, int budsAvailable, Optional<String> fuelAvailable) {
        return (need.buds() && budsAvailable > 0) || need.water() || need.clearBucket() || need.light()
                || (need.fuel() && fuelAvailable.isPresent());
    }

    /** Whether a new session may be loaded, given when the last one was (or a negative number if there's been none). */
    public static boolean sessionDue(long now, long lastLoaded) {
        return lastLoaded < 0 || now - lastLoaded >= SESSION_EVERY;
    }

    private BongTendingRules() {}
}
