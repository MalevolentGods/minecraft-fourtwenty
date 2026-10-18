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

    /**
     * Ingredients the hut keeps on hand for its menu: a few batches' worth per item. At level 1
     * this is the original 16 buds and 32 paper.
     */
    public static Map<String, Integer> ingredientsKept(int completedLevel) {
        var kept = new LinkedHashMap<String, Integer>();
        for (var item : MenuItem.menu(completedLevel)) {
            int batches = switch (item) { case JOINT -> 16; case COOKIE -> 8; case BROWNIE -> 4; case PIPE -> 2; };
            for (var input : recipe(item)) kept.merge(input.itemId(), input.count() * batches, Integer::sum);
        }
        return kept;
    }

    private ProductionRules() {}
}
