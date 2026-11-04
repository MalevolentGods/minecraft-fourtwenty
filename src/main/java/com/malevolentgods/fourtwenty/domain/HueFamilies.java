package com.malevolentgods.fourtwenty.domain;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which hue family a terpene colour belongs to, for the Kush Suit's terpene slots. A final terpene's hue is its first two letters
 * (its row in the palette); the Minecraft colours join the family they look like. Greys, creams and browns belong to none.
 */
public final class HueFamilies {
    private HueFamilies() {}

    public enum Family { REDS, YELLOWS, GREENS, BLUES, PURPLES }

    private static final Map<String, Family> ROWS = Map.ofEntries(
            Map.entry("lim_pin", Family.REDS), Map.entry("lim_lav", Family.REDS),
            Map.entry("lim_lim", Family.YELLOWS),
            Map.entry("pin_lim", Family.GREENS), Map.entry("pin_pin", Family.GREENS), Map.entry("pin_myr", Family.GREENS),
            Map.entry("lav_pin", Family.BLUES), Map.entry("lav_lav", Family.BLUES), Map.entry("pin_lav", Family.BLUES),
            Map.entry("lav_myr", Family.PURPLES), Map.entry("lav_lim", Family.PURPLES));

    private static final Map<String, Family> MINECRAFT = Map.ofEntries(
            Map.entry("red", Family.REDS), Map.entry("orange", Family.REDS),
            Map.entry("yellow", Family.YELLOWS),
            Map.entry("green", Family.GREENS), Map.entry("lime", Family.GREENS),
            Map.entry("blue", Family.BLUES), Map.entry("light_blue", Family.BLUES), Map.entry("cyan", Family.BLUES),
            Map.entry("purple", Family.PURPLES), Map.entry("magenta", Family.PURPLES), Map.entry("pink", Family.PURPLES));

    /** The 16 Minecraft dye names, so the tests can see every one is placed or left out on purpose. */
    public static final Set<String> MINECRAFT_COLOURS = Set.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");

    /** {@code code} is a final terpene's code ({@code lim_pin_lav}) or a Minecraft one ({@code mc_red}). */
    public static Optional<Family> of(String code) {
        if (code == null) return Optional.empty();
        if (code.startsWith("mc_")) return Optional.ofNullable(MINECRAFT.get(code.substring(3)));
        String[] letters = code.split("_");
        if (letters.length != 3) return Optional.empty();   // only final terpenes go in gear
        return Optional.ofNullable(ROWS.get(letters[0] + "_" + letters[1]));
    }
}
