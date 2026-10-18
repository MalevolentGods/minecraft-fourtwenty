package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MenuProductionRulesTest {
    @Test void itemsAreOnlyProducedOnceTheirLevelIsReached() {
        assertEquals(0, ProductionRules.target(MenuItem.COOKIE, 1));
        assertEquals(16, ProductionRules.target(MenuItem.COOKIE, 2));
        assertEquals(0, ProductionRules.target(MenuItem.PIPE, 2));
        assertEquals(4, ProductionRules.target(MenuItem.PIPE, 3));
        assertEquals(0, ProductionRules.target(MenuItem.BROWNIE, 3));
        assertEquals(32, ProductionRules.target(MenuItem.BROWNIE, 5));
        assertEquals(0, ProductionRules.target(MenuItem.PIPE, 6));
    }
    @Test void pipesAreMadeAFewAtATime() {
        assertEquals(4, ProductionRules.batch(MenuItem.PIPE, 5, 0, false));
        assertEquals(16, ProductionRules.batch(MenuItem.BROWNIE, 5, 0, false));
        assertEquals(0, ProductionRules.batch(MenuItem.COOKIE, 1, 0, false));
    }
    @Test void levelOneKeepsTheOriginalIngredientsAndHigherLevelsAddTheirRecipes() {
        assertEquals(Map.of("minecraft:paper", 32, "fourtwenty:weed_bud", 16), ProductionRules.ingredientsKept(1));
        var five = ProductionRules.ingredientsKept(5);
        assertEquals(16 + 8 + 2 + 4, five.get("fourtwenty:weed_bud"));
        assertEquals(8, five.get("minecraft:cookie"));
        assertEquals(2, five.get("minecraft:charcoal"));
        assertEquals(16, five.get("minecraft:cocoa_beans"));
    }
    @Test void everyRecipeUsesOneBud() {
        for (var item : MenuItem.values()) {
            assertEquals(1, ProductionRules.recipe(item).stream().filter(i -> i.itemId().equals("fourtwenty:weed_bud")).mapToInt(ProductionRules.Input::count).sum());
        }
    }
}
