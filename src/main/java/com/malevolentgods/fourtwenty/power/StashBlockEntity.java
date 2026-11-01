package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.List;

/** Stores BI and gives it back at its tier's rate. Next to another Stash it only gives downhill, so they settle level. */
public class StashBlockEntity extends BIBlockEntity implements net.minecraft.world.MenuProvider {
    private static final List<Direction> ALL = List.of(Direction.values());

    private final BIRules.Tier tier;
    private final BIBuffer buffer;
    private final BIPush push = new BIPush();
    private final net.minecraft.world.inventory.ContainerData data;

    public StashBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.STASH_ENTITY.get(), pos, state);
        StashBlock block = (StashBlock) state.getBlock();
        tier = block.tier();
        buffer = new BIBuffer(tier, block.capacity(), true, true, this::setChanged);
        data = BIBlockEntity.data(buffer, () -> level == null ? 0 : level.getGameTime(), tier::ordinal, 0);
    }

    public BINode node() { return buffer; }

    @Override public void serverTick() {
        boolean overloaded = overloadTick(buffer);
        lit(buffer.stored() > 0 && !overloaded);
        if (!overloaded) push.push(level, worldPosition, ALL, tier, tier.rate(), buffer, sink -> !sink.canExtract() || BIRules.flows(buffer.fill(), sink.fill()));
    }

    @Override public @Nonnull net.minecraft.network.chat.Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, @Nonnull net.minecraft.world.entity.player.Inventory inventory, @Nonnull net.minecraft.world.entity.player.Player player) {
        return new BIMenu(id, this, data);
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("bi", buffer.stored());
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.set(tag.getLong("bi"));
    }
}
