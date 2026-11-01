package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.domain.Traits;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/** Holds a planted crop's traits, too many to keep in its block state. A crop without one, from an old world or placed by another mod, reads as rated 2. */
public class WeedCropBlockEntity extends BlockEntity {
    private Traits traits = Traits.DEFAULT;

    public WeedCropBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEED_CROP.get(), pos, state);
    }

    public Traits traits() { return traits; }

    public static Traits at(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WeedCropBlockEntity crop ? crop.traits : Traits.DEFAULT;
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (traits.equals(Traits.DEFAULT)) return;
        tag.putInt("Potency", traits.potency());
        tag.putInt("Yield", traits.yield());
        tag.putInt("Length", traits.length());
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        traits = tag.contains("Potency") ? new Traits(rating(tag, "Potency"), rating(tag, "Yield"), rating(tag, "Length")) : Traits.DEFAULT;
    }

    private static int rating(CompoundTag tag, String key) { return Math.clamp(tag.getInt(key), Traits.MIN, Traits.MAX); }

    @Override protected void applyImplicitComponents(@Nonnull DataComponentInput input) {
        super.applyImplicitComponents(input);
        traits = input.getOrDefault(ModDataComponents.TRAITS.get(), Traits.DEFAULT);
    }

    @Override protected void collectImplicitComponents(@Nonnull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!traits.equals(Traits.DEFAULT)) builder.set(ModDataComponents.TRAITS.get(), traits);
    }

    @Override public @Nonnull CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) { return saveCustomOnly(registries); }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
