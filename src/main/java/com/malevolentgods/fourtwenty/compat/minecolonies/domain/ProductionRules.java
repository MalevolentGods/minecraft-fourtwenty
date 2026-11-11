package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Targets are individual items, never inventory slots or stacks. */
public final class ProductionRules {
    /** One ingredient of a menu recipe, as a registry id and count. */
    public record Input(String itemId, int count) {}

    /** The Budtender's recipes; the same inputs as the crafting-table recipes. */
    public static List<Input> recipe(MenuItem item) {
        return switch (item) {
            case JOINT -> List.of(new Input("minecraft:paper", 2), new Input("fourtwenty:weed_bud", 1));
            case COOKIE -> List.of(new Input("minecraft:cookie", 1), new Input("fourtwenty:weed_bud", 1));
            case PIPE -> List.of(new Input("minecraft:oak_planks", 1), new Input("minecraft:birch_planks", 1),
                    new Input("minecraft:spruce_planks", 1), new Input("minecraft:charcoal", 1), new Input("fourtwenty:weed_bud", 1));
            case BROWNIE -> List.of(new Input("minecraft:cocoa_beans", 4), new Input("minecraft:wheat", 4), new Input("fourtwenty:weed_bud", 1));
        };
    }

    public static int stockTarget(int completedLevel) { return target(MenuItem.JOINT, completedLevel); }

    /** Replenishment target per menu item; zero below the level that adds it to the menu. */
    public static int target(MenuItem item, int completedLevel) {
        // Unknown levels (outside the hut's 1–5) produce nothing, as before.
        if (completedLevel < item.level || completedLevel > 5) return 0;
        int level = completedLevel;
        return switch (item) {
            case JOINT -> new int[]{0, 16, 32, 64, 96, 128}[level];
            case COOKIE -> new int[]{0, 0, 16, 16, 32, 32}[level];
            case PIPE -> new int[]{0, 0, 0, 4, 8, 12}[level];
            case BROWNIE -> new int[]{0, 0, 0, 0, 16, 32}[level];
        };
    }

    public static int batch(int completedLevel, int held, boolean outstanding) {
        return batch(MenuItem.JOINT, completedLevel, held, outstanding);
    }

    /** One request at a time per item; pipes don't stack, so they are made a few at a time. */
    public static int batch(MenuItem item, int completedLevel, int held, boolean outstanding) {
        int size = item == MenuItem.PIPE ? 4 : 16;
        return outstanding ? 0 : Math.min(size, Math.max(0, target(item, completedLevel) - held));
    }

    /** Raw buds the Budtender never crafts from (bong sessions, the hookah), by Dispensary level. */
    public static int budReserve(int completedLevel) {
        return completedLevel < 1 || completedLevel > 5 ? 0 : new int[]{0, 4, 6, 8, 12, 16}[completedLevel];
    }

    /** How many requested items can be crafted without dipping into the raw-bud reserve. */
    public static int craftableCount(int completedLevel, int requested, int buds) {
        return Math.min(Math.max(0, requested), Math.max(0, buds - budReserve(completedLevel)));
    }

    /** Small batches so stock is shared out between the menu items. */
    public static int smallBatch(MenuItem item) {
        return switch (item) { case JOINT -> 8; case COOKIE, BROWNIE -> 4; case PIPE -> 2; };
    }

    /**
     * The next production step: {@code item} x {@code count} to request (null when nothing is
     * needed), and {@code budsToRequest} raw buds to ask the colony for.
     */
    public record Step(MenuItem item, int count, int budsToRequest) {}

    /**
     * Balanced production. One batch at a time, always for the menu item least stocked relative to
     * its target. Requests do not wait for local buds: the native request tree must be able to ask
     * for every ingredient. The reserve is enforced when each item is crafted. Direct top-ups cover
     * what the native ingredient request will not supply and restart production at the reserve.
     */
    public static Step next(int completedLevel, java.util.Map<MenuItem, Integer> held, int buds,
                            boolean productionOpen, boolean budRequestOpen) {
        MenuItem neediest = null;
        double lowest = Double.MAX_VALUE;
        for (var item : MenuItem.menu(completedLevel)) {
            int target = target(item, completedLevel);
            int have = held.getOrDefault(item, 0);
            if (target <= 0 || have >= target) continue;
            double fill = have / (double) target;
            if (fill < lowest) { lowest = fill; neediest = item; }
        }
        int reserve = budReserve(completedLevel);
        int want = neediest == null ? 0 : Math.min(smallBatch(neediest), target(neediest, completedLevel) - held.getOrDefault(neediest, 0));
        // With less local stock than a full batch, the native child bud request obtains the missing
        // buds externally. Otherwise it can use local stock and we top up the reserve too.
        boolean nativeRequestSuppliesBatch = !productionOpen && neediest != null && buds < want;
        int budsWanted = productionOpen
                ? (buds == reserve ? reserve + want : reserve)
                : reserve + (nativeRequestSuppliesBatch ? 0 : want);
        int budsToRequest = budRequestOpen ? 0 : Math.max(0, budsWanted - buds);
        int count = productionOpen || neediest == null ? 0 : want;
        return new Step(count > 0 ? neediest : null, count, budsToRequest);
    }

    /**
     * Ingredients the hut keeps on hand for its menu: a few batches' worth per item, plus the raw
     * bud reserve. At level 1 this is the original 16 buds and 32 paper, plus 4 reserve buds.
     */
    public static Map<String, Integer> ingredientsKept(int completedLevel) {
        var kept = new LinkedHashMap<String, Integer>();
        for (var item : MenuItem.menu(completedLevel)) {
            int batches = switch (item) { case JOINT -> 16; case COOKIE -> 8; case BROWNIE -> 4; case PIPE -> 2; };
            for (var input : recipe(item)) kept.merge(input.itemId(), input.count() * batches, Integer::sum);
        }
        if (!kept.isEmpty()) kept.merge("fourtwenty:weed_bud", budReserve(completedLevel), Integer::sum);
        return kept;
    }

    private ProductionRules() {}
}
