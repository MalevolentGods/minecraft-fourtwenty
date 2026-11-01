package com.malevolentgods.fourtwenty.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import static com.malevolentgods.fourtwenty.domain.Strain.*;

/**
 * What two strains make. In a Pheno Chamber, listed parents of a Cross make it, and any other pair makes one of the two with mixed traits. Anything above a
 * Cross is made by pollinating instead: see {@link Route}.
 */
public final class Crosses {
    private Crosses() {}

    public record Cross(Strain a, Strain b, Strain child) {}

    /** How a hybrid gets made: in a Pheno Chamber; by pollinating, natural or a Buzz Bot's; or only by a Buzz Bot. */
    public enum Route { CHAMBER, POLLEN, BOT }

    public static final List<Cross> CROSSES = List.of(
            new Cross(TANGIE_TANGO, MERMAID_HOTBOX, TROPICAL_THUNDER),
            new Cross(CHERRY_PIE, COTTON_CANDY_KUSH, RAZZLE_DAZZLE),
            new Cross(WAKE_AND_BAKE, SUNSHINE_DAYDREAM, ALARM_CLOCK_ASSASSIN),
            new Cross(COUCHLOCK_HUM, BROWNIE_BATCH, COUCH_FORT),
            new Cross(MOSSY_PHILOSOPHER, STICKY_ICKY, MOSS_BOSS),
            new Cross(THIRD_EYE_TONIC, SPACEBAR_STATIC, DEEP_THOUGHTS),
            new Cross(BLUE_DREAM, CLOUD_NINE_DRIFT, LUCID_LAGOON),
            new Cross(LIME_IN_THE_COCONUT, PURPLE_HAZE, ROCKET_LIME),
            new Cross(TROPICAL_THUNDER, BOG_BUTTER, HURRICANE_HAZE),
            new Cross(RAZZLE_DAZZLE, COUCH_FORT, SLEDGEHAMMER_SERENADE),
            new Cross(ALARM_CLOCK_ASSASSIN, ROCKET_LIME, CAFFEINE_COMET),
            new Cross(MOSS_BOSS, CANOPY_CREEPER, OLD_GROWTH),
            new Cross(DEEP_THOUGHTS, DUST_DEVIL, ORACLE_OF_SNACKS),
            new Cross(LUCID_LAGOON, SNOWBLIND, POLAR_VORTEX),
            new Cross(SLEDGEHAMMER_SERENADE, POLAR_VORTEX, COSMIC_COUCH),
            new Cross(HURRICANE_HAZE, CAFFEINE_COMET, STORM_SHEPHERD),
            new Cross(OLD_GROWTH, ORACLE_OF_SNACKS, THE_ELDER_BUD));

    /** Ticks a cross takes, by the grade it makes. */
    private static final List<Integer> TICKS = List.of(1200, 1200, 6000, 18000, 36000, 72000);

    /** The hybrid these parents make, in either order. Any two different Ultras make Golden God. */
    public static Optional<Strain> hybrid(Strain a, Strain b) {
        if (a != b && a.family() == Family.ULTRA && b.family() == Family.ULTRA) return Optional.of(GOLDEN_GOD);
        return CROSSES.stream().filter(c -> c.a == a && c.b == b || c.a == b && c.b == a).map(Cross::child).findFirst();
    }

    public static Route route(Strain hybrid) {
        return hybrid.grade() <= 2 ? Route.CHAMBER : hybrid.grade() == 3 ? Route.POLLEN : Route.BOT;
    }

    /** Whether these parents make a hybrid that a Pheno Chamber can't. */
    public static boolean needsPollinating(Strain a, Strain b) {
        return hybrid(a, b).filter(strain -> route(strain) != Route.CHAMBER).isPresent();
    }

    /** The child of a cross; {@code roll} is uniform in 0..1 and picks a parent when there's no hybrid. */
    public static Strain child(Strain a, Strain b, DoubleSupplier roll) {
        return hybrid(a, b).orElseGet(() -> roll.getAsDouble() < 0.5 ? a : b);
    }

    /** The grade a cross works at: its hybrid's, or the higher parent's. */
    public static int grade(Strain a, Strain b) {
        return hybrid(a, b).map(Strain::grade).orElse(Math.max(a.grade(), b.grade()));
    }

    /** The lowest power tier of Pheno Chamber that can run a cross of this grade. */
    public static BIRules.Tier chamber(int grade) {
        return grade <= 2 ? BIRules.Tier.TOKE : grade == 3 ? BIRules.Tier.RIP : BIRules.Tier.BLAZE;
    }

    public static int ticks(int grade) { return TICKS.get(grade); }

    /** The strains that make this one. */
    public static List<Strain> parents(Strain strain) {
        List<Strain> out = new ArrayList<>();
        if (strain == GOLDEN_GOD) {
            for (Strain s : values()) if (s.family() == Family.ULTRA) out.add(s);
            return out;
        }
        for (Cross c : CROSSES) {
            if (c.child == strain) {
                out.add(c.a);
                out.add(c.b);
            }
        }
        return out;
    }
}
