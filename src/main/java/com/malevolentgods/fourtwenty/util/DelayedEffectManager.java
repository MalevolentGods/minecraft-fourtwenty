package com.malevolentgods.fourtwenty.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Iterator;

public class DelayedEffectManager {
    private static final Map<UUID, PendingEffect> pendingEffects = new HashMap<>();

    public static void init() {
        NeoForge.EVENT_BUS.register(DelayedEffectManager.class);
    }

    public static void scheduleEffect(Player player, int delayTicks, int duration, int amplifier, boolean isPremium) {
        pendingEffects.put(player.getUUID(), new PendingEffect(delayTicks, duration, amplifier, isPremium));
    }

    public static boolean hasPendingEffect(Player player) {
        return pendingEffects.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, PendingEffect>> iterator = pendingEffects.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingEffect> entry = iterator.next();
            UUID playerId = entry.getKey();
            PendingEffect effect = entry.getValue();
            
            // Find the player
            Player player = null;
            for (Level level : event.getServer().getAllLevels()) {
                player = level.getPlayerByUUID(playerId);
                if (player != null) break;
            }
            
            if (player == null) {
                // Player not found, remove effect
                iterator.remove();
                continue;
            }
            
            effect.ticksRemaining--;
            
            if (effect.ticksRemaining <= 0) {
                // Apply the effects
                applyWeedEffects(player, effect.duration, effect.amplifier, effect.isPremium);
                iterator.remove();
            }
        }
    }

    private static void applyWeedEffects(Player player, int duration, int amplifier, boolean isPremium) {
        Level level = player.level();
        
        if (!level.isClientSide()) {
            // Base effects similar to joint but longer duration
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration / 4, amplifier)); // Regeneration
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0)); // Night vision
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration / 8, amplifier + 1)); // Munchies
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration * 3 / 4, 0)); // Luck

            if (isPremium) {
                // Premium edibles get additional effects
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration / 2, amplifier)); // Jump boost
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration / 3, amplifier)); // Speed
            }
            
            // Reduced chance for confusion on edibles (they're more mellow)
            if (level.random.nextFloat() < 0.15f) { // 15% chance vs 30% for joints
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration / 6, 0)); // Shorter confusion
            }

            // Spawn effect onset particles
            if (level instanceof ServerLevel serverLevel) {
                double x = player.getX();
                double y = player.getY() + player.getEyeHeight();
                double z = player.getZ();
                
                // Create onset effect particles
                for (int i = 0; i < 8; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 1.0;
                    double offsetY = level.random.nextDouble() * 0.5;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 1.0;
                    
                    serverLevel.sendParticles(ParticleTypes.ENCHANT, 
                        x + offsetX, y + offsetY, z + offsetZ, 
                        1, 0, 0.1, 0, 0.1);
                }
            }
        }
    }

    private static class PendingEffect {
        int ticksRemaining;
        final int duration;
        final int amplifier;
        final boolean isPremium;

        PendingEffect(int delay, int duration, int amplifier, boolean isPremium) {
            this.ticksRemaining = delay;
            this.duration = duration;
            this.amplifier = amplifier;
            this.isPremium = isPremium;
        }
    }
}
