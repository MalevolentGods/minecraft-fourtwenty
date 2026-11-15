package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.malevolentgods.fourtwenty.compat.minecolonies.mixin.BuilderAIAccessor;
import com.malevolentgods.fourtwenty.compat.minecolonies.mixin.BuilderPathAccessor;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.jobs.JobBuilder;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.util.TeleportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;

import javax.annotation.Nullable;

import static com.minecolonies.api.research.util.ResearchConstants.BLOCK_PLACE_SPEED;

/**
 * The one place that reaches into MineColonies' Builder internals (specs/013-lifted-builders, plan "BuilderAccess").
 * Everything here fails soft: a MineColonies update that moves a field logs once and switches the part off.
 */
public final class BuilderAccess {
    private BuilderAccess() {}

    /** MineColonies' placing delay: BUILD_BLOCK_DELAY * PROGRESS_MULTIPLIER / (placeSpeed/2 + PROGRESS_MULTIPLIER). */
    private static final double BUILD_BLOCK_DELAY = 15, PROGRESS_MULTIPLIER = 10;

    @Nullable
    public static EntityAIStructureBuilder ai(EntityCitizen citizen) {
        try {
            if (citizen.getCitizenJobHandler() == null) return null;
            if (citizen.getCitizenJobHandler().getColonyJob() instanceof JobBuilder job
                    && job.getWorkerAI() instanceof EntityAIStructureBuilder ai) return ai;
        } catch (Throwable t) {
            BuilderLog.missing("builder AI", t);
        }
        return null;
    }

    public static boolean isBuilder(EntityCitizen citizen) {
        return ai(citizen) != null;
    }

    /** Working, and her own AI is building: placing, breaking a block in the way, or walking to the site. */
    /** At work at all (any job state), not on a break: eating, sleeping, fleeing, sick, and so on. */
    public static boolean onTheJob(EntityCitizen citizen) {
        try {
            return citizen.getCitizenAI().getState() == CitizenAIState.WORKING;
        } catch (Throwable t) {
            BuilderLog.missing("AI states", t);
            return false;
        }
    }

    public static boolean building(EntityCitizen citizen) {
        var ai = ai(citizen);
        if (ai == null) return false;
        try {
            if (citizen.getCitizenAI().getState() != CitizenAIState.WORKING) return false;
            var state = ai.getState();
            return state == AIWorkerState.BUILDING_STEP || state == AIWorkerState.MINE_BLOCK;
        } catch (Throwable t) {
            BuilderLog.missing("AI states", t);
            return false;
        }
    }

    @Nullable
    public static AbstractBuildingStructureBuilder hut(EntityCitizen citizen) {
        var data = citizen.getCitizenData();
        return data != null && data.getWorkBuilding() instanceof AbstractBuildingStructureBuilder hut ? hut : null;
    }

    /** The build's progress (iterator position and stage) as one key, or null with nothing in progress. */
    @Nullable
    public static Long progressKey(EntityCitizen citizen) {
        var hut = hut(citizen);
        if (hut == null) return null;
        var progress = hut.getProgress();
        if (progress == null || progress.getA() == null) return null;
        return progress.getA().asLong() * 31 + (progress.getB() == null ? 0 : progress.getB().ordinal());
    }

    /** The block she works on next, in the world. */
    @Nullable
    public static BlockPos nextBlock(EntityCitizen citizen) {
        var ai = ai(citizen);
        if (ai == null) return null;
        try {
            return ((BuilderAIAccessor) ai).fourtwenty$posToWorkAt();
        } catch (Throwable t) {
            BuilderLog.missing("getPosToWorkAt", t);
            return null;
        }
    }

    @Nullable
    public static BlockPos workFrom(EntityCitizen citizen) {
        var ai = ai(citizen);
        if (ai == null) return null;
        try {
            return ((BuilderAIAccessor) ai).fourtwenty$workFrom();
        } catch (Throwable t) {
            BuilderLog.missing("workFrom", t);
            return null;
        }
    }

    /** Forget her standing spot and any route search, so she picks a fresh spot for her next block. */
    public static void clearWorkFrom(EntityCitizen citizen) {
        var ai = ai(citizen);
        if (ai == null) return;
        try {
            ((BuilderAIAccessor) ai).fourtwenty$workFrom(null);
            ((BuilderAIAccessor) ai).fourtwenty$prevBlockPosition(null);
            var path = ((BuilderPathAccessor) ai).fourtwenty$gotoPath();
            if (path != null) path.cancel();
            ((BuilderPathAccessor) ai).fourtwenty$gotoPath(null);
            citizen.getNavigation().stop();
        } catch (Throwable t) {
            BuilderLog.missing("workFrom reset", t);
        }
    }

    /** Ticks per block her skills and the colony's research allow (MineColonies' own placing delay). */
    public static double expectedTicksPerBlock(EntityCitizen citizen) {
        var ai = ai(citizen);
        if (ai == null) return Double.NaN;
        try {
            double decrease = 1 - citizen.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects()
                    .getEffectStrength(BLOCK_PLACE_SPEED);
            return Math.max(1, BUILD_BLOCK_DELAY * PROGRESS_MULTIPLIER / (ai.getPlaceSpeedLevel() / 2.0 + PROGRESS_MULTIPLIER) * decrease);
        } catch (Throwable t) {
            BuilderLog.missing("place speed", t);
            return Double.NaN;
        }
    }

    /** The corners of the building she's working on, or null when it isn't a colony building. */
    @Nullable
    public static Tuple<BlockPos, BlockPos> siteCorners(EntityCitizen citizen) {
        var hut = hut(citizen);
        if (hut == null || hut.getWorkOrder() == null) return null;
        var site = hut.getColony().getServerBuildingManager().getBuilding(hut.getWorkOrder().getLocation());
        return site == null ? null : site.getCorners();
    }

    /** A citizen can stand at the spot: solid underfoot, nothing solid where her feet and head go. Unloaded counts as fine. */
    public static boolean standable(net.minecraft.world.level.Level level, BlockPos spot) {
        if (!level.isLoaded(spot)) return true;
        return level.getBlockState(spot.below()).blocksMotion() && !level.getBlockState(spot).blocksMotion()
                && !level.getBlockState(spot.above()).blocksMotion();
    }

    /** MineColonies' own recall: back to her hut. */
    public static boolean recall(EntityCitizen citizen) {
        var hut = hut(citizen);
        if (hut == null) return false;
        try {
            return TeleportHelper.teleportCitizen(citizen, citizen.level(), hut.getPosition());
        } catch (Throwable t) {
            BuilderLog.missing("recall", t);
            return false;
        }
    }
}
