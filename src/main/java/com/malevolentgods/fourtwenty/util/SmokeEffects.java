package com.malevolentgods.fourtwenty.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** The high from a pipe or a bong hit, and the exhale. Server side only. */
public final class SmokeEffects {
    public static void apply(ServerLevel level, Player player) {
        SmokeEvents.smoked(player);
        StrainEffects.markHigh(player, 2400);
        // Smoking inspires colours (a colour drift on screen); with nothing new to inspire, sometimes just the drift.
        if (player instanceof net.minecraft.server.level.ServerPlayer sp
                && !com.malevolentgods.fourtwenty.infuser.Infuser.inspire(sp, com.malevolentgods.fourtwenty.domain.Strain.LANDRACE)
                && level.random.nextFloat() < 0.3f) {
            com.malevolentgods.fourtwenty.infuser.Infuser.woozy(sp);
        }
        player.addEffect(quiet(MobEffects.REGENERATION, 600, 1)); // 30 seconds regeneration II
        player.addEffect(quiet(MobEffects.NIGHT_VISION, 2400, 0)); // 2 minutes night vision
        player.addEffect(quiet(MobEffects.HUNGER, 200, 1)); // 10 seconds hunger II (makes player hungrier)
        player.addEffect(quiet(MobEffects.LUCK, 1800, 0)); // 1.5 minutes luck I (better loot/fishing)
        exhale(level, player, 5);
    }

    /** An effect without swirl particles (they get tiresome on long buffs); the HUD icon still shows. */
    public static MobEffectInstance quiet(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int ticks, int amplifier) {
        return new MobEffectInstance(effect, ticks, amplifier, false, false, true);
    }

    /** Smoke puffs above the player's head. */
    public static void exhale(ServerLevel level, Player player, int puffs) {
        double x = player.getX();
        double y = player.getY() + player.getEyeHeight() + 0.5;
        double z = player.getZ();
        for (int i = 0; i < puffs; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.6;
            double offsetY = level.random.nextDouble() * 0.4;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.6;
            level.sendParticles(ParticleTypes.SMOKE, x + offsetX, y + offsetY, z + offsetZ, 1, 0, 0.1, 0, 0.03);
        }
    }

    private SmokeEffects() {}
}
