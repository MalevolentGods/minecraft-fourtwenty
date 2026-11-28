package com.malevolentgods.fourtwenty.compat.minecolonies.tiedye;

import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The Tie Dye Shack (specs/018-tie-dye-shack research R5). Its stations are whatever stands inside its area: dye vats (water cauldrons)
 * and drying racks (Curing Racks). It keeps the current batch's missing supplies; everything else goes to the warehouse. Orders live with
 * the colony ({@link TieDyeOrders}), not here.
 */
public final class ShackBuilding extends AbstractBuilding {
    public enum Activity { IDLE, WAITING, WORKING, PAUSED }

    private static final int SCAN_EVERY = 200;
    private List<BlockPos> vats = List.of(), racks = List.of();
    private long scannedAt = Long.MIN_VALUE;
    private int scannedLevel = -1;
    private Activity activity = Activity.IDLE;
    private String reason = "";
    private long made;

    public ShackBuilding(IColony colony, BlockPos pos) { super(colony, pos); }

    @Override public String getSchematicName() { return ShackBlock.NAME; }
    @Override public int getMaxBuildingLevel() { return 5; }

    public List<BlockPos> vats() { rescan(); return vats; }
    public List<BlockPos> racks() { rescan(); return racks; }

    private void rescan() {
        Level level = getColony().getWorld();
        long now = level.getGameTime();
        if (now - scannedAt < SCAN_EVERY && scannedLevel == getBuildingLevel()) return;
        scannedAt = now;
        scannedLevel = getBuildingLevel();
        var corners = getCorners();
        List<BlockPos> v = new ArrayList<>(), r = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(corners.getA(), corners.getB())) {
            if (!level.isLoaded(pos)) continue;
            var state = level.getBlockState(pos);
            if (state.is(Blocks.WATER_CAULDRON)) v.add(pos.immutable());
            else if (state.getBlock() instanceof com.malevolentgods.fourtwenty.processing.CuringRackBlock) r.add(pos.immutable());
        }
        v.sort(Comparator.comparingDouble(p -> p.distSqr(getPosition())));
        r.sort(Comparator.comparingDouble(p -> p.distSqr(getPosition())));
        vats = List.copyOf(v);
        racks = List.copyOf(r);
    }

    public static boolean matches(ItemStack stack, TieDyeRules.Supply supply) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(supply.item());
    }

    public void report(Activity activity, String reason) {
        this.activity = activity;
        this.reason = reason == null ? "" : reason;
    }

    public Activity activity() { return activity; }
    public String reason() { return reason; }
    public long made() { return made; }

    public void madeSome(int n) {
        made += n;
        markDirty();
    }

    /** The current batch's missing supplies stay here; the rest is picked up for the warehouse. */
    @Override public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount() {
        var required = super.getRequiredItemsAndAmount();
        if (!getColony().getWorld().isClientSide()) {
            for (TieDyeRules.Supply s : TieDyeOrders.record(getColony()).missing()) required.put(stack -> matches(stack, s), new Tuple<>(s.count(), true));
        }
        return required;
    }

    @Override public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag tag = super.serializeNBT(provider);
        tag.putLong("fourtwentyTieDyed", made);
        return tag;
    }

    @Override public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag) {
        super.deserializeNBT(provider, tag);
        made = tag.getLong("fourtwentyTieDyed");
    }
}
