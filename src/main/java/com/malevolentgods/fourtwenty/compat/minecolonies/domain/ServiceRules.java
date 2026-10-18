package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What one Dispensary visit hands a customer, by the customer's home level:
 * levels 1–2 consume at the shop; 3–4 carry a supply away; 5 do both. A missing take-away falls
 * back to something consumed at the shop, and the customer complains.
 *
 * <p>Pure rules so they can be unit tested without a game.
 */
public final class ServiceRules {
    /** Edibles are handed out four at a time; a pipe already holds four doses. */
    public static final int EDIBLE_TAKE_AWAY = 4;

    /** {@code onSpot}/{@code takeAway} are null when not handed out. */
    public record Plan(MenuItem onSpot, MenuItem takeAway, int takeAwayCount, boolean complain) {
        public static final Plan NOTHING = new Plan(null, null, 0, false);
        public boolean any() { return onSpot != null || takeAway != null; }
    }

    public static Plan plan(int homeLevel, Preference preference, Map<MenuItem, Integer> stock) {
        if (homeLevel < 1 || preference == Preference.NONE) return Plan.NOTHING;
        var left = new EnumMap<MenuItem, Integer>(MenuItem.class);
        for (var item : MenuItem.values()) left.put(item, Math.max(0, stock.getOrDefault(item, 0)));
        if (homeLevel <= 2) {
            var spot = best(preference.onSpot, left);
            return spot == null ? Plan.NOTHING : new Plan(spot, null, 0, false);
        }
        if (homeLevel <= 4) {
            var take = best(preference.takeAway, left);
            if (take != null) return new Plan(null, take, count(take, left), false);
            var spot = best(preference.onSpot, left);
            return spot == null ? Plan.NOTHING : new Plan(spot, null, 0, true);
        }
        // Level 5: a session at the shop, then a supply to take home.
        var spot = best(preference.onSpot, left);
        if (spot != null) left.merge(spot, -1, Integer::sum);
        var take = best(preference.takeAway, left);
        if (spot == null && take == null) return Plan.NOTHING;
        return new Plan(spot, take, take == null ? 0 : count(take, left), take == null);
    }

    private static MenuItem best(List<MenuItem> choices, Map<MenuItem, Integer> stock) {
        for (var item : choices) if (stock.get(item) > 0) return item;
        return null;
    }

    private static int count(MenuItem item, Map<MenuItem, Integer> stock) {
        return item.edible ? Math.min(EDIBLE_TAKE_AWAY, stock.get(item)) : 1;
    }

    private ServiceRules() {}
}
