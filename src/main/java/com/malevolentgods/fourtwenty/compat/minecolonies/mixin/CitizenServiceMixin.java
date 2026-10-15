package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.service.*;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityCitizen.class, remap = false)
public abstract class CitizenServiceMixin implements DispensaryCustomer {
    @Unique private CitizenSmokingTask fourtwenty$task;
    @Override public CitizenSmokingTask fourtwenty$smokingTask() { return fourtwenty$task; }
    @Override public void fourtwenty$smokingTask(CitizenSmokingTask task) { fourtwenty$task = task; }
    @Inject(method = "remove", at = @At("HEAD"))
    private void fourtwenty$remove(Entity.RemovalReason reason, CallbackInfo callback) {
        if (fourtwenty$task != null) fourtwenty$task.interrupt();
    }
}
