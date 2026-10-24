package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.block.CouchBlock;
import com.malevolentgods.fourtwenty.block.SeatEntity;
import com.minecolonies.api.entity.other.AbstractFastMinecoloniesEntity;
import com.minecolonies.api.util.LookHandler;
import com.minecolonies.core.entity.other.SittingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Couches and colonists. MineColonies already seats a citizen on a couch at the right height (on the
 * lowest top of the block's collision boxes: the cushion). This adds two things it only does for
 * stairs or its own seats: a seat a player sits on (FourTwenty's SeatEntity) counts as taken, and a
 * citizen who sits on a couch faces out of it, the way MineColonies turns them on stairs.
 */
@Mixin(value = SittingEntity.class, remap = false)
public abstract class CouchSittingMixin {
    @Inject(method = "isSittingPosOccupied", at = @At("HEAD"), cancellable = true)
    private static void fourtwenty$playerSeat(BlockPos pos, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (SeatEntity.ours(level, pos)) cir.setReturnValue(true);
    }

    @Inject(method = "sitDown", at = @At("RETURN"))
    private static void fourtwenty$faceOutOfCouch(BlockPos pos, Mob mob, int ticks, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = mob.level().getBlockState(pos);
        if (!cir.getReturnValueZ() || !CouchBlock.isCouch(state) || !(mob instanceof AbstractFastMinecoloniesEntity citizen)) return;
        BlockPos look = pos.relative(state.getValue(CouchBlock.FACING).getOpposite()).above();
        if (citizen.getLookControl() instanceof LookHandler handler) {
            handler.setLookAt(look.getX(), look.getY(), look.getZ());
            handler.setLookAtCooldown(40);
        }
    }
}
