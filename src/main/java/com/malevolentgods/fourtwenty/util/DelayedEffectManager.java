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

import java.util.Map;

public class DelayedEffectManager {
    private static final Map<net.minecraft.server.MinecraftServer, com.malevolentgods.fourtwenty.domain.PendingEffects> sessions = new java.util.WeakHashMap<>();
    private static boolean registered;

    public static void init() {
        if (!registered) {
            NeoForge.EVENT_BUS.register(DelayedEffectManager.class);
            registered = true;
        }
    }

    /** Each server owns a distinct queue even when multiple worlds use the same player UUID. */
    public static boolean scheduleEffect(Player player, int delayTicks, int duration, int amplifier, boolean isPremium) {
        var server = player.getServer();
        if (server == null || player.level().isClientSide() || !player.isAlive()) return false;
        return sessions.computeIfAbsent(server, ignored -> new com.malevolentgods.fourtwenty.domain.PendingEffects())
            .schedule(player.getUUID(), delayTicks, duration, amplifier, isPremium);
    }

    public static boolean hasPendingEffect(Player player) {
        var queue = sessions.get(player.getServer());
        return queue != null && queue.contains(player.getUUID());
    }

    private static void cancel(Player player) {
        var queue = sessions.get(player.getServer());
        if (queue != null) queue.cancel(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        cancel(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.isWasDeath()) cancel(event.getOriginal());
    }

    @SubscribeEvent
    public static void onStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        sessions.remove(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var queue = sessions.get(server);
        if (queue == null) return;
        // Observe finalized life state, not a cancelable death event used by revival mods.
        var due = queue.tick(id -> {
            var player = server.getPlayerList().getPlayer(id);
            return player != null && player.isAlive();
        });
        for (var effect : due) {
            var player = server.getPlayerList().getPlayer(effect.player());
            if (player != null && player.isAlive()) {
                applyWeedEffects(player, effect.duration(), effect.amplifier(), effect.premium());
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

}
