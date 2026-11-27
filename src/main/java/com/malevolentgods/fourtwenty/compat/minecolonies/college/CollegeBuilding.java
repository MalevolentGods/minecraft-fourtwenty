package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.malevolentgods.fourtwenty.domain.college.Study;
import com.malevolentgods.fourtwenty.power.GrowOpBlock;
import com.malevolentgods.fourtwenty.processing.ThinkTankBlock;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The Weed College (specs/017-weed-college; research R11). Its stations are whatever stands inside its area, found by looking: Grow Op
 * plots, a Think Tank and a desk (a lectern), so a player-added one counts too. It keeps the study in progress's supplies and weed seeds
 * for its plots; everything else (harvested buds, seed batches) goes to the warehouse. Research itself lives with the colony
 * ({@link CollegeResearch}), not here.
 */
public final class CollegeBuilding extends AbstractBuilding {
    /** What the College is up to, for its status tab; set by its Weedologists. */
    public enum Activity { IDLE, WAITING, WORKING, PAUSED }

    private static final int SCAN_EVERY = 200;
    private List<BlockPos> plots = List.of();
    private BlockPos tank, desk;
    private long scannedAt = Long.MIN_VALUE;
    private int scannedLevel = -1;
    private Activity activity = Activity.IDLE;
    private String reason = "", reasonArg = "";
    private long studiesFinished;

    public CollegeBuilding(IColony colony, BlockPos pos) { super(colony, pos); }

    @Override public String getSchematicName() { return CollegeBlock.NAME; }
    @Override public int getMaxBuildingLevel() { return 5; }

    public List<BlockPos> plots() { rescan(); return plots; }
    public BlockPos tank() { rescan(); return tank; }
    public BlockPos desk() { rescan(); return desk; }

    private void rescan() {
        Level level = getColony().getWorld();
        long now = level.getGameTime();
        if (now - scannedAt < SCAN_EVERY && scannedLevel == getBuildingLevel()) return;
        scannedAt = now;
        scannedLevel = getBuildingLevel();
        var corners = getCorners();
        List<BlockPos> foundPlots = new ArrayList<>();
        BlockPos foundTank = null, foundDesk = null;
        for (BlockPos pos : BlockPos.betweenClosed(corners.getA(), corners.getB())) {
            if (!level.isLoaded(pos)) continue;
            var block = level.getBlockState(pos).getBlock();
            if (block instanceof GrowOpBlock) foundPlots.add(pos.immutable());
            else if (block instanceof ThinkTankBlock && foundTank == null) foundTank = pos.immutable();
            else if (block instanceof LecternBlock && foundDesk == null) foundDesk = pos.immutable();
        }
        foundPlots.sort(Comparator.comparingDouble(p -> p.distSqr(getPosition())));
        plots = List.copyOf(foundPlots);
        tank = foundTank;
        desk = foundDesk;
    }

    /** Whether {@code stack} is one of {@code supply}'s items (its strain too, when the line names one). */
    public static boolean matches(ItemStack stack, Study.Supply supply) {
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(supply.item())) return false;
        return supply.strain() == null || ModDataComponents.strain(stack).id().equals(supply.strain());
    }

    // ---- status, set by the Weedologist

    public void report(Activity activity, String reason, String arg) {
        this.activity = activity;
        this.reason = reason == null ? "" : reason;
        this.reasonArg = arg == null ? "" : arg;
    }

    public Activity activity() { return activity; }
    public String reason() { return reason; }
    public String reasonArg() { return reasonArg; }
    public long studiesFinished() { return studiesFinished; }

    public void finishedOne() {
        studiesFinished++;
        markDirty();
    }

    /** The study in progress's missing supplies and a seed per plot stay here; the rest is picked up for the warehouse. */
    @Override public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount() {
        var required = super.getRequiredItemsAndAmount();
        required.put(stack -> stack.is(ModItems.WEED_SEEDS.get()), new Tuple<>(Math.max(1, plots().size()) * CollegeConfig.seedsPerPlot(), true));
        if (!getColony().getWorld().isClientSide()) {
            for (Study.Supply s : CollegeResearch.record(getColony()).missing()) {
                required.put(stack -> matches(stack, s), new Tuple<>(s.count(), true));
            }
        }
        return required;
    }

    @Override public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag tag = super.serializeNBT(provider);
        tag.putLong("fourtwentyStudiesFinished", studiesFinished);
        return tag;
    }

    @Override public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag) {
        super.deserializeNBT(provider, tag);
        studiesFinished = tag.getLong("fourtwentyStudiesFinished");
    }
}
