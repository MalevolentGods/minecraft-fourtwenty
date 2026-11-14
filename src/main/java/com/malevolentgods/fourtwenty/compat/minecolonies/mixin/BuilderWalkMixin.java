package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderConfig;
import com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderDiagnostics;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The crawl fix (FR-002, research R1): after a Builder steps aside from her own column, forget her old standing spot,
 * so her next block picks a fresh one out of the way instead of a route search back to the old one for every block.
 * Also logs standing-spot changes and route searches for the diagnostics (FR-001).
 */
@Mixin(value = EntityAIStructureBuilder.class, remap = false)
public abstract class BuilderWalkMixin {
    @Unique private BlockPos fourtwenty$workFromBefore;
    @Unique private boolean fourtwenty$searchingBefore;

    @Inject(method = "walkToConstructionSite", at = @At("HEAD"), cancellable = true, require = 0)
    private void fourtwenty$beforeWalk(BlockPos currentBlock, CallbackInfoReturnable<Boolean> callback) {
        var self = (EntityAIStructureBuilder) (Object) this;
        var worker = ((AbstractEntityAIStructure<?, ?>) self).getWorker();
        // A Lifted Builder flies to her work instead of walking (spec 013 US3/US4).
        if (worker instanceof com.minecolonies.core.entity.citizen.EntityCitizen citizen
                && com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedService.isLifted(citizen)
                && com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedService.allowed(citizen)) {
            callback.setReturnValue(com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedFlight.approach(
                    citizen, currentBlock, com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderConfig.reach(), null));
            return;
        }
        var access = (BuilderAIAccessor) self;
        if (BuilderDiagnostics.takeStepAside(worker) && BuilderConfig.fixStepAside() && access.fourtwenty$workFrom() != null) {
            BuilderDiagnostics.workFromChanged(worker, access.fourtwenty$workFrom(), null, "stepaside");
            access.fourtwenty$workFrom(null);
            access.fourtwenty$prevBlockPosition(null);
        }
        // Her standing spot built over (seen in game 2026-11-13: she walled in her own spot and stood a block from it
        // for minutes). MineColonies never drops a spot that just became solid, so drop it here and she picks a fresh one.
        var spot = access.fourtwenty$workFrom();
        if (spot != null && BuilderConfig.fixStepAside() && !com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderAccess.standable(worker.level(), spot)) {
            BuilderDiagnostics.workFromChanged(worker, spot, null, "blocked");
            access.fourtwenty$workFrom(null);
            access.fourtwenty$prevBlockPosition(null);
        }
        fourtwenty$workFromBefore = access.fourtwenty$workFrom();
        fourtwenty$searchingBefore = ((BuilderPathAccessor) self).fourtwenty$gotoPath() != null;
    }

    @Inject(method = "walkToConstructionSite", at = @At("RETURN"), require = 0)
    private void fourtwenty$afterWalk(BlockPos currentBlock, CallbackInfoReturnable<Boolean> callback) {
        if (!BuilderConfig.logBuilderPathing()) return;
        var self = (EntityAIStructureBuilder) (Object) this;
        var worker = ((AbstractEntityAIStructure<?, ?>) self).getWorker();
        var now = ((BuilderAIAccessor) self).fourtwenty$workFrom();
        if (!fourtwenty$searchingBefore && ((BuilderPathAccessor) self).fourtwenty$gotoPath() != null) BuilderDiagnostics.routeSearch(worker, currentBlock);
        BuilderDiagnostics.workFromChanged(worker, fourtwenty$workFromBefore, now, now == null ? "column|stuck|far" : "path");
    }
}
