package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossesTest {
    @Test
    void orderDoesNotMatter() {
        for (var c : Crosses.CROSSES) {
            assertEquals(c.child(), Crosses.hybrid(c.a(), c.b()).orElseThrow());
            assertEquals(c.child(), Crosses.hybrid(c.b(), c.a()).orElseThrow());
        }
    }

    @Test
    void anyTwoUltrasMakeTheGod() {
        assertEquals(Strain.GOLDEN_GOD, Crosses.hybrid(Strain.COSMIC_COUCH, Strain.THE_ELDER_BUD).orElseThrow());
        assertEquals(Strain.GOLDEN_GOD, Crosses.hybrid(Strain.STORM_SHEPHERD, Strain.COSMIC_COUCH).orElseThrow());
        assertFalse(Crosses.hybrid(Strain.COSMIC_COUCH, Strain.COSMIC_COUCH).isPresent());
    }

    @Test
    void anUnlistedPairMakesAParent() {
        assertEquals(Strain.BLUE_DREAM, Crosses.child(Strain.BLUE_DREAM, Strain.CHERRY_PIE, () -> 0.1));
        assertEquals(Strain.CHERRY_PIE, Crosses.child(Strain.BLUE_DREAM, Strain.CHERRY_PIE, () -> 0.9));
    }

    @Test
    void eachHybridSitsAboveItsParents() {
        for (var c : Crosses.CROSSES) {
            assertTrue(c.child().grade() > c.a().grade() && c.child().grade() > c.b().grade(), c.toString());
        }
    }

    @Test
    void everyStrainCanBeReached() {
        Set<Strain> have = EnumSet.noneOf(Strain.class);
        for (Strain s : Strain.values()) if (s.grade() <= 1) have.add(s);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Strain a : Strain.values()) {
                for (Strain b : Strain.values()) {
                    if (have.contains(a) && have.contains(b)) grew |= Crosses.hybrid(a, b).map(have::add).orElse(false);
                }
            }
        }
        assertEquals(EnumSet.allOf(Strain.class), have);
    }

    @Test
    void onlyCrossesComeFromTheChamber() {
        assertEquals(Crosses.Route.CHAMBER, Crosses.route(Strain.MOSS_BOSS));
        assertEquals(Crosses.Route.POLLEN, Crosses.route(Strain.OLD_GROWTH));
        assertEquals(Crosses.Route.BOT, Crosses.route(Strain.THE_ELDER_BUD));
        assertEquals(Crosses.Route.BOT, Crosses.route(Strain.GOLDEN_GOD));
        assertFalse(Crosses.needsPollinating(Strain.WAKE_AND_BAKE, Strain.SUNSHINE_DAYDREAM));
        assertTrue(Crosses.needsPollinating(Strain.MOSS_BOSS, Strain.CANOPY_CREEPER));
        assertFalse(Crosses.needsPollinating(Strain.OLD_GROWTH, Strain.OLD_GROWTH));
    }

    @Test
    void betterStrainsNeedBetterChambers() {
        assertEquals(BIRules.Tier.TOKE, Crosses.chamber(Strain.MOSS_BOSS.grade()));
        assertEquals(BIRules.Tier.RIP, Crosses.chamber(Strain.OLD_GROWTH.grade()));
        assertEquals(BIRules.Tier.BLAZE, Crosses.chamber(Strain.THE_ELDER_BUD.grade()));
        assertEquals(BIRules.Tier.BLAZE, Crosses.chamber(Strain.GOLDEN_GOD.grade()));
        assertEquals(1, Crosses.grade(Strain.BLUE_DREAM, Strain.CHERRY_PIE));
        assertEquals(4, Crosses.grade(Strain.COSMIC_COUCH, Strain.COSMIC_COUCH));
        assertEquals(5, Crosses.grade(Strain.COSMIC_COUCH, Strain.STORM_SHEPHERD));
    }
}
