package com.malevolentgods.fourtwenty.domain;

/**
 * A Lit Bit as a real RGB LED behind a constant-current driver (specs/008-lit-bits/research.md R3). Below its forward voltage it is dark
 * and draws nothing; between that and its full voltage its current rises in a straight line; from its full voltage up the driver holds
 * the current it is set to. Its full voltage is a little under its rated one (the driver's headroom), so a healthy line lights it fully. Each channel sets its share of the current, plus a tiny idle draw for the driver itself.
 */
public final class LedRules {
    private LedRules() {}

    /** The tunable numbers; the config builds these. Voltages in Kerberos, currents in Ignus. */
    public record Numbers(double ratedVolts, double forwardShare, double fullShare, double channelAmps, double idleAmps) {
        public static final Numbers DEFAULT = new Numbers(4.2, 0.6, 0.95, 0.238, 0.002);

        public double forwardVolts() { return ratedVolts * forwardShare; }

        /** Where the driver reaches its set current. */
        public double fullVolts() { return ratedVolts * fullShare; }

        /** The current the driver is set to for a colour, at full voltage. */
        public double setAmps(int r, int g, int b) {
            return (clamp8(r) + clamp8(g) + clamp8(b)) / 255.0 * channelAmps + idleAmps;
        }

        /** How far up its knee the LED is at {@code volts} across it: 0 dark, 1 full. */
        public double brightness(double volts) {
            double span = fullVolts() - forwardVolts();
            if (volts <= forwardVolts()) return 0;
            return span <= 0 ? 1 : Math.min(1, (volts - forwardVolts()) / span);
        }

        /** The current through it at {@code volts}; nothing at all wired backwards or below its knee. */
        public double amps(int r, int g, int b, double volts) {
            return setAmps(r, g, b) * brightness(volts);
        }
    }

    public static int clamp8(int channel) { return Math.max(0, Math.min(255, channel)); }

    /** An RGB colour as one int, 0xRRGGBB. */
    public static int pack(int r, int g, int b) { return clamp8(r) << 16 | clamp8(g) << 8 | clamp8(b); }

    public static int red(int rgb) { return rgb >> 16 & 0xFF; }

    public static int green(int rgb) { return rgb >> 8 & 0xFF; }

    public static int blue(int rgb) { return rgb & 0xFF; }

    /** The light a lamp gives, 0 to 15, from its brightest channel and how lit it is. */
    public static int lightLevel(int rgb, double brightness) {
        int top = Math.max(red(rgb), Math.max(green(rgb), blue(rgb)));
        return (int) Math.round(15 * top / 255.0 * Math.max(0, Math.min(1, brightness)));
    }

    /** Hue (degrees), saturation and value (0 to 1) to RGB. */
    public static int hsv(double hue, double saturation, double value) {
        double h = ((hue % 360) + 360) % 360 / 60, s = Math.max(0, Math.min(1, saturation)), v = Math.max(0, Math.min(1, value));
        int sector = (int) Math.floor(h);
        double f = h - sector, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        double r, g, b;
        switch (sector) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return pack((int) Math.round(r * 255), (int) Math.round(g * 255), (int) Math.round(b * 255));
    }
}
