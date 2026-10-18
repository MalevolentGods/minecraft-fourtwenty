package com.malevolentgods.fourtwenty.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Core-side hook for "someone just smoked here" (players finishing a joint or pipe). The optional
 * MineColonies integration sets the listener to expose nearby colonists to secondhand smoke; core
 * code never references MineColonies.
 */
public final class SmokeEvents {
    public static volatile java.util.function.BiConsumer<ServerLevel, LivingEntity> smokedListener;

    public static void smoked(LivingEntity smoker) {
        var listener = smokedListener;
        if (listener != null && smoker.level() instanceof ServerLevel level) listener.accept(level, smoker);
    }

    private SmokeEvents() {}
}
