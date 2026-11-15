package com.malevolentgods.fourtwenty.domain;

/**
 * A Lifted Builder's dance (specs/014-midnight-machine US3, FR-012 to FR-015): where in it she is at a moment of her
 * track, from the track's beat map. Purely how she's drawn: nothing here moves her, turns her or slows her work.
 * <ul>
 *   <li>a bob that peaks on each beat (every other beat on double-time tracks above {@link #DOUBLE_TIME_BPM});</li>
 *   <li>a sway from side to side over two of those beats;</li>
 *   <li>a full spin on the big hits (accents), 0.4 s long, never two within the cooldown;</li>
 *   <li>arms and a head nod on the beat, bigger while she's still (waiting, placing) than while she's moving.</li>
 * </ul>
 * On top of that, each track's {@link Routine}: the move for the section playing (blended over {@link #BLEND_MS} when
 * the section changes, smaller while she moves) and its flourishes, which take the place of the accent spins while they
 * play. With no beat map (music off, or a track without one) she dances to the fallback tempo, without spins. Pure.
 */
public final class DanceRules {
    private DanceRules() {}

    public static final long SPIN_MS = 400;
    public static final double DOUBLE_TIME_BPM = 140;
    public static final long BLEND_MS = 500;
    /** Her moves while she's flying or walking somewhere, against while she's still. */
    static final double MOVING_ARMS = 0.6;

    public record Tuning(double bob, long spinCooldownMs, double fallbackBpm, double swayDeg,
                         double armMovingDeg, double armWaitingDeg, double headNodDeg) {
        public static Tuning of(double bob, double spinCooldownSeconds, double fallbackBpm) {
            return new Tuning(bob, Math.round(spinCooldownSeconds * 1000), fallbackBpm, 9, 20, 55, 12);
        }
    }

    /**
     * @param bob      how high she is drawn above her real place, in blocks
     * @param spinDeg  extra turn for a spin, 0..360
     * @param swayDeg  extra turn from side to side
     * @param body     arms, head, legs, lean and tilt (plus a little extra bob and turn) from the move and flourish
     * @param envelope 0..1, 1 on the beat: for anything else that pulses with it
     */
    public record Pose(double bob, double spinDeg, double swayDeg, DanceMoves.Body body, double envelope) {}

    public static Pose pose(double bpm, long[] beatsMs, long[] accentsMs, long ms, boolean moving, Tuning t) {
        return pose(bpm, beatsMs, accentsMs, ms, moving, t, Routine.NONE);
    }

    public static Pose pose(double bpm, long[] beatsMs, long[] accentsMs, long ms, boolean moving, Tuning t, Routine routine) {
        boolean mapped = bpm > 0 && beatsMs.length >= 2;
        double tempo = bpm > 0 ? bpm : t.fallbackBpm();
        double beats = mapped ? beatCount(beatsMs, ms, 60_000 / tempo) : ms / (60_000 / tempo);
        double danced = beats / (tempo > DOUBLE_TIME_BPM ? 2 : 1);
        double pulse = DanceMoves.pulse(danced);   // a snap on the beat, easing off between
        double side = DanceMoves.side(danced);     // one way, then the other: two beats a sway

        DanceMoves.Body body;
        int i = routine.sectionAt(ms);
        if (i >= 0) {
            body = section(routine, i, danced, ms);
            if (i > 0 && ms - routine.sections().get(i).startMs() < BLEND_MS) {
                double k = (ms - routine.sections().get(i).startMs()) / (double) BLEND_MS;
                body = DanceMoves.Body.lerp(section(routine, i - 1, danced, ms), body, DanceMoves.ease(k));
            }
            if (moving) body = body.armsScaled(MOVING_ARMS);
        } else {
            double arms = moving ? t.armMovingDeg() : t.armWaitingDeg();
            var arm = new DanceMoves.Limb(arms * pulse, 0, arms * 0.5 * side);
            body = new DanceMoves.Body(arm, arm, new DanceMoves.Limb(t.headNodDeg() * pulse, 0, 0), 0, 0, 0, 0, 0, 0);
        }

        var flourish = routine.flourishAt(ms);
        double spin = 0;
        if (flourish != null) {
            double s = (ms - flourish.ms()) / (double) DanceMoves.flourishMs(flourish.move());
            body = body.plus(DanceMoves.flourish(flourish.move(), s));
        } else if (mapped) {
            spin = spin(accentsMs, ms, t.spinCooldownMs());
        }
        return new Pose(t.bob() * pulse, spin, t.swayDeg() * side, body, pulse);
    }

    private static DanceMoves.Body section(Routine routine, int i, double danced, long ms) {
        var sec = routine.sections().get(i);
        double progress = (ms - sec.startMs()) / (double) Math.max(1, sec.endMs() - sec.startMs());
        return DanceMoves.move(sec.move(), danced, Math.max(0, Math.min(1, progress))).scale(sec.intensity());
    }

    /** Beats since the first one, fractional, following the map's own (slightly uneven) spacing. */
    static double beatCount(long[] beats, long ms, double periodMs) {
        int k = floor(beats, ms);
        if (k < 0) return (ms - beats[0]) / periodMs;
        if (k >= beats.length - 1) return k + (ms - beats[k]) / periodMs;
        return k + (ms - beats[k]) / (double) Math.max(1, beats[k + 1] - beats[k]);
    }

    /** The spin she's in at {@code ms}, in degrees (0 when none): accents spaced by the cooldown, each 0.4 s long. */
    static double spin(long[] accents, long ms, long cooldownMs) {
        long started = Long.MIN_VALUE;
        for (long a : accents) {
            if (a > ms) break;
            if (started == Long.MIN_VALUE || a - started >= cooldownMs) started = a;
        }
        if (started == Long.MIN_VALUE || ms - started >= SPIN_MS) return 0;
        double s = (ms - started) / (double) SPIN_MS;
        return 360 * s * s * (3 - 2 * s);   // eased in and out
    }

    /** The last index with {@code a[i] <= ms}, or -1. */
    private static int floor(long[] a, long ms) {
        int lo = 0, hi = a.length - 1, found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] <= ms) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return found;
    }
}
