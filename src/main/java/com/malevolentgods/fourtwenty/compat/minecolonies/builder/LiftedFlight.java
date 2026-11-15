package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.malevolentgods.fourtwenty.domain.FlightPlanner;
import com.malevolentgods.fourtwenty.domain.StandingSpot;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        long walkUntil;
        int descending;   // ticks spent landing, for the backstop
        double bestToNext = Double.MAX_VALUE;   // closest she's got to the next cell of her route
        int noProgress;   // ticks without getting closer to it
        int replans;      // stalls on this trip; too many and she walks
        long lastAsked;            // game time MineColonies last asked her to fly somewhere (approach)
        @Nullable BlockPos goal;   // the hover point this trip is for; legs toward it are kept until each is flown
        @Nullable BlockPos hoverFor, hover;   // the hover point chosen for a block, kept while it still fits
        @Nullable BlockPos tripBlock;   // the block she's on her way to, for the progress backstop
        double tripBest;
        long tripSince;
    }

    /**
     * Flight only holds her while MineColonies keeps asking it to (3 s). Its other walking (mining a block out of the
     * way, fetching) doesn't come through approach(), and holding her in the air then froze her mid-air in MINE_BLOCK
     * for minutes (2026-11-14); after this long without a request she lands and walks the MineColonies way.
     */
    private static final int ASKED_WINDOW_TICKS = 60;

    /**
     * Flying that hasn't got her any closer to her block in this long gives up and walks (5 s). Measured as distance
     * to the block itself: replanning restarts a route, which made every other measure look like progress and let her
     * grind against a wall for half a minute (2026-11-14).
     */
    private static final int ARRIVAL_BACKSTOP_TICKS = 100;

    /** About a second without getting closer to the next cell of her route: something's in the way, plan again. */
    private static final int STALL_TICKS = 20;
    /** Stalls on one trip before she gives up flying it and walks. */
    private static final int MAX_REPLANS = 3;

    /**
     * Free for flying: open (as the watchdog sees it) and with nothing from the block below rising into it. Walls,
     * fences and gates stand 1.5 blocks tall, so the cell above them isn't really open (she flew into wall tops,
     * 2026-11-14).
     */
    static boolean flyFree(ServerLevel level, int x, int y, int z) {
        if (!BuilderWatchdog.free(level, x, y, z)) return false;
        var pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).getCollisionShape(level, pos).max(Direction.Axis.Y) > 0.2) return false;
        var below = pos.below();
        var shape = level.getBlockState(below).getCollisionShape(level, below);
        return shape.isEmpty() || shape.max(Direction.Axis.Y) <= 1.0;
    }

    /** A landing that hasn't touched down by now snaps her to the ground below (5 s). */
    private static final int LANDING_BACKSTOP_TICKS = 100;

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
            boolean flying = !flight.landing && LiftedService.isLifted(citizen) && LiftedService.allowed(citizen) && BuilderAccess.building(citizen)
                    && level.getGameTime() - flight.lastAsked <= ASKED_WINDOW_TICKS;
            if (!flying && !flight.landing) {   // MineColonies walks her itself now: come down first
                flight.landing = true;
                flight.landAt = null;
                flight.descending = 0;
            }
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
     * place), false while she's on her way, null when she should walk this step instead (no flight route, or a
     * failed search in the last few seconds). Trips longer than the planner's radius go in legs.
     */
    @Nullable
    public static Boolean approach(EntityCitizen citizen, BlockPos block, int reach, @Nullable BlockPos hover) {
        if (!(citizen.level() instanceof ServerLevel level)) return true;
        var flight = FLIGHTS.computeIfAbsent(citizen.getUUID(), id -> new Flight());
        long now = level.getGameTime();
        flight.lastAsked = now;
        if (now < flight.walkUntil) return null;
        flight.landing = false;
        BlockPos at = citizen.blockPosition();
        boolean inTheWay = at.equals(block) || at.above().equals(block) || at.getX() == block.getX() && at.getZ() == block.getZ();
        if (!inTheWay && citizen.position().distanceTo(Vec3.atCenterOf(block)) <= reach && (hover == null || flight.target == null || flight.target.equals(hover))) {
            hold(citizen);   // Higher Ground: place everything in reach from here before moving on
            flight.tripBlock = null;
            return true;
        }
        BlockPos goal = hover;
        if (goal == null) {
            // Keep the point chosen for this block while it still fits her: choosing afresh each call could flip
            // between equally good points and send her back and forth.
            if (block.equals(flight.hoverFor) && flight.hover != null
                    && FlightPlanner.fits((x, y, z) -> flyFree(level, x, y, z), flight.hover.getX(), flight.hover.getY(), flight.hover.getZ())) {
                goal = flight.hover;
            } else {
                goal = hoverPoint(level, citizen, block, reach);
                flight.hoverFor = block;
                flight.hover = goal;
            }
        }
        if (goal == null) return walk(citizen, flight, now);
        BlockPos leg = goal;
        int legMax = Math.max(4, BuilderConfig.flightRadius() - 4);
        if (goal.equals(flight.goal) && flight.target != null) {
            leg = flight.target;   // the leg she's flying: finish it before working out the next
        } else if (Math.sqrt(citizen.blockPosition().distSqr(goal)) > legMax) {
            leg = waypoint(level, citizen.blockPosition(), goal, legMax);
            if (leg == null) return walk(citizen, flight, now);
        }
        flight.goal = goal;
        if (flight.replans > MAX_REPLANS) {
            flight.replans = 0;
            return walk(citizen, flight, now);
        }
        if (!leg.equals(flight.target)) {
            var start = startCell(level, citizen);
            var route = FlightPlanner.plan(start, new FlightPlanner.Cell(leg.getX(), leg.getY(), leg.getZ()),
                    (x, y, z) -> flyFree(level, x, y, z), BuilderConfig.flightRadius(), MAX_CELLS);
            if (route.isEmpty()) {
                BuilderLog.flightStall(citizen.getName().getString(), citizen.blockPosition(), "no route to " + leg.toShortString() + ", walking");
                return walk(citizen, flight, now);
            }
            flight.route = route;
            flight.next = 0;
            flight.target = leg;
            flight.bestToNext = Double.MAX_VALUE;
            flight.noProgress = 0;
        }
        if (flight.next >= flight.route.size() && !leg.equals(goal)) {
            flight.replans = 0;
            flight.target = null;   // this leg's done: the next call plans the next one from here
            return false;
        }
        // At the end of the route to her hover point she's there: the point was chosen within reach of the block,
        // and measuring again from her feet (half a block lower) left her hovering on it for good (2026-11-14).
        boolean there = flight.next >= flight.route.size()
                && (leg.equals(goal) || citizen.position().distanceTo(Vec3.atCenterOf(block)) <= reach);
        return arrived(citizen, flight, block, now, there);
    }

    /** Keep track of her getting closer to the block; past the backstop without, land and walk instead of hanging there. */
    @Nullable
    private static Boolean arrived(EntityCitizen citizen, Flight flight, BlockPos block, long now, boolean there) {
        if (there) {
            flight.tripBlock = null;
            return true;
        }
        double d = citizen.position().distanceTo(Vec3.atCenterOf(block));
        if (!block.equals(flight.tripBlock) || d < flight.tripBest - 0.5) {
            flight.tripBlock = block;
            flight.tripBest = d;
            flight.tripSince = now;
        } else if (now - flight.tripSince > ARRIVAL_BACKSTOP_TICKS) {
            BuilderLog.flightStall(citizen.getName().getString(), citizen.blockPosition(), "no closer to " + block.toShortString() + " in 5 s, walking");
            flight.tripBlock = null;
            return walk(citizen, flight, now);
        }
        return false;
    }

    /**
     * Where her route starts: the cell she's in, or the first open one up to two above it. Standing on a stair or slab
     * puts her feet inside a cell that isn't open, and a route can't start there: she never took off again and walked
     * the Warehouse's stairwell stairs, where MineColonies' walking sticks (2026-11-14).
     */
    private static FlightPlanner.Cell startCell(ServerLevel level, EntityCitizen citizen) {
        int x = citizen.getBlockX(), z = citizen.getBlockZ(), y = Mth.floor(citizen.getY() + 0.01);
        for (int dy = 0; dy <= 2; dy++) {
            if (FlightPlanner.fits((xx, yy, zz) -> flyFree(level, xx, yy, zz), x, y + dy, z)) return new FlightPlanner.Cell(x, y + dy, z);
        }
        return new FlightPlanner.Cell(x, y, z);
    }

    /** No flight this step: come down, and walk for a few seconds before trying to fly again. */
    @Nullable
    private static Boolean walk(EntityCitizen citizen, Flight flight, long now) {
        flight.landing = true;
        flight.landAt = null;
        flight.target = null;
        flight.walkUntil = now + 100;
        flight.replans = 0;
        flight.tripBlock = null;
        flight.goal = null;
        flight.hoverFor = null;
        return null;
    }

    /** A cell she fits in about {@code distance} along the way to the goal, rising up to 8 blocks if that's blocked. */
    @Nullable
    private static BlockPos waypoint(ServerLevel level, BlockPos from, BlockPos goal, int distance) {
        double d = Math.sqrt(from.distSqr(goal));
        double t = distance / d;
        int x = (int) Math.round(from.getX() + (goal.getX() - from.getX()) * t);
        int y = (int) Math.round(from.getY() + (goal.getY() - from.getY()) * t);
        int z = (int) Math.round(from.getZ() + (goal.getZ() - from.getZ()) * t);
        FlightPlanner.Free free = (xx, yy, zz) -> flyFree(level, xx, yy, zz);
        for (int dy = 0; dy <= 8; dy++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (FlightPlanner.fits(free, x + dx, y + dy, z + dz)) return new BlockPos(x + dx, y + dy, z + dz);
        }
        return null;
    }

    /** Bring her down gently from wherever she is (keeps her Lifted time; only flying stops). */
    public static void land(LivingEntity citizen) {
        var flight = FLIGHTS.computeIfAbsent(citizen.getUUID(), id -> new Flight());
        flight.landing = true;
        flight.landAt = null;
        flight.descending = 0;
    }

    /** Whether flight is looking after her at all (flying or landing). */
    public static boolean tracked(LivingEntity citizen) {
        return FLIGHTS.containsKey(citizen.getUUID());
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
            flight.bestToNext = Double.MAX_VALUE;
            flight.noProgress = 0;
            return;
        }
        if (distance < flight.bestToNext - 0.02) {
            flight.bestToNext = distance;
            flight.noProgress = 0;
        } else if (++flight.noProgress > STALL_TICKS
                || !FlightPlanner.fits((x, y, z) -> flyFree((ServerLevel) citizen.level(), x, y, z), cell.x(), cell.y(), cell.z())) {
            // Blocked (a block she just placed, or one the plan misjudged): stop and plan again from here.
            BuilderLog.flightStall(citizen.getName().getString(), citizen.blockPosition(), new BlockPos(cell.x(), cell.y(), cell.z()));
            flight.replans++;
            flight.target = null;
            flight.route = List.of();
            flight.next = 0;
            hold(citizen);
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
        if (citizen.onGround() || ++flight.descending > LANDING_BACKSTOP_TICKS) {
            // Anything she's standing on will do (a slab above the spot we aimed for left her hovering on it with
            // gravity off, 2026-11-14); and a landing that can't finish snaps her down rather than hang there.
            if (!citizen.onGround()) snapDown(level, citizen);
            citizen.setNoGravity(false);
            citizen.setDeltaMovement(Vec3.ZERO);
            citizen.fallDistance = 0;
            return true;
        }
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

    /**
     * Higher Ground's hover point (HoverCover): a free cell within reach of the block that also reaches the most of
     * the layer she's building around it. The next blocks are taken as that layer (MineColonies builds layer by layer)
     * rather than read from its iterator.
     */
    @Nullable
    private static BlockPos hoverPoint(ServerLevel level, EntityCitizen citizen, BlockPos block, int reach) {
        FlightPlanner.Free free = (x, y, z) -> BuilderWatchdog.free(level, x, y, z);
        int grid = Math.max(1, BuilderConfig.hoverGrid());
        var candidates = new java.util.ArrayList<com.malevolentgods.fourtwenty.domain.HoverCover.Pos>();
        for (int dy = Math.min(reach, 5); dy >= -1; dy--) for (int dx = -reach; dx <= reach; dx += grid) for (int dz = -reach; dz <= reach; dz += grid) {
            int x = block.getX() + dx, y = block.getY() + dy, z = block.getZ() + dz;
            if (FlightPlanner.fits(free, x, y, z)) candidates.add(new com.malevolentgods.fourtwenty.domain.HoverCover.Pos(x, y, z));
        }
        var next = new java.util.ArrayList<com.malevolentgods.fourtwenty.domain.HoverCover.Pos>();
        var corners = BuilderAccess.siteCorners(citizen);
        int limit = BuilderConfig.hoverLookahead();
        if (corners != null) {
            var a = corners.getA();
            var b = corners.getB();
            int r = reach * 2;
            for (int x = Math.max(Math.min(a.getX(), b.getX()), block.getX() - r); x <= Math.min(Math.max(a.getX(), b.getX()), block.getX() + r) && next.size() < limit; x++)
                for (int z = Math.max(Math.min(a.getZ(), b.getZ()), block.getZ() - r); z <= Math.min(Math.max(a.getZ(), b.getZ()), block.getZ() + r) && next.size() < limit; z++)
                    next.add(new com.malevolentgods.fourtwenty.domain.HoverCover.Pos(x, block.getY(), z));
        }
        var current = new com.malevolentgods.fourtwenty.domain.HoverCover.Pos(block.getX(), block.getY(), block.getZ());
        if (next.isEmpty()) next.add(current);
        return com.malevolentgods.fourtwenty.domain.HoverCover.choose(candidates, current, next, reach)
                .map(p -> new BlockPos(p.x(), p.y(), p.z())).orElse(null);
    }

    private static final float TURN_DEGREES_PER_TICK = 12;

    private static void face(Mob mob, Vec3 step) {
        if (step.horizontalDistanceSqr() < 1e-6) return;
        // Turn toward where she's going a little each tick: snapping to every cell of a zigzag route looked like a
        // twitchy dance (2026-11-14).
        float target = (float) (Mth.atan2(step.z, step.x) * (180 / Math.PI)) - 90;
        float yaw = Mth.approachDegrees(mob.getYRot(), target, TURN_DEGREES_PER_TICK);
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
