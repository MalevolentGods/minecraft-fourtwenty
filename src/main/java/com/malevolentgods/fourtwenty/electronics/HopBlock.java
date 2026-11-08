package com.malevolentgods.fourtwenty.electronics;

import com.malevolentgods.fourtwenty.power.BIGrid;
import com.malevolentgods.fourtwenty.power.WickBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Nah, Skip Me, Bro (a jumper, a crossover): two wires through one block, at right angles, one hopping over the other without touching
 * it. Use a Wick, Hemp Thread or Eighth Ribbon on it to lay one through, along the face you use it on; use the Skip Me item on a straight
 * wire to put that wire through it. Broken, it gives back both.
 */
public class HopBlock extends Block implements EntityBlock {
    private static final VoxelShape CORE = Block.box(3, 3, 3, 13, 13, 13);

    public HopBlock(Properties properties) { super(properties); }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new HopBlockEntity(pos, state); }

    /** Drawn by its renderer (client/HopRenderer): each wire in its own look. */
    @Override protected @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        VoxelShape shape = CORE;
        if (level.getBlockEntity(pos) instanceof HopBlockEntity hop) {
            for (int i = 0; i < 2; i++) {
                var w = hop.wire(i);
                if (w == null) continue;
                shape = Shapes.or(shape, switch (w.axis()) {
                    case X -> Block.box(0, 4, 4, 16, 12, 12);
                    case Y -> Block.box(4, 0, 4, 12, 16, 12);
                    case Z -> Block.box(4, 4, 0, 12, 12, 16);
                });
            }
        }
        return shape;
    }

    /** A wire item a hop takes: a Wick or Hemp Thread (not a cable), or an Eighth Ribbon (not a Roach Clip). */
    public static @Nullable Block wireOf(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi)) return null;
        Block b = bi.getBlock();
        if (b instanceof WickBlock) return b;
        if (b instanceof RibbonBlock && !(b instanceof RoachClipBlock)) return b;
        return null;
    }

    /** Lays the wire in hand through it, along the face it's used on. */
    @Override protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                 @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        Block wire = wireOf(stack);
        if (wire == null || !(level.getBlockEntity(pos) instanceof HopBlockEntity hop)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        Direction.Axis axis = hit.getDirection().getAxis();
        if (!hop.add(new HopBlockEntity.Wire(wire, WickBlock.colourOf(stack), axis))) {
            player.displayClientMessage(Component.translatable(hop.along(axis) >= 0 ? "message.fourtwenty.skip_me_bro.taken" : "message.fourtwenty.skip_me_bro.full"), true);
            return ItemInteractionResult.SUCCESS;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        changed(level, pos, state);
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.1f);
        return ItemInteractionResult.SUCCESS;
    }

    /** Its wires changed: its neighbours see it again (a Wick reaches toward a wire of its colour), and the circuit is walked again. */
    static void changed(Level level, BlockPos pos, BlockState state) {
        level.sendBlockUpdated(pos, state, state, 3);
        for (Direction d : Direction.values()) {
            BlockPos at = pos.relative(d);
            BlockState around = level.getBlockState(at);
            BlockState updated = around.updateShape(d.getOpposite(), state, level, at, pos);
            if (updated != around) level.setBlock(at, updated, 3);
        }
        BIGrid.changed(level);
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        if (!old.is(this)) BIGrid.changed(level);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) BIGrid.changed(level);
        super.onRemove(state, level, pos, next, moved);
    }

    /** Itself, and both its wires back. */
    @Override protected @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof HopBlockEntity hop) {
            for (int i = 0; i < 2; i++) if (hop.wire(i) != null) drops.add(hop.wire(i).item());
        }
        return drops;
    }

    /** The Skip Me item: placed, an empty hop; used on a straight wire, it puts that wire through a hop in its place. */
    public static class Item extends BlockItem {
        public Item(Block block, Properties properties) { super(block, properties); }

        @Override public @Nonnull InteractionResult useOn(@Nonnull UseOnContext context) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState wire = level.getBlockState(pos);
            boolean wick = wire.getBlock() instanceof WickBlock, ribbon = wire.getBlock() instanceof RibbonBlock && !(wire.getBlock() instanceof RoachClipBlock);
            if (!wick && !ribbon) return super.useOn(context);
            // the axis it runs along: straight, or a stub, or alone (then the face it's used on)
            Direction.Axis axis = null;
            boolean bent = false;
            for (Direction d : Direction.values()) {
                boolean linked = wick ? wire.getValue(WickBlock.SIDES.get(d)) != WickBlock.Link.NONE : wire.getValue(RibbonBlock.SIDES.get(d));
                if (!linked) continue;
                if (axis != null && axis != d.getAxis()) bent = true;
                axis = d.getAxis();
            }
            if (bent) {
                if (context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.translatable("message.fourtwenty.skip_me_bro.straight"), true);
                return InteractionResult.FAIL;
            }
            if (axis == null) axis = context.getClickedFace().getAxis();
            if (level.isClientSide) return InteractionResult.SUCCESS;
            WickBlock.Colour colour = wire.getValue(WickBlock.COLOUR);
            BlockState hopState = getBlock().defaultBlockState();
            level.setBlock(pos, hopState, 3);
            if (level.getBlockEntity(pos) instanceof HopBlockEntity hop) hop.add(new HopBlockEntity.Wire(wire.getBlock(), colour, axis));
            changed(level, pos, hopState);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 0.9f);
            return InteractionResult.SUCCESS;
        }
    }
}
