package com.malevolentgods.fourtwenty.util;

import com.malevolentgods.fourtwenty.domain.BongVariety;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Players' one-day specialty bong buffs: the variety's potion effects for {@link BongVariety#BUFF_TICKS},
 * and for the Phantom bong creative-style flight for the same time. Flight is kept in the player's
 * persisted data (it survives relogs and death) and enforced by a player tick; when it ends the
 * player gets a few seconds of Slow Falling so it never ends in a fatal drop.
 */
public final class BongBuffs {
    private static final String FLIGHT_UNTIL = "fourtwenty_flight_until";

    public static void apply(ServerLevel level, Player player, BongVariety variety) {
        for (var effect : variety.effects) {
            BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(effect.id())).ifPresent(holder ->
                    player.addEffect(SmokeEffects.quiet(holder, BongVariety.BUFF_TICKS, effect.amplifier())));
        }
        if (variety.flight && player instanceof ServerPlayer server) {
            persisted(server).putLong(FLIGHT_UNTIL, now(server) + BongVariety.BUFF_TICKS);
            enable(server);
            server.displayClientMessage(Component.translatableWithFallback("fourtwenty.bong.flight.start",
                    "You feel weightless. Double-tap jump to fly."), true);
        }
    }

    /** Remaining flight ticks, or 0. */
    public static long flightLeft(ServerPlayer player) {
        return Math.max(0, persisted(player).getLong(FLIGHT_UNTIL) - now(player));
    }

    /** Registered on the NeoForge bus: keeps flight on while the buff lasts, and ends it safely. */
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        var data = persisted(player);
        if (!data.contains(FLIGHT_UNTIL)) return;
        if (now(player) < data.getLong(FLIGHT_UNTIL)) {
            enable(player);
            return;
        }
        data.remove(FLIGHT_UNTIL);
        if (player.isCreative() || player.isSpectator()) return;
        boolean wasFlying = player.getAbilities().flying;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        if (wasFlying || !player.onGround()) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        player.displayClientMessage(Component.translatableWithFallback("fourtwenty.bong.flight.end", "The weightless feeling fades."), true);
    }

    private static void enable(ServerPlayer player) {
        if (player.getAbilities().mayfly) return;
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
    }

    /** Survives death and relogs (NeoForge copies the persisted compound to the respawned player). */
    private static net.minecraft.nbt.CompoundTag persisted(ServerPlayer player) {
        var root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new net.minecraft.nbt.CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    /** One clock for the whole server, so dimension changes don't shift the deadline. */
    private static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }

    private BongBuffs() {}
}
