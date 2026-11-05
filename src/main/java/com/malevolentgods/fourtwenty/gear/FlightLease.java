package com.malevolentgods.fourtwenty.gear;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Flight the mod lends: the Puff Plate's Puff Pack and Golden God's. Several sources can lend at once, and flight is taken back only when
 * the last lets go; a player's own flight (creative, another mod) is never touched. Flight can never kill: whenever lent flight ends in
 * the air, Slow Falling is kept on until the player lands, however far down that is. Saved on the player, so a crash or restart can't
 * leave flight lent or a fall unsoftened.
 */
public final class FlightLease {
    private FlightLease() {}

    private static final String LEASE = "fourtwenty.flight_lease", OWN = "own", LANDING = "fourtwenty.landing";
    /** Golden God's older flag, read once so a player who logged out mid-flight before this lease existed still lands softly. */
    private static final String OLD_GOD_FLAG = "fourtwenty.god_flight";
    private static final int SOFT_FALL_TICKS = 60;
    public static final float NORMAL_FLY_SPEED = 0.05f;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FlightLease::tick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) releaseAll(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) releaseAll(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) releaseAll(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) releaseAll(p); });
    }

    public static boolean holds(ServerPlayer player, String source) {
        return player.getPersistentData().getCompound(LEASE).getBoolean(source);
    }

    /** Lends flight for {@code source}. If the player already flies by their own means, nothing is lent and nothing will be taken. */
    public static void lend(ServerPlayer player, String source) {
        var data = player.getPersistentData();
        CompoundTag lease = data.getCompound(LEASE);
        if (lease.isEmpty()) lease.putBoolean(OWN, player.getAbilities().mayfly);
        lease.putBoolean(source, true);
        data.put(LEASE, lease);
        data.remove(LANDING);
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    /** Lets go for {@code source}; the last one out takes the flight back, softly. */
    public static void release(ServerPlayer player, String source) {
        var data = player.getPersistentData();
        CompoundTag lease = data.getCompound(LEASE);
        if (!lease.getBoolean(source)) return;
        lease.remove(source);
        if (lease.getAllKeys().stream().anyMatch(k -> !k.equals(OWN))) {
            data.put(LEASE, lease);
            return;
        }
        end(player, lease.getBoolean(OWN));
    }

    /** Takes back everything this lease lent (a login, logout, death or dimension change). */
    public static void releaseAll(ServerPlayer player) {
        var data = player.getPersistentData();
        boolean old = data.getBoolean(OLD_GOD_FLAG);
        data.remove(OLD_GOD_FLAG);
        if (!data.contains(LEASE) && !old) return;
        end(player, data.getCompound(LEASE).getBoolean(OWN) && !old);
    }

    private static void end(ServerPlayer player, boolean ownFlight) {
        player.getPersistentData().remove(LEASE);
        if (ownFlight) return;
        boolean inAir = !player.onGround();
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.getAbilities().setFlyingSpeed(NORMAL_FLY_SPEED);
        player.onUpdateAbilities();
        if (inAir && player.isAlive()) {
            player.getPersistentData().putBoolean(LANDING, true);
            softFall(player);
        }
    }

    private static void softFall(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SOFT_FALL_TICKS, 0, false, false, true));
        player.resetFallDistance();
    }

    /** Keeps a player who lost lent flight floating until they're down. */
    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.getPersistentData().getBoolean(LANDING)) return;
        if (player.onGround() || player.isInWater() || player.getAbilities().flying || !player.isAlive()) {
            player.getPersistentData().remove(LANDING);
            return;
        }
        var effect = player.getEffect(MobEffects.SLOW_FALLING);
        if (effect == null || effect.getDuration() < SOFT_FALL_TICKS / 2) softFall(player);
    }
}
