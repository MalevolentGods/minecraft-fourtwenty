package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.GearRules.Piece;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GearAbilitiesTest {
    @Test
    void everyColourIsInItsPiecesFamily() {
        for (var a : GearAbilities.ALL) assertTrue(GearAbilities.fits(a.piece(), a.colour()), a.toString());
    }

    @Test
    void noColourUnlocksTwoThingsOnOnePiece() {
        var seen = new HashSet<String>();
        for (var a : GearAbilities.ALL) assertTrue(seen.add(a.piece() + "/" + a.colour()), a.toString());
    }

    @Test
    void theStartingSetHasAtLeastTwoPerArmourPieceAndTheBlazerModes() {
        for (Piece p : new Piece[] {Piece.HOTBOX_HELM, Piece.PUFF_PLATE, Piece.COUCHLOCK_GREAVES, Piece.MOON_BOOTS})
            assertTrue(GearAbilities.ALL.stream().filter(a -> a.piece() == p).count() >= 2, p.name());
        for (String mode : new String[] {"bore_toke", "timber_torch", "paint_puff"})
            assertTrue(GearAbilities.byId(Piece.BLAZER, mode).isPresent(), mode);
    }

    @Test
    void wrongFamiliesDontFit() {
        assertFalse(GearAbilities.fits(Piece.MOON_BOOTS, "lim_pin_pin"), "a red in the boots");
        assertFalse(GearAbilities.fits(Piece.DAB_CELL, "lav_lav_lav"), "the Dab Cell takes no terpenes");
    }
}
