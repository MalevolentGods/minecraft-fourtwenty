package com.malevolentgods.fourtwenty.domain;

/** How a lamp's colour fades with distance. */
public final class LightFalloff {
    private LightFalloff() {}

    /** How strong the colour is at {@code distance}, from 1 at the source to 0 at {@code reach}. */
    public static float film(double distance, double reach) {
        float left = (float) Math.max(0, 1 - distance / reach);
        return left * left;
    }
}
