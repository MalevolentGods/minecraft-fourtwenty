package com.malevolentgods.fourtwenty.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/** An Eighth Ribbon's or Roach Clip's nine wires, as bits (Shown.bits): each wire glows while its bit is 1. */
public class RibbonBlockEntity extends ShowingBlockEntity {
    public RibbonBlockEntity(BlockPos pos, BlockState state) { super(ModElectronics.RIBBON_ENTITY.get(), pos, state); }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadShown(tag);
    }
}
