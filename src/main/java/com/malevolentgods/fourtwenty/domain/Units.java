package com.malevolentgods.fourtwenty.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Readable electrical values in the mod's units: Willards (W), Willard-seconds (Ws), Ignus (Ig), Kerberos (Kb), Phus (Φ). Three
 * significant figures. Values from 1 up to 9,999 are shown plainly (4.20 Kb, 4,200 Kb); smaller ones take m, µ or n, and larger ones k, M
 * or G (168 mΦ, 25.2 kWs).
 */
public final class Units {
    private Units() {}

    public static final String WATTS = "W", WS = "Ws", AMPS = "Ig", VOLTS = "Kb", OHMS = "Φ";
    private static final String[] SMALL = {"m", "µ", "n"};
    private static final String[] LARGE = {"k", "M", "G"};
    private static final MathContext THREE = new MathContext(3, RoundingMode.HALF_UP);

    public static String format(double value, String unit) {
        if (value == 0 || !Double.isFinite(value)) return "0 " + unit;
        double abs = Math.abs(value);
        String prefix = "";
        double scaled = value;
        if (abs < 1) {
            for (int i = 0; i < SMALL.length && Math.abs(scaled) < 1; i++) {
                scaled *= 1000;
                prefix = SMALL[i];
            }
        } else if (abs >= 10_000) {
            for (int i = 0; i < LARGE.length && Math.abs(scaled) >= 1000; i++) {
                scaled /= 1000;
                prefix = LARGE[i];
            }
        }
        BigDecimal rounded = new BigDecimal(scaled).round(THREE);
        // rounding up can reach the next prefix, as 999.7 m to 1,000 m: show it as 1.00 instead
        if (!prefix.isEmpty() && rounded.abs().compareTo(BigDecimal.valueOf(1000)) >= 0) return format(Math.signum(value) * rounded.abs().doubleValue() * scale(prefix), unit);
        return text(rounded) + " " + prefix + unit;
    }

    private static double scale(String prefix) {
        for (int i = 0; i < SMALL.length; i++) if (SMALL[i].equals(prefix)) return Math.pow(1000, -(i + 1));
        for (int i = 0; i < LARGE.length; i++) if (LARGE[i].equals(prefix)) return Math.pow(1000, i + 1);
        return 1;
    }

    private static String text(BigDecimal rounded) {
        int digits = rounded.precision() - rounded.scale();
        int scale = Math.max(0, 3 - digits);
        return String.format(Locale.ROOT, "%,." + scale + "f", rounded.setScale(scale, RoundingMode.HALF_UP));
    }
}
