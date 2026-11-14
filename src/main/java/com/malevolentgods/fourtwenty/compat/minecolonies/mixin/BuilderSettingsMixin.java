package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedSetting;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every Builder's hut gets the "Fly when Lifted" setting as MineColonies makes it (spec 013, research R5). */
@Mixin(value = BuildingEntry.class, remap = false)
public abstract class BuilderSettingsMixin {
    @Inject(method = "produceBuilding", at = @At("RETURN"), require = 0)
    private void fourtwenty$flyWhenLifted(BlockPos position, IColony colony, CallbackInfoReturnable<IBuilding> callback) {
        if (LiftedSetting.BUILDER.equals(((BuildingEntry) (Object) this).getRegistryName()) && callback.getReturnValue() != null) {
            LiftedSetting.addTo(callback.getReturnValue());
        }
    }
}
