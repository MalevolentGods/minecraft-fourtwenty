package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.malevolentgods.fourtwenty.domain.FlightPlanner;
import com.malevolentgods.fourtwenty.domain.StandingSpot;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A Lifted Builder in the air (specs/013-lifted-builders FR-012, FR-014, research R7). While she's building and may
 * fly, she has no gravity and follows {@link FlightPlanner} routes to a spot within reach of her work. Whenever that
 * stops being true she lands gently, and nothing ever leaves her in the air: an unloading chunk or a restart puts her
 * on the first floor below.
 */
public final class LiftedFlight {
    private LiftedFlight() {}

    /** About her walking speed, in blocks a tick. */
    private static final double SPEED = 0.2;
    private static final int MAX_CELLS = 4000;

    private static final class Flight {
        List<FlightPlanner.Cell> route = List.of();
        int next;
        BlockPos target;
        boolean landing;
        @Nullable BlockPos landAt;
    }

    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();

    /** Every tick, for Lifted Builders only. */
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.tickRateManager().runsNormally()) return;
        if (FLIGHTS.isEmpty()) return;
        var it = FLIGHTS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (!(level.getEntity(entry.getKey()) instanceof EntityCitizen citizen) || !citizen.isAlive()) continue;
            var flight = entry.getValue();
            boolean flying = !flight.landing && LiftedService.isLifted(citizen) && LiftedService.allowed(citizen) && BuilderAccess.building(citizen);
            citizen.fallDistance = 0;
            if (flying) {
                fly(citizen, flight);
            } else if (descend(level, citizen, flight)) {
                it.remove();
            }
        }
    }

    /**
     * Higher Ground's walk-to-site: fly to a spot within {@code reach} of the block. True once she's there (she may
     * place), false while she's on her way. With no route at all she lands and MineColonies walks her instead.
     */
    public static boolean approach(EntityCitizen citizen, BlockPos block, int reach, @Nullable BlockPos hover) {
        if (!(citizen.level() instanceof ServerLevel level)) return true;
        var flight = FLIGHTS.computeIfAbsent(citizen.getUUID(), id -> new Flight());
        flight.landing = false;
        if (citizen.position().distanceTo(Vec3.atCenterOf(block)) <= reach && (hover == null || flight.target == null || flight.target.equals(hover))) {
            hold(citizen);
            return true;
        }
        BlockPos goal = hover != null ? hover : spotNear(level, block, reach, citizen.blockPosition());
        if (goal == null) {
            land(citizen);
            return false;
        }
        if (!goal.equals(flight.target)) {
            var start = new FlightPlanner.Cell(citizen.getBlockX(), Mth.floor(citizen.getY() + 0.01), citizen.getBlockZ());
            var route = FlightPlanner.plan(start, new FlightPlanner.Cell(goal.getX(), goal.getY(), goal.getZ()),
                    (x, y, z) -> BuilderWatchdog.free(level, x, y, z), BuilderConfig.flightRadius(), MAX_CELLS);
            if (route.isEmpty()) {
                land(citizen);
                return false;
            }
            flight.route = route;
            flight.next = 0;
            flight.target = goal;
        }
        return citizen.position().distanceTo(Vec3.atCenterOf(block)) <= reach && flight.next >= flight.route.size();
    }

    /** Bring her down gently from wherever she is (keeps her Lifted time; only flying stops). */
    public static void land(LivingEntity citizen) {
        var flight = FLIGHTS.computeIfAbsent(citizen.getUUID(), id -> new Flight());
        flight.landing = true;
        flight.landAt = null;
    }

    public static boolean inFlight(LivingEntity citizen) {
        var f = FLIGHTS.get(citizen.getUUID());
        return f != null && !f.landing;
    }

    private static void fly(EntityCitizen citizen, Flight flight) {
        citizen.setNoGravity(true);
        citizen.getNavigation().stop();
        if (flight.next >= flight.route.size()) {
            hold(citizen);
            return;
        }
        var cell = flight.route.get(flight.next);
        Vec3 to = new Vec3(cell.x() + 0.5, cell.y() + 0.05, cell.z() + 0.5);
        Vec3 delta = to.subtract(citizen.position());
        double distance = delta.length();
        if (distance < 0.25) {
            flight.next++;
            return;
        }
        Vec3 step = delta.scale(Math.min(SPEED, distance) / distance);
        citizen.setDeltaMovement(step);
        citizen.hasImpulse = true;
        face(citizen, step);
    }

    /** Hover in place. */
    private static void hold(EntityCitizen citizen) {
        citizen.setNoGravity(true);
        citizen.setDeltaMovement(citizen.getDeltaMovement().multiply(0.5, 0, 0.5));
    }

    /** One tick of a gentle landing; true once she's down. */
    private static boolean descend(ServerLevel level, EntityCitizen citizen, Flight flight) {
        if (citizen.onGround() && !citizen.isNoGravity()) return true;
        citizen.getNavigation().stop();   // her own walking waits until she's down
        if (flight.landAt == null) flight.landAt = landingSpot(level, citizen);
        if (flight.landAt == null) {   // nowhere to stand within reach: straight down, slowly, until she touches something
            citizen.setNoGravity(true);
            citizen.setDeltaMovement(0, -BuilderConfig.landingSpeed(), 0);
            if (citizen.onGround()) {
                citizen.setNoGravity(false);
                return true;
            }
            return false;
        }
        Vec3 to = new Vec3(flight.landAt.getX() + 0.5, flight.landAt.getY(), flight.landAt.getZ() + 0.5);
        Vec3 delta = to.subtract(citizen.position());
        if (delta.length() < 0.2 || citizen.onGround() && Math.abs(delta.y) < 0.6) {
            citizen.setNoGravity(false);
            citizen.setDeltaMovement(Vec3.ZERO);
            citizen.fallDistance = 0;
            return true;
        }
        citizen.setNoGravity(true);
        double horizontal = Math.min(SPEED, Math.hypot(delta.x, delta.z));
        double flat = Math.hypot(delta.x, delta.z);
        double dy = Math.max(-BuilderConfig.landingSpeed(), Math.min(SPEED, delta.y));
        citizen.setDeltaMovement(flat < 1e-6 ? 0 : delta.x / flat * horizontal, dy, flat < 1e-6 ? 0 : delta.z / flat * horizontal);
        citizen.hasImpulse = true;
        return false;
    }

    /**
     * Where to come down: the first floor straight below her. (Seen in game 2026-11-14: steering her to a spot outside the
     * footprint flew her into the building's own walls and she never landed. Down is always clear: she flew there, and
     * once on the floor MineColonies walks her out, with the watchdog behind it.)
     */
    @Nullable
    private static BlockPos landingSpot(ServerLevel level, EntityCitizen citizen) {
        BlockPos at = citizen.blockPosition();
        return StandingSpot.below(at.getX(), at.getY(), at.getZ(), 64,
                        (x, y, z) -> BuilderWatchdog.solid(level, x, y, z), (x, y, z) -> BuilderWatchdog.free(level, x, y, z))
                .map(s -> new BlockPos(s.x(), s.y(), s.z())).orElse(null);
    }

    /** A free cell within reach of the block, preferring above it, then nearest her. */
    @Nullable
    private static BlockPos spotNear(ServerLevel level, BlockPos block, int reach, BlockPos from) {
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        int r = Math.min(reach, 6);
        for (int dy = r; dy >= -2; dy--) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            int x = block.getX() + dx, y = block.getY() + dy, z = block.getZ() + dz;
            if (dx * dx + dy * dy + dz * dz > reach * reach) continue;
            if (dx == 0 && dz == 0 && dy <= 1) continue;   // never in the block's own column, right at it
            if (!FlightPlanner.fits((xx, yy, zz) -> BuilderWatchdog.free(level, xx, yy, zz), x, y, z)) continue;
            double score = from.distSqr(new BlockPos(x, y, z)) - dy * 2.0;
            if (score < bestScore) {
                bestScore = score;
                best = new BlockPos(x, y, z);
            }
        }
        return best;
    }

    private static void face(Mob mob, Vec3 step) {
        if (step.horizontalDistanceSqr() < 1e-6) return;
        float yaw = (float) (Mth.atan2(step.z, step.x) * (180 / Math.PI)) - 90;
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
    }

    /** A chunk unloading under a flying Builder: put her on the floor below, never leave her in the air. */
    public static void leave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof EntityCitizen citizen) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (FLIGHTS.remove(citizen.getUUID()) != null || citizen.isNoGravity()) snapDown(level, citizen);
    }

    /** A Builder loading in the air (a restart, a chunk coming back): down to the first floor below, gravity back on. */
    public static void join(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof EntityCitizen citizen) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!citizen.isNoGravity()) return;
        snapDown(level, citizen);
    }

    private static void snapDown(ServerLevel level, EntityCitizen citizen) {
        citizen.setNoGravity(false);
        citizen.fallDistance = 0;
        BlockPos at = citizen.blockPosition();
        StandingSpot.below(at.getX(), at.getY(), at.getZ(), 128, (x, y, z) -> BuilderWatchdog.solid(level, x, y, z),
                        (x, y, z) -> BuilderWatchdog.free(level, x, y, z))
                .ifPresent(s -> citizen.setPos(s.x() + 0.5, s.y(), s.z() + 0.5));
    }

    /** Test hook (game tests): land any mob the same way. */
    public static boolean landStep(ServerLevel level, Mob mob, BlockPos landAt) {
        var flight = new Flight();
        flight.landing = true;
        flight.landAt = landAt;
        mob.fallDistance = 0;
        Vec3 to = new Vec3(landAt.getX() + 0.5, landAt.getY(), landAt.getZ() + 0.5);
        Vec3 delta = to.subtract(mob.position());
        if (delta.length() < 0.2 || mob.onGround() && Math.abs(delta.y) < 0.6) {
            mob.setNoGravity(false);
            return true;
        }
        mob.setNoGravity(true);
        mob.setDeltaMovement(0, Math.max(-BuilderConfig.landingSpeed(), Math.min(SPEED, delta.y)), 0);
        return false;
    }

    /** For colonies being unloaded or deleted. */
    public static void forget(UUID citizen) {
        FLIGHTS.remove(citizen);
    }
}
