package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.ProductionRules;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Map;
import java.util.function.Predicate;

public final class DispensaryBuilding extends AbstractBuilding {
    public DispensaryBuilding(IColony colony, BlockPos pos) { super(colony, pos); }
    @Override public String getSchematicName() { return "dispensary"; }
    @Override public int getMaxBuildingLevel() { return 5; }
    public int stockTarget() { return ProductionRules.stockTarget(getBuildingLevel()); }

    /** Retain the service target and one production batch; workers still dump their output normally. */
    @Override public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount() {
        var required = super.getRequiredItemsAndAmount();
        retain(required, ModItems.WEED_JOINT.get(), stockTarget());
        retain(required, ModItems.WEED_BUD.get(), getBuildingLevel() > 0 ? 16 : 0);
        retain(required, Items.PAPER, getBuildingLevel() > 0 ? 32 : 0);
        return required;
    }
    private static void retain(Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> required, Item item, int minimum) {
        // Merge native request reservations and user minimum stock, avoiding competing predicate order.
        int amount = minimum;
        boolean workerInventory = false;
        var sample = new ItemStack(item);
        var iterator = required.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().test(sample)) {
                amount = Math.max(amount, entry.getValue().getA());
                workerInventory |= entry.getValue().getB();
                iterator.remove();
            }
        }
        required.put(stack -> stack.is(item), new Tuple<>(amount, workerInventory));
    }
}
