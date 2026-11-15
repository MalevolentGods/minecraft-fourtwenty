package com.malevolentgods.fourtwenty.domain;

import java.util.Set;

/**
 * The Midnight Machine's dance moves (specs/014-midnight-machine T033): original moves made of ordinary dance
 * vocabulary (no copied game emotes), each a pose of her body at a moment of the beat, and the flourishes, short showy
 * moves from rest back to rest. Pure; angles in degrees.
 * <p>
 * Conventions: an arm's {@code pitch} raises it forward and up (180 is straight up), {@code roll} takes it out to her
 * side, {@code yaw} turns it in; the head's {@code pitch} nods down, {@code yaw} turns to her left, {@code roll} tilts;
 * a leg's angle kicks it forward; {@code lean} tips her back, {@code tilt} to her left; {@code bob} lifts her (blocks);
 * {@code turn} turns her (counterclockwise from above).
 */
public final class DanceMoves {
    private DanceMoves() {}

    public record Limb(double pitch, double yaw, double roll) {
        public static final Limb REST = new Limb(0, 0, 0);

        Limb plus(Limb o) { return new Limb(pitch + o.pitch, yaw + o.yaw, roll + o.roll); }
        Limb scale(double k) { return new Limb(pitch * k, yaw * k, roll * k); }
    }

    public record Body(Limb leftArm, Limb rightArm, Limb head, double leftLeg, double rightLeg,
                       double lean, double tilt, double bob, double turn) {
        public static final Body REST = new Body(Limb.REST, Limb.REST, Limb.REST, 0, 0, 0, 0, 0, 0);

        public Body plus(Body o) {
            return new Body(leftArm.plus(o.leftArm), rightArm.plus(o.rightArm), head.plus(o.head), leftLeg + o.leftLeg,
                    rightLeg + o.rightLeg, lean + o.lean, tilt + o.tilt, bob + o.bob, turn + o.turn);
        }

        public Body scale(double k) {
            return new Body(leftArm.scale(k), rightArm.scale(k), head.scale(k), leftLeg * k, rightLeg * k,
                    lean * k, tilt * k, bob * k, turn * k);
        }

        /** Arms only, scaled (smaller while she's on the move). */
        public Body armsScaled(double k) {
            return new Body(leftArm.scale(k), rightArm.scale(k), head, leftLeg, rightLeg, lean, tilt, bob, turn);
        }

        public static Body lerp(Body a, Body b, double t) {
            return a.scale(1 - t).plus(b.scale(t));
        }
    }

    public static final Set<String> MOVES = Set.of("groove", "step_touch", "shoulder_shimmy", "lean_pump", "windmill",
            "wave_overhead", "air_synth", "conduct", "robot_pop", "float", "cupped_ear", "horizon", "reach_climb", "mirror");
    public static final Set<String> FLOURISHES = Set.of("big_spin_point", "dip_and_rise", "double_pop", "slow_turn_open",
            "ear_cup_freeze", "jump_kick");

    // beat helpers: b is the fractional count of her dance beats
    static double frac(double x) { return x - Math.floor(x); }
    /** 1 on the beat, easing to 0 between. */
    static double pulse(double b) { double up = (Math.cos(2 * Math.PI * frac(b)) + 1) / 2; return up * up; }
    /** One way then the other, two beats a swing. */
    static double side(double b) { return Math.sin(Math.PI * b); }
    /** A slow wave, {@code beats} long. */
    static double wave(double b, double beats) { return Math.sin(2 * Math.PI * b / beats); }
    static double ease(double s) { s = Math.max(0, Math.min(1, s)); return s * s * (3 - 2 * s); }
    /** 0 at both ends, 1 in the middle. */
    static double bell(double s) { return Math.sin(Math.PI * Math.max(0, Math.min(1, s))); }
    static double wrap(double deg) { deg = deg % 360; return deg > 180 ? deg - 360 : deg <= -180 ? deg + 360 : deg; }

    private static Limb arm(double pitch, double roll) { return new Limb(pitch, 0, roll); }
    private static Body body(Limb l, Limb r, Limb head) { return new Body(l, r, head, 0, 0, 0, 0, 0, 0); }

    /**
     * A move at beat {@code b}, {@code progress} (0..1) through its section. Unknown names dance the groove, so a
     * mistyped routine still dances.
     */
    public static Body move(String name, double b, double progress) {
        double p = pulse(b), s = side(b);
        return switch (name) {
            case "step_touch" -> new Body(arm(25 * s, 8), arm(-25 * s, 8), new Limb(6 * p, 0, 5 * s),
                    12 * Math.max(0, s), 12 * Math.max(0, -s), 0, 4 * s, 0, 0);
            case "shoulder_shimmy" -> {
                double q = Math.sin(4 * Math.PI * b);
                yield new Body(arm(35, 12 + 8 * q), arm(35, 12 - 8 * q), new Limb(4 * p, 6 * s, 0), 0, 0, 0, 3 * q, 0, 0);
            }
            case "lean_pump" -> new Body(arm(150 + 25 * p, 15), arm(150 + 25 * p, 15), new Limb(-15, 0, 0),
                    0, 0, 10 + 4 * p, 0, 0, 0);
            case "windmill" -> new Body(new Limb(-10, 20, 35), arm(wrap(360 * frac(b / 2)), 10), new Limb(5 * p, 0, 0),
                    0, 0, 0, 0, 0, 10 * s);
            case "wave_overhead" -> {
                double w = wave(b, 4);
                yield new Body(arm(160, 20 + 25 * w), arm(160, 20 - 25 * w), new Limb(-10, 8 * w, 0), 0, 0, 0, 5 * w, 0, 6 * w);
            }
            case "air_synth" -> new Body(new Limb(70 + 10 * p, 0, -10), new Limb(70 + 10 * pulse(b + 0.5), 0, -10),
                    new Limb(12 * p, 0, 0), 0, 0, -5, 0, 0, 0);
            case "conduct" -> {
                double[] pattern = {0, 25, -15, 10};
                int k = (int) Math.floor(b) & 3;
                double roll = pattern[k] + (pattern[(k + 1) & 3] - pattern[k]) * ease(frac(b));
                yield body(arm(20, 10), arm(90 - 30 * p, roll), new Limb(6 * p, 8 * s, 0));
            }
            case "robot_pop" -> {
                Body[] poses = {
                        body(arm(90, 0), arm(0, 0), new Limb(0, 20, 0)),
                        body(arm(0, 0), arm(90, 0), new Limb(0, -20, 0)),
                        body(arm(0, 90), arm(0, 90), Limb.REST),
                        body(arm(150, 0), arm(-30, 0), new Limb(10, 0, 0)),
                };
                int k = (int) Math.floor(b) & 3;
                double snap = Math.min(1, frac(b) / 0.12);   // a sharp pop into each pose, then hold
                yield Body.lerp(poses[(k + 3) & 3], poses[k], snap);
            }
            case "float" -> new Body(arm(10 * wave(b, 8), 75 + 8 * wave(b, 4)), arm(10 * wave(b, 8), 75 - 8 * wave(b, 4)),
                    new Limb(-8, 0, 0), 0, 0, 5, 0, 0.05 * wave(b, 8), 20 * wave(b, 16));
            case "cupped_ear" -> new Body(arm(15 + 15 * p, 8), new Limb(140, 0, -50), new Limb(8 * p, -10, 15),
                    0, 0, 0, 3, 0, 0);
            case "horizon" -> new Body(arm(0, 15), new Limb(165, 0, -30), new Limb(-5, 25 * wave(b, 8), 0),
                    0, 0, 3, 0, 0, 0);
            case "reach_climb" -> {
                double hi = 100 + 50 * progress;
                yield new Body(arm(hi + 30 * s, 10), arm(hi - 30 * s, 10), new Limb(-10 * progress, 0, 0),
                        0, 0, 0, 0, 0.05 * progress * p, 0);
            }
            case "mirror" -> {
                double r = 40 + 30 * Math.sin(Math.PI * b / 2), up = 30 + 20 * Math.cos(Math.PI * b / 2);
                yield body(arm(up, r), arm(up, r), new Limb(-5, 0, 0));
            }
            default -> body(arm(15 + 20 * p, 10), arm(15 + 20 * p, 10), new Limb(10 * p, 0, 0));   // groove
        };
    }

    /** How long a flourish lasts. */
    public static long flourishMs(String name) {
        return switch (name) {
            case "big_spin_point", "ear_cup_freeze" -> 1200;
            case "dip_and_rise" -> 1000;
            case "double_pop" -> 900;
            case "slow_turn_open" -> 1600;
            case "jump_kick" -> 800;
            default -> 1000;
        };
    }

    /** A flourish {@code s} (0..1) of the way through: rest at both ends, added on top of her move. */
    public static Body flourish(String name, double s) {
        double b = bell(s);
        return switch (name) {
            case "big_spin_point" -> new Body(arm(0, 30 * b), arm(170 * bell(Math.max(0, s * 1.6 - 0.6)), 0),
                    new Limb(-10 * b, 0, 0), 0, 0, 0, 0, 0.08 * b, 360 * ease(s));
            case "dip_and_rise" -> new Body(arm(170 * b, 20 * b), arm(170 * b, 20 * b), new Limb(-15 * b, 0, 0),
                    0, 0, 0, 0, -0.2 * Math.sin(2 * Math.PI * s), 0);
            case "double_pop" -> {
                double one = Math.max(0, 1 - Math.abs(s - 0.25) / 0.2), two = Math.max(0, 1 - Math.abs(s - 0.7) / 0.2);
                yield body(arm(90 * one, 0), arm(90 * two, 0), new Limb(0, 30 * one - 30 * two, 0));
            }
            case "slow_turn_open" -> new Body(arm(0, 85 * b), arm(0, 85 * b), new Limb(-15 * b, 0, 0),
                    0, 0, 4 * b, 0, 0, 360 * ease(s));
            case "ear_cup_freeze" -> new Body(Limb.REST, new Limb(140 * b, 0, -50 * b), new Limb(0, -15 * b, 20 * b),
                    0, 0, 5 * b, 0, 0, 0);
            case "jump_kick" -> new Body(arm(120 * b, 10 * b), arm(120 * b, 10 * b), new Limb(-10 * b, 0, 0),
                    0, 60 * b, 0, 0, 0.3 * b, 0);
            default -> Body.REST;
        };
    }
}
