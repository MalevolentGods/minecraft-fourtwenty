package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderConfig;
import com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedService;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import com.minecolonies.core.entity.ai.workers.util.BuildingProgressStage;
import com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Higher Ground clears first on upgrades (specs/013-lifted-builders FR-015 3, research R8). MineColonies gives an
 * upgrade no CLEAR stage, so it rebuilds in place around the old building. For a Lifted Builder, CLEAR goes back on
 * the front: changed blocks come out top-down before anything is built. Keep-terrain placeholders, blocks that already
 * match and the hut are left alone by CLEAR itself. Adjusted before the handler finds its resume stage, so a resumed
 * upgrade still lands on the right one.
 */
@Mixin(value = BuildingStructureHandler.class, remap = false)
public abstract class HigherGroundStageMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, require = 0)
    private static BuildingProgressStage[] fourtwenty$clearFirst(BuildingProgressStage[] stages, Level world, IWorkOrder workOrder,
                                                                 AbstractEntityAIStructure<?, ?> ai) {
        if (stages.length == 0 || stages[0] != BuildingProgressStage.BUILD_SOLID || !BuilderConfig.clearFirst()) return stages;
        if (!(ai instanceof EntityAIStructureBuilder) || !(ai.getWorker() instanceof EntityCitizen citizen)) return stages;
        if (!LiftedService.isLifted(citizen) || !LiftedService.allowed(citizen)) return stages;
        var out = new BuildingProgressStage[stages.length + 1];
        out[0] = BuildingProgressStage.CLEAR;
        System.arraycopy(stages, 0, out, 1, stages.length);
        return out;
    }
}
