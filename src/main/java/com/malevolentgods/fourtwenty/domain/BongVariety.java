package com.malevolentgods.fourtwenty.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Glass bong varieties. The plain glass bong gives a pipe's hit; each specialty bong (an upgraded
 * glass bong) adds a one-day buff: potion effects for players and two boosted skills for colonists.
 *
 * <p>Pure registry-id/skill-name data so the rules can be unit tested without a game. Skill names
 * are MineColonies' {@code Skill} constants.
 */
public enum BongVariety {
    GLASS("glass_bong", List.of(), List.of(), List.of(), false),
    JACKRABBIT("jackrabbit_bong", List.of(effect("minecraft:speed", 1), effect("minecraft:jump_boost", 1)),
            List.of("Athletics", "Agility"), List.of("minecraft:rabbit_foot", "minecraft:sugar"), false),
    DEEP_SEA("deep_sea_bong", List.of(effect("minecraft:water_breathing", 0), effect("minecraft:dolphins_grace", 0), effect("minecraft:conduit_power", 0)),
            List.of("Stamina", "Adaptability"), List.of("minecraft:prismarine_crystals", "minecraft:nautilus_shell"), false),
    FORGE("forge_bong", List.of(effect("minecraft:fire_resistance", 0), effect("minecraft:strength", 0)),
            List.of("Strength", "Stamina"), List.of("minecraft:blaze_rod", "minecraft:magma_cream"), false),
    OWL("owl_bong", List.of(effect("minecraft:night_vision", 0), effect("minecraft:haste", 1)),
            List.of("Focus", "Dexterity"), List.of("minecraft:golden_carrot", "minecraft:spyglass"), false),
    SAGE("sage_bong", List.of(effect("minecraft:luck", 1), effect("minecraft:hero_of_the_village", 0)),
            List.of("Knowledge", "Intelligence"), List.of("minecraft:amethyst_shard", "minecraft:book"), false),
    PHANTOM("phantom_bong", List.of(effect("minecraft:slow_falling", 0)),
            List.of("Agility", "Mana"), List.of("minecraft:phantom_membrane", "minecraft:feather"), true);

    /** One day of simulation time: how long a specialty buff lasts. */
    public static final int BUFF_TICKS = 24_000;
    /** Skill levels added to each of the bong's two skills, doubled when one is the colonist's primary job skill. */
    public static final int SKILL_BONUS = 5;

    public record Effect(String id, int amplifier) {}
    private static Effect effect(String id, int amplifier) { return new Effect(id, amplifier); }

    /** Block and item id path ({@code fourtwenty:<path>}). */
    public final String path;
    public final List<Effect> effects;
    public final List<String> skills;
    /** Shapeless upgrade recipe: a glass bong plus these. */
    public final List<String> ingredients;
    /** Grants creative-style flight for the buff's duration. */
    public final boolean flight;

    BongVariety(String path, List<Effect> effects, List<String> skills, List<String> ingredients, boolean flight) {
        this.path = path;
        this.effects = effects;
        this.skills = skills;
        this.ingredients = ingredients;
        this.flight = flight;
    }

    public boolean specialty() { return this != GLASS; }

    public static List<BongVariety> specialties() { return Arrays.stream(values()).filter(BongVariety::specialty).toList(); }

    public static Optional<BongVariety> byPath(String path) {
        return Arrays.stream(values()).filter(v -> v.path.equals(path)).findFirst();
    }

    /**
     * The specialty bong a colonist picks from those available: one boosting their job's primary
     * skill, else their secondary skill, else the first available (menu order). Empty if none.
     */
    public static Optional<BongVariety> choose(List<BongVariety> available, String primarySkill, String secondarySkill) {
        var specialty = available.stream().filter(BongVariety::specialty).distinct().sorted().toList();
        for (String skill : new String[]{primarySkill, secondarySkill}) {
            if (skill == null) continue;
            for (var variety : specialty) if (variety.skills.contains(skill)) return Optional.of(variety);
        }
        return specialty.stream().findFirst();
    }

    /** Levels added to each of this bong's skills for a colonist with this primary job skill. */
    public int skillBonus(String primarySkill) {
        if (!specialty()) return 0;
        return primarySkill != null && skills.contains(primarySkill) ? SKILL_BONUS * 2 : SKILL_BONUS;
    }
}
