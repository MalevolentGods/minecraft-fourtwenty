package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.malevolentgods.fourtwenty.domain.BuilderWatchRules;
import com.malevolentgods.fourtwenty.domain.BuilderWatchRules.Step;
import com.malevolentgods.fourtwenty.domain.StandingSpot;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rescues Builders stuck or crawling while they build (specs/013-lifted-builders FR-003 to FR-007). Once a second it
 * samples each Builder: only while she's WORKING and her own AI is building does the watch run; anything else (meals,
 * the restaurant, sleep, fetching, a raid) resets it. {@link BuilderWatchRules} decides; this applies the rescue.
 */
public final class BuilderWatchdog {
    private BuilderWatchdog() {}

    private static final int EVERY = 20;
    private static final Map<UUID, BuilderWatchRules.State> WATCHES = new HashMap<>();

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % EVERY != 0) return;
        if (!level.tickRateManager().runsNormally()) return;
        boolean watching = BuilderConfig.watchdogEnabled();
        var cfg = new BuilderWatchRules.Config(BuilderConfig.slowTicks(), BuilderConfig.slowFraction(), BuilderConfig.stuckTicks());
        Set<UUID> seen = new HashSet<>();
        for (var colony : IColonyManager.getInstance().getColonies(level)) {
            for (var data : colony.getCitizenManager().getCitizens()) {
                var entity = data.getEntity();
                if (entity.isEmpty() || !(entity.get() instanceof EntityCitizen citizen) || !citizen.isAlive()) continue;
                if (!BuilderAccess.isBuilder(citizen)) continue;
                seen.add(citizen.getUUID());
                var state = WATCHES.computeIfAbsent(citizen.getUUID(), id -> new BuilderWatchRules.State());
                var sample = new BuilderWatchRules.Sample(level.getGameTime(), BuilderAccess.building(citizen),
                        BuilderAccess.progressKey(citizen), BuilderDiagnostics.placements(citizen));
                LiftedService.tick(level, colony, citizen, EVERY);
                if (!watching) continue;
                var decision = BuilderWatchRules.next(state, sample, BuilderAccess.expectedTicksPerBlock(citizen), cfg);
                if (decision.rescue() != Step.NONE) rescue(level, colony.getID(), citizen, decision.rescue());
            }
        }
        WATCHES.keySet().retainAll(seen);
    }

    private static void rescue(ServerLevel level, int colony, EntityCitizen citizen, Step step) {
        BlockPos next = BuilderAccess.nextBlock(citizen);
        BlockPos at = citizen.blockPosition();
        Object building = BuilderAccess.hut(citizen) == null ? "?" : BuilderAccess.hut(citizen).getPosition().toShortString();
        String name = citizen.getName().getString();
        switch (step) {
            case FRESH_SPOT -> BuilderAccess.clearWorkFrom(citizen);
            case NEAR_BLOCK -> {
                var spot = next == null ? null : nearest(level, next, BuilderConfig.rescueRadius(), null);
                if (spot == null) {
                    step = Step.OUTSIDE;   // nowhere to stand by the block: go straight to the next step
                    rescue(level, colony, citizen, step);
                    return;
                }
                moveTo(citizen, spot);
            }
            case OUTSIDE -> {
                var corners = BuilderAccess.siteCorners(citizen);
                BlockPos spot = null;
                if (corners != null) {
                    var a = corners.getA();
                    var b = corners.getB();
                    var box = new StandingSpot.Box(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()) - 1, Math.min(a.getZ(), b.getZ()),
                            Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()));
                    // The nearest point just outside the footprint, from where she is.
                    int x = Math.clamp(at.getX(), box.minX() - 1, box.maxX() + 1);
                    int z = Math.clamp(at.getZ(), box.minZ() - 1, box.maxZ() + 1);
                    if (box.contains(x, at.getY(), z)) {
                        int west = x - box.minX(), east = box.maxX() - x, north = z - box.minZ(), south = box.maxZ() - z;
                        int least = Math.min(Math.min(west, east), Math.min(north, south));
                        if (least == west) x = box.minX() - 1;
                        else if (least == east) x = box.maxX() + 1;
                        else if (least == north) z = box.minZ() - 1;
                        else z = box.maxZ() + 1;
                    }
                    spot = nearest(level, new BlockPos(x, at.getY(), z), BuilderConfig.rescueRadius() + 2, box);
                }
                if (spot == null) {
                    rescue(level, colony, citizen, Step.RECALL);
                    return;
                }
                moveTo(citizen, spot);
            }
            case RECALL -> BuilderAccess.recall(citizen);
            default -> { return; }
        }
        BuilderLog.rescue(step.name(), colony, name, building, at.toShortString(), next == null ? "?" : next.toShortString());
    }

    private static BlockPos nearest(ServerLevel level, BlockPos centre, int radius, StandingSpot.Box outside) {
        return StandingSpot.nearest(centre.getX(), centre.getY(), centre.getZ(), radius,
                        (x, y, z) -> solid(level, x, y, z), (x, y, z) -> free(level, x, y, z), outside, 6)
                .map(s -> new BlockPos(s.x(), s.y(), s.z())).orElse(null);
    }

    static boolean solid(ServerLevel level, int x, int y, int z) {
        var pos = new BlockPos(x, y, z);
        return level.isLoaded(pos) && level.getBlockState(pos).blocksMotion();
    }

    static boolean free(ServerLevel level, int x, int y, int z) {
        var pos = new BlockPos(x, y, z);
        if (!level.isLoaded(pos)) return false;
        var state = level.getBlockState(pos);
        return !state.blocksMotion() && state.getFluidState().isEmpty();
    }

    private static void moveTo(EntityCitizen citizen, BlockPos spot) {
        BuilderAccess.clearWorkFrom(citizen);
        citizen.getNavigation().stop();
        citizen.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        citizen.fallDistance = 0;
    }
}
