package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.HueFamilies.Family;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HueFamiliesTest {
    private static final String[] LETTERS = {"myr", "lim", "pin", "lav"};

    @Test
    void eachFamilyHasAboutSixteenColours() {
        Map<Family, Integer> counts = new EnumMap<>(Family.class);
        for (String a : LETTERS) for (String b : LETTERS) for (String c : LETTERS)
            HueFamilies.of(a + "_" + b + "_" + c).ifPresent(f -> counts.merge(f, 1, Integer::sum));
        for (String dye : HueFamilies.MINECRAFT_COLOURS) HueFamilies.of("mc_" + dye).ifPresent(f -> counts.merge(f, 1, Integer::sum));
        for (Family f : Family.values()) assertTrue(counts.getOrDefault(f, 0) >= 5 && counts.get(f) <= 16, f + ": " + counts.get(f));
    }

    @Test
    void theRowsAreTheirHues() {
        assertEquals(Optional.of(Family.REDS), HueFamilies.of("lim_pin_pin"));
        assertEquals(Optional.of(Family.YELLOWS), HueFamilies.of("lim_lim_lim"));
        assertEquals(Optional.of(Family.GREENS), HueFamilies.of("pin_pin_lim"));
        assertEquals(Optional.of(Family.BLUES), HueFamilies.of("lav_pin_lav"));
        assertEquals(Optional.of(Family.PURPLES), HueFamilies.of("lav_myr_pin"));
        assertEquals(Optional.of(Family.BLUES), HueFamilies.of("mc_cyan"));
    }

    @Test
    void greysBrownsAndBasesBelongToNone() {
        assertTrue(HueFamilies.of("myr_myr_myr").isEmpty());
        assertTrue(HueFamilies.of("lim_myr_pin").isEmpty(), "browns");
        assertTrue(HueFamilies.of("mc_black").isEmpty());
        assertTrue(HueFamilies.of("lim_pin").isEmpty(), "an accord isn't a final terpene");
        assertTrue(HueFamilies.of(null).isEmpty());
    }
}
