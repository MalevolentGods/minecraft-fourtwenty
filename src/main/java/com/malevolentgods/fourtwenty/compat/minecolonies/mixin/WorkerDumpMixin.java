package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.malevolentgods.fourtwenty.compat.minecolonies.service.JointService;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;

/**
 * Workers periodically dump their inventory into their workplace, keeping only what the building
 * asks for. A colonist's carried doses (a take-away pipe, edibles, a joint) are personal, so they
 * are never dumped, except by the Budtender into his own Dispensary, whose crafted stock must reach
 * the racks. A completely full inventory can still dump them natively ("dump anyway").
 */
@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public abstract class WorkerDumpMixin {
    @Redirect(method = "dumpOneMoreSlot", at = @At(value = "INVOKE", target =
            "Lcom/minecolonies/api/colony/buildings/IBuilding;buildingRequiresCertainAmountOfItem(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;ZLcom/minecolonies/api/colony/jobs/registry/JobEntry;)I"))
    private int fourtwenty$keepCarriedDoses(IBuilding building, ItemStack stack, List<ItemStorage> kept, boolean inventory, JobEntry job) {
        if (inventory && !(building instanceof DispensaryBuilding) && JointService.menuItem(stack) != null) return 0;
        return building.buildingRequiresCertainAmountOfItem(stack, kept, inventory, job);
    }
}
