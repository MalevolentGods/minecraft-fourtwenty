package com.malevolentgods.fourtwenty.electronics;

import com.malevolentgods.fourtwenty.power.WickBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Nah, Skip Me, Bro: two wires crossing in one block, each running straight along its own axis, neither touching the other. Each is a
 * Wick (any tier, any colour), Hemp Thread, or an Eighth Ribbon.
 */
public class HopBlockEntity extends BlockEntity {
    /** One of the wires: which wire block it is, its colour (Wicks and thread), and the axis it runs along. */
    public record Wire(Block block, WickBlock.Colour colour, Direction.Axis axis) {
        public boolean ribbon() { return block instanceof RibbonBlock; }

        /** The wire it stands for, straight along its axis: what it looks like, and what it joins. */
        public BlockState state() {
            BlockState s = block.defaultBlockState();
            if (block instanceof WickBlock) {
                s = s.setValue(WickBlock.COLOUR, colour);
                for (Direction d : ends(axis)) s = s.setValue(WickBlock.SIDES.get(d), WickBlock.Link.WIRE);
            } else if (block instanceof RibbonBlock) {
                s = s.setValue(RibbonBlock.COLOUR, colour);
                for (Direction d : ends(axis)) s = s.setValue(RibbonBlock.SIDES.get(d), true);
            }
            return s;
        }

        /** The item it was placed from, to give back. */
        public ItemStack item() {
            ItemStack stack = new ItemStack(block);
            if (colour != WickBlock.Colour.NONE) {
                stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                        net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(WickBlock.COLOUR, colour));
            }
            return stack;
        }
    }

    public static Direction[] ends(Direction.Axis axis) {
        return new Direction[]{Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE), Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE)};
    }

    private final Wire[] wires = new Wire[2];

    public HopBlockEntity(BlockPos pos, BlockState state) { super(ModElectronics.HOP_ENTITY.get(), pos, state); }

    public @Nullable Wire wire(int i) { return wires[i]; }

    /** Which of its wires runs along {@code axis}, or -1. */
    public int along(Direction.Axis axis) {
        for (int i = 0; i < 2; i++) if (wires[i] != null && wires[i].axis() == axis) return i;
        return -1;
    }

    /** Lays a wire through it; false if one already runs that way or both are taken. */
    public boolean add(Wire wire) {
        if (along(wire.axis()) >= 0) return false;
        for (int i = 0; i < 2; i++) {
            if (wires[i] == null) {
                wires[i] = wire;
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** The wire a neighbour on side {@code side} of the block at {@code pos} meets, or null: the one running that way. */
    public static @Nullable Wire facing(net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof HopBlockEntity hop && hop.along(side.getAxis()) >= 0 ? hop.wires[hop.along(side.getAxis())] : null;
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < 2; i++) {
            if (wires[i] == null) continue;
            CompoundTag w = new CompoundTag();
            w.putString("block", BuiltInRegistries.BLOCK.getKey(wires[i].block()).toString());
            w.putString("colour", wires[i].colour().getSerializedName());
            w.putString("axis", wires[i].axis().getSerializedName());
            tag.put("wire" + i, w);
        }
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < 2; i++) {
            wires[i] = null;
            if (!tag.contains("wire" + i)) continue;
            CompoundTag w = tag.getCompound("wire" + i);
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(w.getString("block")));
            if (!(block instanceof WickBlock) && !(block instanceof RibbonBlock)) continue;
            WickBlock.Colour colour = WickBlock.Colour.NONE;
            for (WickBlock.Colour c : WickBlock.Colour.values()) if (c.getSerializedName().equals(w.getString("colour"))) colour = c;
            Direction.Axis axis = Direction.Axis.byName(w.getString("axis"));
            if (axis != null) wires[i] = new Wire(block, colour, axis);
        }
    }

    @Override public @Nonnull CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
