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
        // a part mounted on a Rolling Tray faces out from it: there's no other way for it to turn. A key's number (for a Dankoder) is set
        // with it instead: 0 to F, or its place on the board
        if (state.hasProperty(ComponentBlock.MOUNTED) && state.getValue(ComponentBlock.MOUNTED)) {
            if (!(state.getBlock() instanceof ComponentBlock cb) || !cb.type().key() || !(level.getBlockEntity(pos) instanceof ComponentBlockEntity be)) {
                return InteractionResult.PASS;
            }
            be.step(context.getPlayer() != null && context.getPlayer().isSecondaryUseActive() ? -1 : 1);
            BIGrid.changed(level);
            level.sendBlockUpdated(pos, state, state, 2);
            int key = (int) Math.round(be.value());
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(key < 0 ? net.minecraft.network.chat.Component.translatable("message.fourtwenty.dankoder.auto")
                        : net.minecraft.network.chat.Component.translatable("message.fourtwenty.dankoder.key", Integer.toHexString(key).toUpperCase(java.util.Locale.ROOT)), true);
            }
            level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.5f, 1.9f);
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
