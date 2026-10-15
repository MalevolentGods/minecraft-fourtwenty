package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.service.*;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.core.entity.ai.workers.CitizenAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve native next/last-state bookkeeping and all non-work decisions, including the cult's ritual state. */
@Mixin(value = CitizenAI.class, remap = false)
public abstract class CitizenAIMixin {
    @Unique private CitizenSmokingTask fourtwenty$task;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void fourtwenty$register(EntityCitizen citizen, CallbackInfo callback) {
        fourtwenty$task = ((DispensaryCustomer) citizen).fourtwenty$smokingTask();
        if (fourtwenty$task == null) fourtwenty$task = new CitizenSmokingTask(citizen);
        ((DispensaryCustomer) citizen).fourtwenty$smokingTask(fourtwenty$task);
    }
    @Inject(method = "calculateNextState", at = @At("RETURN"), cancellable = true)
    private void fourtwenty$choose(CallbackInfoReturnable<IState> callback) {
        callback.setReturnValue(fourtwenty$task.choose(callback.getReturnValue()));
    }
    // A HEAD-cancelling third-party decision can bypass our RETURN hook above. This outer hook still cleans up.
    @Inject(method = "decideAiTask", at = @At("RETURN"))
    private void fourtwenty$preempt(CallbackInfoReturnable<IState> callback) {
        if (callback.getReturnValue() != null && callback.getReturnValue() != CitizenSmokingTask.State.VISIT) {
            fourtwenty$task.interrupt();
        }
    }
}
