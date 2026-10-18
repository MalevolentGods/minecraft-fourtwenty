package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import java.util.Arrays;
import java.util.List;

/**
 * The Dispensary menu, unlocked by completed Dispensary level. The level is also the item's rank:
 * citizens who like both kinds prefer the higher-level item in stock.
 *
 * <p>Pure registry-id rules so they can be unit tested without a game.
 */
public enum MenuItem {
    JOINT("fourtwenty:weed_joint", 1, 1, false),
    COOKIE("fourtwenty:weed_cookie", 2, 1, true),
    PIPE("fourtwenty:weed_pipe", 3, 3, false),
    BROWNIE("fourtwenty:weed_brownie", 4, 2, true);

    public final String itemId;
    /** Completed Dispensary level that adds this item to the menu; also its preference rank. */
    public final int level;
    /** Default emerald price for players without colony permission (server config can change it). */
    public final int defaultPrice;
    public final boolean edible;

    MenuItem(String itemId, int level, int defaultPrice, boolean edible) {
        this.itemId = itemId;
        this.level = level;
        this.defaultPrice = defaultPrice;
        this.edible = edible;
    }

    /** Everything a Dispensary of this completed level offers, in menu order. */
    public static List<MenuItem> menu(int completedLevel) {
        return Arrays.stream(values()).filter(item -> item.level <= completedLevel).toList();
    }

    /** What a player pays: colony members are served free, as at the restaurant. */
    public static int price(boolean colonyMember, int configuredPrice) {
        return colonyMember ? 0 : Math.max(0, configuredPrice);
    }
}
