package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.GrowRules;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/** Gives the crop above extra random ticks. Out of power it gives none, and the crop just grows as it would on farmland. */
public class GrowOpBlockEntity extends BIBlockEntity implements MenuProvider {
    private static final int BUFFER_TICKS = 400;

    private final GrowOpBlock block;
    private final BIBuffer buffer;
    private final MachinePower.Meter meter = new MachinePower.Meter();
    private final ContainerData data;
    private boolean running;

    public GrowOpBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.GROW_OP_ENTITY.get(), pos, state);
        block = (GrowOpBlock) state.getBlock();
        BIRules.Tier tier = block.tier();
        buffer = new BIBuffer(tier == null ? BIRules.Tier.BLAZE : tier, tier == null ? 0 : (long) Math.ceil(tier.machine() * BUFFER_TICKS), tier != null, false, this::setChanged);
        data = BIBlockEntity.data(buffer, () -> level == null ? 0 : level.getGameTime(), () -> tier == null ? 0 : tier.ordinal(), BIBlockEntity.KIND_PLOT, block.plotLevel());
    }

    public BINode node() { return buffer; }

    public double speed() { return ProcessingConfig.plotSpeed(block.plotLevel()); }

    /** Whether it's growing a crop at its full speed right now. */
    public boolean running() { return running; }

    @Override public void serverTick() {
        boolean overloaded = overloadTick(buffer);
        BlockPos above = worldPosition.above();
        BlockState crop = level.getBlockState(above);
        boolean hasCrop = GrowOpBlock.isCrop(crop);
        BIRules.Tier tier = block.tier();
        running = hasCrop && !overloaded && (tier == null || MachinePower.steps(level, worldPosition, buffer, tier.machine(), meter) > 0);
        lit(running && tier != null);
        if (running && level instanceof ServerLevel server) {
            double chance = GrowRules.extraTickChance(speed(), server.getGameRules().getInt(GameRules.RULE_RANDOMTICKING));
            if (server.random.nextDouble() < chance) crop.randomTick(server, above, server.random);
        }
    }

    @Override public @Nonnull Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
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
