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

    @Inject(method = "walkToConstructionSite", at = @At("HEAD"), require = 0)
    private void fourtwenty$beforeWalk(BlockPos currentBlock, CallbackInfoReturnable<Boolean> callback) {
        var self = (EntityAIStructureBuilder) (Object) this;
        var worker = ((AbstractEntityAIStructure<?, ?>) self).getWorker();
        var access = (BuilderAIAccessor) self;
        if (BuilderDiagnostics.takeStepAside(worker) && BuilderConfig.fixStepAside() && access.fourtwenty$workFrom() != null) {
            BuilderDiagnostics.workFromChanged(worker, access.fourtwenty$workFrom(), null, "stepaside");
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
