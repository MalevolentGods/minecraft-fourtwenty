package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.ConstructionRules;
import com.malevolentgods.fourtwenty.power.BIBlockEntity;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.BINode;
import com.malevolentgods.fourtwenty.power.MachinePower;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/** A Construction bench's store of BI. Each batch taken from it costs BI, or nothing while a high player is near. */
public class ConstructionBlockEntity extends BIBlockEntity implements MenuProvider {
    public static final int DATA_READY = 0, DATA_YIELD = 1, DATA_OVERLOADED = 2, DATA_COUNT = 3;

    private final ConstructionJointBlock block;
    private final BIBuffer buffer;
    private boolean overloaded;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_READY -> ready() ? 1 : 0;
                case DATA_YIELD -> ConstructionRules.factor(tier());
                case DATA_OVERLOADED -> overloaded ? 1 : 0;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}

        @Override public int getCount() { return DATA_COUNT; }
    };

    public ConstructionBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.CONSTRUCTION_ENTITY.get(), pos, state);
        block = (ConstructionJointBlock) state.getBlock();
        buffer = new BIBuffer(block.tier(), ConstructionRules.cost(block.tier()) * ConstructionRules.BATCHES_HELD, true, false, this::setChanged);
    }

    public BINode node() { return buffer; }

    public BIRules.Tier tier() { return block.tier(); }

    /** Whether a batch can be paid for now. */
    public boolean ready() {
        return !overloaded && level != null && (MachinePower.highNear(level, worldPosition) || buffer.stored() >= ConstructionRules.cost(tier()));
    }

    /** Pays for a batch. */
    public void pay() {
        if (level != null && !MachinePower.highNear(level, worldPosition)) buffer.extract(ConstructionRules.cost(tier()));
    }

    @Override public void serverTick() {
        overloaded = overloadTick(buffer);
        lit(ready());
    }

    @Override public @Nonnull Component getDisplayName() { return block.getName(); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new ConstructionMenu(id, inventory, ContainerLevelAccess.create(level, worldPosition), this, data);
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
