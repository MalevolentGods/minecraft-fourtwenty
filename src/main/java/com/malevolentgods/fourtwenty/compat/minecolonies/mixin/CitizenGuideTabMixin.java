package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.client.BudtenderGuideWindow;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.AbstractWindowCitizen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every citizen-window page builds its own tab column; Budtenders get one more tab: the guide. */
@Mixin(value = AbstractWindowCitizen.class, remap = false)
public abstract class CitizenGuideTabMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void fourtwenty$guideTab(ICitizenDataView citizen, ResourceLocation ui, CallbackInfo ci) {
        BudtenderGuideWindow.addTab((AbstractWindowSkeleton) (Object) this, citizen);
    }
}
