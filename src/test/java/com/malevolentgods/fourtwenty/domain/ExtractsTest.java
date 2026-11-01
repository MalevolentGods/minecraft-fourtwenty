package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractsTest {
    @Test
    void theChainRunsFromBudToRosin() {
        assertEquals(Extracts.KIEF.id(), Extracts.step("fourtwenty:weed_bud", false).orElseThrow().output());
        assertEquals(2, Extracts.step(Extracts.KIEF.id(), false).orElseThrow().count());
        assertEquals(Extracts.RESIN.id(), Extracts.step(Extracts.HASH.id(), false).orElseThrow().output());
        assertEquals(Extracts.ROSIN.id(), Extracts.step(Extracts.RESIN.id(), false).orElseThrow().output());
        assertFalse(Extracts.step(Extracts.ROSIN.id(), true).isPresent());
    }

    @Test
    void aBottleMakesOil() {
        assertEquals(Extracts.OIL.id(), Extracts.step(Extracts.RESIN.id(), true).orElseThrow().output());
    }

    @Test
    void refiningAlwaysGivesMoreBurnPerBud() {
        assertTrue(Extracts.KIEF.burn() > 1);
        assertTrue(Extracts.HASH.burn() > Extracts.KIEF.burn() * 2);
        assertTrue(Extracts.RESIN.burn() > Extracts.HASH.burn());
        assertTrue(Extracts.ROSIN.burn() > Extracts.RESIN.burn());
    }

    @Test
    void burnLengths() {
        assertEquals(1200, Extracts.burnTicks("fourtwenty:weed_bud", Strain.DUST_DEVIL), "a bud is a minute, whatever the strain");
        assertEquals(6000, Extracts.burnTicks(Extracts.HASH.id(), Strain.BLUE_DREAM));
        assertEquals(9000, Extracts.burnTicks(Extracts.HASH.id(), Strain.DUST_DEVIL), "biome strains burn longer");
        assertEquals(Extracts.LONGEST_BURN, Extracts.burnTicks(Extracts.ROSIN.id(), Strain.SNOWBLIND));
    }

    @Test
    void pipesSmokeStrongerPastKief() {
        Traits plain = new Traits(2, 3, 2);
        assertEquals(plain, Extracts.boosted("fourtwenty:weed_bud", plain));
        assertEquals(plain, Extracts.boosted(Extracts.KIEF.id(), plain));
        assertEquals(new Traits(3, 3, 3), Extracts.boosted(Extracts.HASH.id(), plain));
        assertEquals(new Traits(4, 3, 4), Extracts.boosted(Extracts.OIL.id(), plain));
        assertEquals(new Traits(5, 3, 5), Extracts.boosted(Extracts.OIL.id(), new Traits(5, 3, 4)), "never past 5");
    }
}
