package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import java.util.List;

/** What a citizen consumes. Until affinity is rolled, everyone is served as {@link #BOTH}. */
public enum Preference {
    SMOKE(List.of(MenuItem.JOINT), List.of(MenuItem.PIPE)),
    EDIBLE(List.of(MenuItem.BROWNIE, MenuItem.COOKIE), List.of(MenuItem.BROWNIE, MenuItem.COOKIE)),
    /** Takes whatever is in stock, preferring the higher-level menu item. */
    BOTH(List.of(MenuItem.BROWNIE, MenuItem.COOKIE, MenuItem.JOINT), List.of(MenuItem.BROWNIE, MenuItem.PIPE, MenuItem.COOKIE)),
    NONE(List.of(), List.of());

    /** Items consumed at the shop, best first. */
    public final List<MenuItem> onSpot;
    /** Items carried away for later doses, best first. */
    public final List<MenuItem> takeAway;

    Preference(List<MenuItem> onSpot, List<MenuItem> takeAway) {
        this.onSpot = onSpot;
        this.takeAway = takeAway;
    }
}
