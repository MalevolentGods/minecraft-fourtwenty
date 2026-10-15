package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

/** Targets are individual joints, never inventory slots or stacks. */
public final class ProductionRules {
    public static int stockTarget(int completedLevel) {
        return switch (completedLevel) {
            case 1 -> 16;
            case 2 -> 32;
            case 3 -> 64;
            case 4 -> 96;
            case 5 -> 128;
            default -> 0;
        };
    }
    public static int batch(int completedLevel, int held, boolean outstanding) {
        return outstanding ? 0 : Math.min(16, Math.max(0, stockTarget(completedLevel) - held));
    }
    private ProductionRules() {}
}
