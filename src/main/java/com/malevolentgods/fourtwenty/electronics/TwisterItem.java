package com.malevolentgods.fourtwenty.electronics;

import com.malevolentgods.fourtwenty.power.BIGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * The Twister (a wrench): turns a component a quarter turn about the face you use it on, clockwise as you look at that face; sneak to
 * turn it back. Any of the 24 orientations is a few twists away. Like the meter, it never works the part it touches.
 */
public class TwisterItem extends Item {
    public TwisterItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override public @Nonnull InteractionResult useOn(@Nonnull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ComponentBlock)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Direction face = context.getClickedFace();
        // looking at a face, clockwise is the negative turn about its outward axis for faces pointing the positive way, and the reverse
        boolean clockwise = (face.getAxisDirection() == Direction.AxisDirection.POSITIVE) != (context.getPlayer() != null && context.getPlayer().isSecondaryUseActive());
        // a part mounted on a Rolling Tray faces out from it: there's no other way for it to turn
        if (state.hasProperty(ComponentBlock.MOUNTED) && state.getValue(ComponentBlock.MOUNTED)) return InteractionResult.PASS;
        if (state.getBlock() instanceof BigChipBlock) {
            // a big chip stands on its own height: it turns about that, both blocks together (set quietly, so neither sees the other
            // half turned away and goes)
            Direction height = BigChipBlock.toPartner(state);
            boolean cw = clockwise;
            BlockPos partner = pos.relative(height);
            BlockState other = level.getBlockState(partner);
            level.setBlock(pos, turn(state, height.getAxis(), cw), 2 | 16);
            if (BigChipBlock.partnerOf(state, other)) level.setBlock(partner, turn(other, height.getAxis(), cw), 2 | 16);
            BIGrid.changed(level);
            level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.6f, 1.6f);
            return InteractionResult.SUCCESS;
        }
        BlockState turned = turn(state, face.getAxis(), clockwise);
        level.setBlock(pos, turned, 3);
        BIGrid.changed(level);
        level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.6f, 1.6f);
        return InteractionResult.SUCCESS;
    }

    /** A component's state turned a quarter about {@code axis}. */
    public static BlockState turn(BlockState state, Direction.Axis axis, boolean clockwise) {
        Direction facing = state.getValue(ComponentBlock.FACING);
        Direction top = Orientation.top(facing, state.getValue(ComponentBlock.SPIN));
        Direction newFacing = Orientation.turn(facing, axis, clockwise), newTop = Orientation.turn(top, axis, clockwise);
        return state.setValue(ComponentBlock.FACING, newFacing).setValue(ComponentBlock.SPIN, Orientation.spinFor(newFacing, newTop));
    }

    /** With it in hand, a block's own right-click doesn't happen (a Toggle Joint doesn't flip as you twist it). */
    public static void rightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof TwisterItem) event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
    }
}
