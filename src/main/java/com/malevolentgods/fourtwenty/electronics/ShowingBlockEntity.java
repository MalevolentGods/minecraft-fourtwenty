package com.malevolentgods.fourtwenty.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * A block that shows its circuit (domain/electronics/Shown): the server sets what it shows every tick; clients get it only when it
 * changes, at most every {@link #EVERY} ticks, so a fast circuit doesn't flood the network. Not saved: the circuit sets it again on its
 * first tick.
 */
public abstract class ShowingBlockEntity extends BlockEntity {
    /** At most four updates a second. */
    public static final int EVERY = 5;
    private int shown, sent = Integer.MIN_VALUE;
    private long sentAt = Long.MIN_VALUE / 2;

    protected ShowingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    /** What it shows (Shown.pack). Client side, the last value the server sent. */
    public int shown() { return shown; }

    /** Server: what it shows now; sent on when it differs from what clients have and they haven't had one in {@link #EVERY} ticks. */
    public void show(int value) {
        shown = value;
        if (level == null || level.isClientSide || value == sent) return;
        long now = level.getGameTime();
        if (now - sentAt < EVERY) return;
        sent = value;
        sentAt = now;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    /** What goes to clients: what it shows, and whatever {@link #updateTag} adds. */
    @Override public @Nonnull CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("shown", shown);
        updateTag(tag);
        return tag;
    }

    protected void updateTag(CompoundTag tag) {}

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    /** Call from loadAdditional: picks up what it shows when the tag carries it (an update), leaves it otherwise (a save). */
    protected void loadShown(CompoundTag tag) {
        if (tag.contains("shown")) shown = tag.getInt("shown");
    }
}
