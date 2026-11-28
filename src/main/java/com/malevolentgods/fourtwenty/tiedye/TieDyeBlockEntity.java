package com.malevolentgods.fourtwenty.tiedye;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * A placed tie-dyed block's colourway (research R1): its pattern is in the block state, its colourway here. Taken from the item on
 * placement and given back to the drop (as the item's `fourtwenty:tie_dye` component); sent to clients, which re-mesh the block so its
 * colour handler tints the pattern's four slots.
 */
public class TieDyeBlockEntity extends BlockEntity {
    private String colourway = "ashtray";

    public TieDyeBlockEntity(BlockPos pos, BlockState state) { super(ModTieDye.ENTITY.get(), pos, state); }

    public String colourway() { return colourway; }

    public void setColourway(String id) {
        if (id == null || id.equals(colourway)) return;
        colourway = id;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public TieDye look() { return new TieDye(getBlockState().getValue(TieDyeBlocks.PATTERN), colourway); }

    @Override protected void applyImplicitComponents(@Nonnull DataComponentInput input) {
        TieDye look = input.get(com.malevolentgods.fourtwenty.registry.ModDataComponents.TIE_DYE.get());
        if (look != null) colourway = look.colourway();
    }

    @Override protected void collectImplicitComponents(@Nonnull DataComponentMap.Builder builder) {
        builder.set(com.malevolentgods.fourtwenty.registry.ModDataComponents.TIE_DYE.get(), look());
    }

    @Override public void removeComponentsFromTag(@Nonnull CompoundTag tag) { tag.remove("colourway"); }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("colourway", colourway);
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String before = colourway;
        if (tag.contains("colourway")) colourway = tag.getString("colourway");
        // on a client the colour comes from here, so re-mesh when it arrives or changes
        if (level != null && level.isClientSide() && !before.equals(colourway)) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
    }

    @Override public @Nonnull CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("colourway", colourway);
        return tag;
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
    }
}
