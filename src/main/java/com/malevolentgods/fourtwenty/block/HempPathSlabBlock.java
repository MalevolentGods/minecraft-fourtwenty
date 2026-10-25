package com.malevolentgods.fourtwenty.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * A road slab, sized to the road rather than a full block: a bottom slab is 7/16 high (half a step
 * down from a road), a top slab runs from 7/16 to the road's 15/16 (flush on a bottom slab or a tread beside it), and a double slab is a road's
 * full 15/16. So a top or double slab is flush with a road and joins it ({@link RoadShape}), and
 * bottom slabs join each other; the border runs round the whole surface. Same speed as its road.
 */
public class HempPathSlabBlock extends SlabBlock implements RoadSurface {
    public static final MapCodec<HempPathSlabBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(b -> b.tier), propertiesCodec()).apply(i, HempPathSlabBlock::new));
    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 7, 16), TOP = Block.box(0, 7, 0, 16, 15, 16), DOUBLE = Block.box(0, 0, 0, 16, 15, 16);

    private final int tier;

    public HempPathSlabBlock(int tier, Properties properties) {
        super(properties.speedFactor(HempPathBlock.speedFactorFor(HempPathBlock.gainFor(tier))));
        this.tier = tier;
    }

    @Override public int tier() { return tier; }

    @Override public int surface(BlockState state) { return state.getValue(TYPE) == SlabType.BOTTOM ? 7 : 15; }

    @Override public @Nonnull MapCodec<? extends SlabBlock> codec() { return CODEC; }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return switch (state.getValue(TYPE)) { case BOTTOM -> BOTTOM; case TOP -> TOP; default -> DOUBLE; };
    }

    /** Light and culling follow the shape, double slabs too (they're 15/16, like a road). */
    @Override protected boolean useShapeForLightOcclusion(@Nonnull BlockState state) { return true; }
}
