package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderAccess;
import com.malevolentgods.fourtwenty.compat.minecolonies.builder.StorageTransfer;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Before a Builder breaks a block that holds items, its contents go to her hut instead of spilling on the site
 * (specs/013-lifted-builders FR-016, clarification Q3). Runs for every block she mines, so it covers Higher Ground's
 * clear-first and MineColonies' own clearing alike.
 */
@Mixin(value = AbstractEntityAIStructure.class, remap = false)
public abstract class HigherGroundStorageMixin {
    @ModifyArg(method = "doMining", index = 0, require = 0, at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/core/entity/ai/workers/AbstractEntityAIStructure;mineBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Z"))
    private BlockPos fourtwenty$emptyFirst(BlockPos block) {
        var self = (AbstractEntityAIStructure<?, ?>) (Object) this;
        if (!(self instanceof EntityAIStructureBuilder) || !(self.getWorker() instanceof EntityCitizen citizen)) return block;
        var level = citizen.level();
        var contents = level.getCapability(Capabilities.ItemHandler.BLOCK, block, null);
        if (contents == null) return block;
        var hut = BuilderAccess.hut(citizen);
        StorageTransfer.move(contents, hut == null ? null : hut.getItemHandlerCap(), level, hut == null ? citizen.blockPosition() : hut.getPosition());
        return block;
    }

    /**
     * Higher Ground mines from wherever she's hovering: MineColonies then also walks her to her ground standing spot
     * (and gives up past 12 blocks from it), which a flying Builder never reaches, so she hung in MINE_BLOCK for
     * minutes (2026-11-14). Flight already brought her within reach of the block, so her spot is where she is.
     */
    @ModifyArg(method = "doMining", index = 1, require = 0, at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/core/entity/ai/workers/AbstractEntityAIStructure;mineBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Z"))
    private BlockPos fourtwenty$mineFromHere(BlockPos safeStand) {
        var self = (AbstractEntityAIStructure<?, ?>) (Object) this;
        if (!(self instanceof EntityAIStructureBuilder) || !(self.getWorker() instanceof EntityCitizen citizen)) return safeStand;
        if (!com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedFlight.inFlight(citizen)) return safeStand;
        return citizen.blockPosition();
    }
}
