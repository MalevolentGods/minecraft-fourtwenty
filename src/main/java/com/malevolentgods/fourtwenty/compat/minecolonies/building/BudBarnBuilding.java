package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryConfig;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.BudBarnRules;
import com.malevolentgods.fourtwenty.processing.CuringRackBlockEntity;
import com.malevolentgods.fourtwenty.processing.TrimTrayBlock;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.util.InventoryUtils;
import net.minecraft.util.Tuple;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The Bud Barn: trims and cures the colony's rough weed by hand. Its Trim Trays and Curing Racks are whatever stands inside its area,
 * found by looking (so a tray a player adds counts too). It keeps rough and trimmed buds; cured buds and seeds go to the warehouse.
 */
public final class BudBarnBuilding extends AbstractBuilding {
    private static final int SCAN_EVERY = 200;
    private List<BlockPos> trays = List.of(), racks = List.of();
    private long scannedAt = Long.MIN_VALUE;
    private int scannedLevel = -1;
    private long curedMade, seedsMade;

    public BudBarnBuilding(IColony colony, BlockPos pos) { super(colony, pos); }

    @Override public String getSchematicName() { return BudBarnBlock.NAME; }
    @Override public int getMaxBuildingLevel() { return 5; }

    public List<BlockPos> trays() { rescan(); return trays; }
    public List<BlockPos> racks() { rescan(); return racks; }

    private void rescan() {
        Level level = getColony().getWorld();
        long now = level.getGameTime();
        if (now - scannedAt < SCAN_EVERY && scannedLevel == getBuildingLevel()) return;
        scannedAt = now;
        scannedLevel = getBuildingLevel();
        var corners = getCorners();
        var foundTrays = new ArrayList<BlockPos>();
        var foundRacks = new ArrayList<BlockPos>();
        for (BlockPos pos : BlockPos.betweenClosed(corners.getA(), corners.getB())) {
            if (!level.isLoaded(pos)) continue;
            if (level.getBlockState(pos).getBlock() instanceof TrimTrayBlock) foundTrays.add(pos.immutable());
            else if (level.getBlockEntity(pos) instanceof CuringRackBlockEntity) foundRacks.add(pos.immutable());
        }
        Comparator<BlockPos> nearHut = Comparator.comparingDouble(p -> p.distSqr(getPosition()));
        foundTrays.sort(nearHut);
        foundRacks.sort(nearHut);
        trays = List.copyOf(foundTrays);
        racks = List.copyOf(foundRacks);
    }

    /** A rack at {@code pos}, if it still stands. */
    public CuringRackBlockEntity rack(BlockPos pos) {
        return getColony().getWorld().getBlockEntity(pos) instanceof CuringRackBlockEntity rack ? rack : null;
    }

    /** What the hut and {@code held} (its worker's inventory, or null) hold, for {@link BudBarnRules}. */
    public BudBarnRules.Snapshot snapshot(IItemHandler held) {
        int hanging = 0, cured = 0, free = 0;
        for (BlockPos pos : racks()) {
            var rack = rack(pos);
            if (rack == null) continue;
            for (ItemStack bud : rack.buds()) {
                if (bud.isEmpty()) free++;
                else {
                    hanging++;
                    if (bud.is(ModItems.WEED_BUD.get())) cured++;
                }
            }
        }
        IItemHandler hut = getItemHandlerCap();
        return new BudBarnRules.Snapshot(trays().size(), racks().size(), hanging, cured, free,
                count(held, ModItems.TRIMMED_BUD.get()), count(hut, ModItems.TRIMMED_BUD.get()),
                count(held, ModItems.ROUGH_BUD.get()), count(hut, ModItems.ROUGH_BUD.get()));
    }

    private static int count(IItemHandler inventory, net.minecraft.world.item.Item item) {
        return inventory == null ? 0 : InventoryUtils.getItemCountInItemHandler(inventory, stack -> stack.is(item));
    }

    public void made(int cured, int seeds) {
        curedMade += cured;
        seedsMade += seeds;
        markDirty();
    }

    public long curedMade() { return curedMade; }
    public long seedsMade() { return seedsMade; }

    /** Rough buds for every tray, and every trimmed bud, stay here and in the worker's pockets; the rest goes to the warehouse. */
    @Override public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount() {
        var required = super.getRequiredItemsAndAmount();
        required.put(stack -> stack.is(ModItems.ROUGH_BUD.get()), new Tuple<>(Math.max(1, trays().size()) * DispensaryConfig.budBarnRoughPerTray(), true));
        required.put(stack -> stack.is(ModItems.TRIMMED_BUD.get()), new Tuple<>(Integer.MAX_VALUE, true));
        return required;
    }

    @Override public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag tag = super.serializeNBT(provider);
        tag.putLong("fourtwentyCuredMade", curedMade);
        tag.putLong("fourtwentySeedsMade", seedsMade);
        return tag;
    }

    @Override public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag) {
        super.deserializeNBT(provider, tag);
        curedMade = tag.getLong("fourtwentyCuredMade");
        seedsMade = tag.getLong("fourtwentySeedsMade");
    }
}
