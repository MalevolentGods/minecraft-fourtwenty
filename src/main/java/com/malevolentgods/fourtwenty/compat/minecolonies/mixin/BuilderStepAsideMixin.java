package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderDiagnostics;
import com.minecolonies.api.entity.other.AbstractFastMinecoloniesEntity;
import com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Notes each time a Builder steps out of her own column to place a block (research R1), for the crawl fix and the log. */
@Mixin(value = BuildingStructureHandler.class, remap = false)
public abstract class BuilderStepAsideMixin {
    @Redirect(method = "prePlacementLogic", require = 0, at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/core/entity/pathfinding/navigation/EntityNavigationUtils;walkAwayFrom(Lcom/minecolonies/api/entity/other/AbstractFastMinecoloniesEntity;Lnet/minecraft/core/BlockPos;ID)Z"))
    private boolean fourtwenty$noteStepAside(AbstractFastMinecoloniesEntity worker, BlockPos block, int distance, double speed) {
        BuilderDiagnostics.steppedAside(worker, block);
        return EntityNavigationUtils.walkAwayFrom(worker, block, distance, speed);
    }
}
