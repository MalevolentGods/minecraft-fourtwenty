package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedFlight;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.PathFindingStatus;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.AbstractPathJob;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While a Builder is Lifted, every walk MineColonies asks of her is flown (specs/015-lifted-body FR-001, research R1).
 * Every walk reaches the navigator as {@code setPathJob(job, destination, ...)}, and MineColonies judges arrival by
 * distance, so: hand back our own path result holding its job, "under way" (its {@code isJobFor} check passes and it
 * waits), compute no walking path, and fly her there; report not done until she's there; never step a walking path
 * meanwhile. Only off the build: on it, spec 013's site flight drives her.
 */
@Mixin(value = MinecoloniesAdvancedPathNavigate.class, remap = false)
public abstract class NavigatorTakeoverMixin {
    @Shadow private PathResult<? extends AbstractPathJob> pathResult;

    /** This navigator's current path is one we're flying, not walking. */
    @Unique private boolean fourtwenty$flown;

    @Unique
    private EntityCitizen fourtwenty$citizen() {
        return ((NavigatorEntityAccessor) this).fourtwenty$entity() instanceof EntityCitizen c ? c : null;
    }

    @Inject(method = "setPathJob", at = @At("HEAD"), cancellable = true, require = 0)
    private void fourtwenty$fly(AbstractPathJob job, BlockPos destination, double speed, boolean safeDestination,
                                CallbackInfoReturnable<PathResult<AbstractPathJob>> callback) {
        var citizen = fourtwenty$citizen();
        if (citizen == null || !LiftedFlight.takesOverWalking(citizen)
                || destination != null && LiftedFlight.lastSteps(citizen, destination)) {
            fourtwenty$flown = false;
            return;
        }
        var result = new PathResult<AbstractPathJob>();
        result.setJob(job);
        if (destination == null) {
            // a walk with nowhere in particular to go (wandering while she waits): while she's high she just stays put
            result.setStatus(PathFindingStatus.COMPLETE);
            result.setPathReachesDestination(true);
            fourtwenty$flown = false;
        } else {
            result.setStatus(PathFindingStatus.IN_PROGRESS_FOLLOWING);
            result.setPathReachesDestination(true);
            LiftedFlight.errand(citizen, destination);
            fourtwenty$flown = true;
        }
        pathResult = result;
        callback.setReturnValue(result);
    }

    @Inject(method = "isDone", at = @At("HEAD"), cancellable = true, require = 0)
    private void fourtwenty$done(CallbackInfoReturnable<Boolean> callback) {
        if (!fourtwenty$flown) return;
        var citizen = fourtwenty$citizen();
        boolean flying = citizen != null && LiftedFlight.onErrand(citizen);
        if (!flying && pathResult != null) pathResult.setStatus(PathFindingStatus.COMPLETE);
        callback.setReturnValue(!flying);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void fourtwenty$noWalking(CallbackInfo ci) {
        if (fourtwenty$flown) ci.cancel();   // our flight moves her
    }

    @Inject(method = "stop", at = @At("HEAD"), require = 0)
    private void fourtwenty$stop(CallbackInfo ci) {
        if (!fourtwenty$flown) return;
        fourtwenty$flown = false;
        var citizen = fourtwenty$citizen();
        if (citizen != null) LiftedFlight.endErrand(citizen);
    }
}
