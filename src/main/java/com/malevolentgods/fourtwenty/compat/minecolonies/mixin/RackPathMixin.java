package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.util.FaceClimbing;
import com.minecolonies.core.entity.pathfinding.PathPointExtended;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import com.minecolonies.core.entity.pathfinding.PathingOptions;
import com.minecolonies.core.entity.pathfinding.world.CachingBlockLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Colonists climb rack stacks from outside: to MineColonies' route-finding, the open cell beside a rack column is a
 * ladder facing the rack ({@link FaceClimbing#pathFace}), so a colonist walks up to it, presses in and climbs, as on
 * a ladder fixed to a wall. The rack itself stays solid.
 */
@Mixin(value = PathfindingUtils.class, remap = false)
public abstract class RackPathMixin {
    @Inject(method = "isLadder(Lnet/minecraft/world/level/block/state/BlockState;Lcom/minecolonies/core/entity/pathfinding/PathingOptions;IIILcom/minecolonies/core/entity/pathfinding/world/CachingBlockLookup;)Z",
            at = @At("RETURN"), cancellable = true, require = 0)
    private static void fourtwenty$rackFaceIsLadder(BlockState state, PathingOptions options, int x, int y, int z, CachingBlockLookup lookup,
                                                    CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ() && lookup != null && FaceClimbing.pathFace(state, x, y, z, lookup::getBlockState) != null) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "setLadderFacing", at = @At("TAIL"), require = 0)
    private static void fourtwenty$faceTheRack(LevelReader world, BlockPos pos, PathPointExtended point, CallbackInfo callback) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof LadderBlock || state.getBlock() instanceof VineBlock) return;
        Direction toRack = FaceClimbing.pathFace(state, pos.getX(), pos.getY(), pos.getZ(), (x, y, z) -> world.getBlockState(new BlockPos(x, y, z)));
        // A ladder's facing points away from its wall; MineColonies steers a climber against the facing.
        if (toRack != null) point.setLadderFacing(toRack.getOpposite());
    }
}
