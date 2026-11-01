package com.malevolentgods.fourtwenty.domain;

/** How a moving light fades with distance: block light in whole steps, like a torch, and the colour film smoothly. */
public final class LightFalloff {
    private LightFalloff() {}

    /** The block light a source of {@code level} gives at {@code distance} blocks. */
    public static int level(int level, double distance) {
        return Math.max(0, level - (int) distance);
    }

    /** How strong the colour is at {@code distance}, from 1 at the source to 0 at {@code reach}. */
    public static float film(double distance, double reach) {
        float left = (float) Math.max(0, 1 - distance / reach);
        return left * left;
    }
}
