package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderDiagnostics;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Each block a Builder places or clears: logged for the diagnostics and counted for the watchdog's pace. (US4 adds the
 * Lifted speed here.) The placing delay is set once per step that did work, just before structureStep returns.
 */
@Mixin(value = AbstractEntityAIStructure.class, remap = false)
public abstract class BuilderPlacementMixin {
    @Inject(method = "structureStep", require = 0, at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/core/entity/ai/workers/AbstractEntityAIStructure;setDelay(I)V"))
    private void fourtwenty$placed(CallbackInfoReturnable<?> callback) {
        var self = (AbstractEntityAIStructure<?, ?>) (Object) this;
        if (!(self instanceof EntityAIStructureBuilder)) return;
        var accessor = (BuilderAIAccessor) self;
        BuilderDiagnostics.placed(self.getWorker(), accessor.fourtwenty$posToWorkAt());
    }
}
