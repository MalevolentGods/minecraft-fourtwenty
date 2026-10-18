package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MenuItemTest {
    @Test void menuGrowsWithDispensaryLevel() {
        assertEquals(List.of(), MenuItem.menu(0));
        assertEquals(List.of(MenuItem.JOINT), MenuItem.menu(1));
        assertEquals(List.of(MenuItem.JOINT, MenuItem.COOKIE), MenuItem.menu(2));
        assertEquals(List.of(MenuItem.JOINT, MenuItem.COOKIE, MenuItem.PIPE), MenuItem.menu(3));
        assertEquals(List.of(MenuItem.JOINT, MenuItem.COOKIE, MenuItem.PIPE, MenuItem.BROWNIE), MenuItem.menu(4));
        assertEquals(MenuItem.menu(4), MenuItem.menu(5));
    }
    @Test void colonyMembersAreServedFree() {
        assertEquals(0, MenuItem.price(true, 3));
        assertEquals(3, MenuItem.price(false, 3));
        assertEquals(0, MenuItem.price(false, -1));
    }
    @Test void defaultPricesMakeThePipeCheaperPerUseAndTheBrowniePremium() {
        assertEquals(1, MenuItem.JOINT.defaultPrice);
        assertEquals(1, MenuItem.COOKIE.defaultPrice);
        assertEquals(2, MenuItem.BROWNIE.defaultPrice);
        assertEquals(3, MenuItem.PIPE.defaultPrice);
        assertTrue(MenuItem.COOKIE.edible && MenuItem.BROWNIE.edible && !MenuItem.PIPE.edible && !MenuItem.JOINT.edible);
    }
}
