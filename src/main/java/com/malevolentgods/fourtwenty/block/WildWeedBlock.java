package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;

public class WildWeedBlock extends BushBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final MapCodec<WildWeedBlock> CODEC = simpleCodec(WildWeedBlock::new);
    
    // Different shapes for different ages
    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
        Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),   // Age 0
        Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),   // Age 1
        Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),   // Age 2
        Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),   // Age 3
        Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0),  // Age 4
        Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0),  // Age 5
        Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0),  // Age 6
        Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)   // Age 7
    };

    public WildWeedBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[state.getValue(AGE)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    public int getAge(BlockState state) {
        return state.getValue(AGE);
    }

    public BlockState getStateForAge(int age) {
        return this.defaultBlockState().setValue(AGE, age);
    }

    public int getMaxAge() {
        return 7;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // Allow placement on grass blocks, dirt, podzol, etc.
        return state.is(net.minecraft.tags.BlockTags.DIRT) || 
               state.getBlock() == net.minecraft.world.level.block.Blocks.GRASS_BLOCK ||
               state.getBlock() == net.minecraft.world.level.block.Blocks.DIRT ||
               state.getBlock() == net.minecraft.world.level.block.Blocks.PODZOL ||
               state.getBlock() == net.minecraft.world.level.block.Blocks.COARSE_DIRT;
    }

}
