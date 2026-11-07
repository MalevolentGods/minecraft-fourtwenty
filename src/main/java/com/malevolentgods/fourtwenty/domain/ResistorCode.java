package com.malevolentgods.fourtwenty.domain;

/** The resistor colour code: a value's two significant digits and its multiplier as band colours (0xRRGGBB). */
public final class ResistorCode {
    private ResistorCode() {}

    /** Black, brown, red, orange, yellow, green, blue, violet, grey, white: 0 to 9. */
    private static final int[] CODE = {0x1E1E1E, 0x7A4A21, 0xD02020, 0xF08020, 0xF0D020, 0x30A040, 0x2050D0, 0x8040C0, 0x808080, 0xF0F0F0};
    public static final int GOLD = 0xD4AF37, SILVER = 0xC0C0C0;

    /** The colour of band {@code band} (0: first digit, 1: second, 2: multiplier) for {@code ohms}; gold is ×0.1, silver ×0.01. */
    public static int band(double ohms, int band) {
        if (ohms <= 0) return CODE[0];
        int exponent = (int) Math.floor(Math.log10(ohms) + 1e-9) - 1;
        int digits = (int) Math.round(ohms / Math.pow(10, exponent));
        if (digits >= 100) {
            digits /= 10;
            exponent++;
        }
        return switch (band) {
            case 0 -> CODE[digits / 10 % 10];
            case 1 -> CODE[digits % 10];
            default -> exponent == -1 ? GOLD : exponent == -2 ? SILVER : CODE[Math.max(0, Math.min(9, exponent))];
        };
    }
}
