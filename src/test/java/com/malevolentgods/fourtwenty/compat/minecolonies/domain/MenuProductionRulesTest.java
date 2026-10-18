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
        assertEquals(Map.of("minecraft:paper", 32, "fourtwenty:weed_bud", 16 + 4), ProductionRules.ingredientsKept(1));
        var five = ProductionRules.ingredientsKept(5);
        assertEquals(16 + 8 + 2 + 4 + 16, five.get("fourtwenty:weed_bud"), "batches plus the raw bud reserve");
        assertEquals(8, five.get("minecraft:cookie"));
        assertEquals(2, five.get("minecraft:charcoal"));
        assertEquals(16, five.get("minecraft:cocoa_beans"));
    }
    @Test void productionGoesToTheLeastStockedItemInSmallBatches() {
        // Level 4 targets: joint 96, cookie 32, pipe 8, brownie 16. Plenty of buds above the reserve (12).
        var held = Map.of(MenuItem.JOINT, 48, MenuItem.COOKIE, 8, MenuItem.PIPE, 4, MenuItem.BROWNIE, 2);
        var step = ProductionRules.next(4, held, 40, false, false);
        assertEquals(MenuItem.BROWNIE, step.item(), "2/16 brownies is the lowest fill");
        assertEquals(4, step.count());
        assertEquals(0, step.budsToRequest());
        var noBrownieNeed = Map.of(MenuItem.JOINT, 48, MenuItem.COOKIE, 8, MenuItem.PIPE, 4, MenuItem.BROWNIE, 16);
        assertEquals(MenuItem.COOKIE, ProductionRules.next(4, noBrownieNeed, 40, false, false).item());
    }
    @Test void theBudReserveIsNeverCraftedAndIsToppedUp() {
        var empty = Map.<MenuItem, Integer>of();
        var onlyReserve = ProductionRules.next(4, empty, 12, false, false);
        assertNull(onlyReserve.item(), "only the reserve left: wait for buds");
        assertEquals(8, onlyReserve.budsToRequest(), "ask for the next batch (8 joints)");
        var some = ProductionRules.next(4, empty, 15, false, false);
        assertEquals(3, some.count(), "craft only the 3 buds above the reserve");
        var none = ProductionRules.next(4, empty, 0, false, false);
        assertEquals(12 + 8, none.budsToRequest());
        assertEquals(0, ProductionRules.next(4, empty, 0, false, true).budsToRequest(), "a bud request is already open");
    }
    @Test void oneBatchAtATimeButTheReserveIsStillKept() {
        var empty = Map.<MenuItem, Integer>of();
        var busy = ProductionRules.next(5, empty, 3, true, false);
        assertNull(busy.item());
        assertEquals(16 - 3, busy.budsToRequest(), "while crafting, keep requesting the reserve");
        var full = Map.of(MenuItem.JOINT, 128, MenuItem.COOKIE, 32, MenuItem.PIPE, 12, MenuItem.BROWNIE, 32);
        var done = ProductionRules.next(5, full, 16, false, false);
        assertNull(done.item());
        assertEquals(0, done.budsToRequest());
    }
    @Test void everyRecipeUsesOneBud() {
        for (var item : MenuItem.values()) {
            assertEquals(1, ProductionRules.recipe(item).stream().filter(i -> i.itemId().equals("fourtwenty:weed_bud")).mapToInt(ProductionRules.Input::count).sum());
        }
    }
}
