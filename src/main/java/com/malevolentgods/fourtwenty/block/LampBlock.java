package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nonnull;

/**
 * A coloured lamp. A regular lamp lights while powered by redstone; an inverted lamp is lit unless
 * powered (always on as decoration). Lit lamps glow at full brightness.
 */
public class LampBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private final boolean inverted;
    /** Halo colours (0xRRGGBBAA) for the block and the item. */
    private final int halo, itemHalo;

    /** A lamp in one of the 16 dye colours (ProjectRed's halo colours). */
    public LampBlock(net.minecraft.world.item.DyeColor color, boolean inverted, Properties properties) {
        this(com.malevolentgods.fourtwenty.domain.LampGlow.rgba(color.getId()), com.malevolentgods.fourtwenty.domain.LampGlow.itemRgba(color.getId()),
                inverted, properties);
    }

    /** A lamp in any colour (0xRRGGBB), such as the terpene palette. */
    public LampBlock(int rgb, boolean inverted, Properties properties) {
        this(com.malevolentgods.fourtwenty.domain.LampGlow.forRgb(rgb), com.malevolentgods.fourtwenty.domain.LampGlow.itemForRgb(rgb), inverted, properties);
    }

    private LampBlock(int halo, int itemHalo, boolean inverted, Properties properties) {
        super(properties);
        this.halo = halo;
        this.itemHalo = itemHalo;
        this.inverted = inverted;
        registerDefaultState(stateDefinition.any().setValue(LIT, inverted));
    }

    public boolean inverted() { return inverted; }
    public int halo() { return halo; }
    public int itemHalo() { return itemHalo; }

    /** Only so the client can draw the halo (see LampHaloRenderer). */
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new com.malevolentgods.fourtwenty.blockentity.LampBlockEntity(pos, state);
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(LIT, inverted != context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override protected void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                             @Nonnull Block neighbor, @Nonnull BlockPos neighborPos, boolean moving) {
        if (level.isClientSide()) return;
        boolean lit = inverted != level.hasNeighborSignal(pos);
        if (lit != state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
    }
}
