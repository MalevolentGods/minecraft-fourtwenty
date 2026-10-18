package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static com.malevolentgods.fourtwenty.domain.BongVariety.*;
import static org.junit.jupiter.api.Assertions.*;

class BongVarietyTest {
    @Test void colonistsPickTheBongForTheirPrimarySkillThenSecondary() {
        var lounge = List.of(GLASS, JACKRABBIT, FORGE, SAGE);
        assertEquals(Optional.of(FORGE), choose(lounge, "Strength", "Focus"), "a miner picks the forge bong");
        assertEquals(Optional.of(SAGE), choose(lounge, "Dexterity", "Intelligence"), "secondary skill when the primary has no bong");
        assertEquals(Optional.of(JACKRABBIT), choose(lounge, "Creativity", "Mana"), "no match: first specialty in menu order");
        assertEquals(Optional.of(JACKRABBIT), choose(lounge, null, null), "unemployed: first specialty");
        assertEquals(Optional.empty(), choose(List.of(GLASS), "Strength", "Focus"), "a plain glass bong has no buff");
        assertEquals(Optional.empty(), choose(List.of(), "Strength", "Focus"));
    }
    @Test void matchingTheJobDoublesTheSkillBonus() {
        assertEquals(10, FORGE.skillBonus("Strength"));
        assertEquals(10, FORGE.skillBonus("Stamina"));
        assertEquals(5, FORGE.skillBonus("Focus"));
        assertEquals(5, FORGE.skillBonus(null));
        assertEquals(0, GLASS.skillBonus("Strength"));
    }
    @Test void everySpecialtyHasEffectsTwoSkillsAndAnUpgradeRecipe() {
        assertEquals(6, specialties().size());
        for (var variety : specialties()) {
            assertEquals(2, variety.skills.size(), variety.name());
            assertFalse(variety.ingredients.isEmpty(), variety.name());
            assertFalse(variety.effects.isEmpty(), variety.name());
            assertTrue(variety.path.endsWith("_bong"), variety.name());
        }
        assertTrue(PHANTOM.flight);
        assertEquals(1, specialties().stream().filter(v -> v.flight).count());
        assertEquals(Optional.of(OWL), byPath("owl_bong"));
        assertEquals(24_000, BUFF_TICKS);
    }
    @Test void skillNamesMatchMineColonies() {
        var known = List.of("Athletics", "Dexterity", "Strength", "Agility", "Stamina", "Mana", "Adaptability", "Focus", "Creativity", "Knowledge", "Intelligence");
        for (var variety : values()) assertTrue(known.containsAll(variety.skills), variety.name());
    }
}
