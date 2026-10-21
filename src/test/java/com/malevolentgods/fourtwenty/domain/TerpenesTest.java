package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.Terpenes.Base;
import com.malevolentgods.fourtwenty.domain.Terpenes.Code;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TerpenesTest {
    @Test
    void fourBasesSpellSixteenAccordsAndSixtyFourFinals() {
        assertEquals(4, Terpenes.bases().size());
        assertEquals(16, Terpenes.accords().size());
        assertEquals(64, Terpenes.finals().size());
        assertEquals(84, new HashSet<>(Terpenes.all()).size(), "Every terpene is distinct");
    }

    @Test
    void codesRoundTripAndIndexMatchesCodexOrder() {
        for (var tier : java.util.List.of(Terpenes.bases(), Terpenes.accords(), Terpenes.finals())) {
            for (int i = 0; i < tier.size(); i++) {
                Code code = tier.get(i);
                assertEquals(Optional.of(code), Code.parse(code.id()));
                assertEquals(i, code.index(), code.id());
            }
        }
        assertEquals(Optional.empty(), Code.parse("myr_nope"));
        assertEquals(Optional.empty(), Code.parse("myr_myr_myr_myr"));
    }

    @Test
    void portThreeCountsInToneOrderEvenWhenLettersRepeat() {
        // Codex columns are pale (lim), bright (pin), dusky (lav), deep (myr).
        assertEquals(3, Code.of(Base.MYR, Base.MYR, Base.MYR).index());
        assertEquals(0, Code.of(Base.MYR, Base.MYR, Base.LIM).index());
        assertEquals("myr_pin_pin", Terpenes.finals().get(9).id(), "Touch of Grey: neutrals, true grey, bright");
    }

    @Test
    void blendingFollowsThePortRules() {
        Code myr = Code.of(Base.MYR), pin = Code.of(Base.PIN), lim = Code.of(Base.LIM);
        assertEquals(Optional.of(Code.of(Base.MYR, Base.PIN)), Terpenes.result(myr, pin, null), "Two bases make an accord");
        assertEquals(Optional.of(Code.of(Base.PIN, Base.MYR)), Terpenes.result(pin, myr, null), "Order matters");
        Code accord = Code.of(Base.MYR, Base.PIN);
        assertEquals(Optional.of(Code.of(Base.MYR, Base.PIN, Base.LIM)), Terpenes.result(accord, null, lim), "Accord + base finishes");
        assertEquals(Optional.empty(), Terpenes.result(myr, pin, lim), "Three bases skip the accord: not allowed");
        assertEquals(Optional.empty(), Terpenes.result(accord, pin, lim), "An accord fills ports 1 and 2");
        assertEquals(Optional.empty(), Terpenes.result(null, pin, lim));
        assertEquals(Optional.empty(), Terpenes.result(Code.of(Base.MYR, Base.PIN, Base.LIM), null, lim), "Finals don't blend further");
    }

    @Test
    void unitsAreConserved() {
        for (Code accord : Terpenes.accords()) {
            assertEquals(2, accord.units());
            for (Base b : Base.values()) assertEquals(accord.units() + 1, Terpenes.finish(accord, b).units());
        }
        assertEquals(Code.of(Base.LAV, Base.LIM), Code.of(Base.LAV, Base.LIM, Base.MYR).accord());
    }
}
