package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.minecolonies.api.blocks.AbstractBlockMinecoloniesRack;
import com.minecolonies.core.blocks.BlockMinecoloniesRack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Lets players and colonists climb rack stacks instead of treating them as solid walls. */
@Mixin(value = BlockMinecoloniesRack.class, remap = false)
public abstract class RackClimbMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/minecolonies/api/blocks/AbstractBlockMinecoloniesRack;<init>(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V"))
    private static BlockBehaviour.Properties fourtwenty$ladderProperties(BlockBehaviour.Properties properties) {
        return properties.forceSolidOff();
    }

    /** Use the vanilla ladder's thin back panel so entities can enter and climb the rack. */
    @Overwrite
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AbstractBlockMinecoloniesRack.FACING)) {
            case NORTH -> Shapes.box(0, 0, 0.8125, 1, 1, 1);
            case SOUTH -> Shapes.box(0, 0, 0, 1, 1, 0.1875);
            case WEST -> Shapes.box(0.8125, 0, 0, 1, 1, 1);
            case EAST -> Shapes.box(0, 0, 0, 0.1875, 1, 1);
            default -> Shapes.block();
        };
    }
}
