package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.ConstructionRules;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.malevolentgods.fourtwenty.processing.ConstructionBlockEntity.*;

/**
 * A Construction bench: a crafting grid that makes more of the mod's building blocks, the better the tier. It's the crafting table's own menu
 * underneath, so the recipe book and JEI work as they do there. The result always shows, with its cost; it can be taken only while the bench's
 * reserve covers that cost, and shift-clicking crafts until it doesn't.
 */
public class ConstructionMenu extends CraftingMenu {
    private static final int RESULT = 0;

    private final ContainerLevelAccess access;
    private final @Nullable ConstructionBlockEntity bench;
    private final ContainerData data;
    private final DataSlot cost = DataSlot.standalone();

    public ConstructionMenu(int id, Inventory inventory, ContainerLevelAccess access, @Nullable ConstructionBlockEntity bench, ContainerData data) {
        super(id, inventory, access);
        this.access = access;
        this.bench = bench;
        this.data = data;
        addDataSlots(data);
        addDataSlot(cost);
        Slot old = slots.get(RESULT);
        Slot paid = new ResultSlot(inventory.player, (CraftingContainer) slots.get(RESULT + 1).container, old.container, old.getContainerSlot(), old.x, old.y) {
            @Override public boolean mayPickup(@Nonnull Player player) { return affordable() && super.mayPickup(player); }

            /** Pays the recipe's cost: a shift-click hands this the stack after it has moved, empty. */
            @Override public void onTake(@Nonnull Player player, @Nonnull ItemStack stack) {
                if (bench != null) bench.pay(cost.get());
                super.onTake(player, stack);
            }
        };
        paid.index = RESULT;
        slots.set(RESULT, paid);
    }

    public ConstructionMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL, null, new SimpleContainerData(DATA_COUNT));
    }

    public int stored() { return data.get(DATA_STORED); }

    public int capacity() { return data.get(DATA_CAPACITY); }

    public int factor() { return data.get(DATA_YIELD); }

    public boolean overloaded() { return data.get(DATA_OVERLOADED) != 0; }

    public int state() { return data.get(DATA_STATE); }

    /** BI a second the reserve charges at. */
    public int rate() { return data.get(DATA_RATE); }

    public BIRules.Tier tier() { return BIRules.Tier.values()[Math.floorMod(data.get(DATA_TIER), BIRules.Tier.values().length)]; }

    /** What the recipe in the grid costs; 0 with no recipe. */
    public int cost() { return getSlot(RESULT).hasItem() ? cost.get() : 0; }

    /** Whether the reserve covers the recipe in the grid. Both sides read the same synced numbers. */
    public boolean affordable() { return !overloaded() && stored() >= cost(); }

    /** What a tier's bench makes of a recipe's result. */
    public static ItemStack multiplied(ItemStack result, BIRules.Tier tier) {
        if (!result.is(ModPower.CONSTRUCTION)) return result;
        return result.copyWithCount(ConstructionRules.batch(result.getCount(), result.getMaxStackSize(), tier));
    }

    @Override public @Nonnull MenuType<?> getType() { return ModPower.CONSTRUCTION_MENU.get(); }

    @Override public boolean stillValid(@Nonnull Player player) {
        return bench == null || !bench.isRemoved() && player.distanceToSqr(bench.getBlockPos().getCenter()) <= 64;
    }

    /** Vanilla checks a shift-click's first craft only; this stops it at the first one the reserve can't cover. */
    @Override public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int index) {
        if (index == RESULT && !getSlot(RESULT).mayPickup(player)) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }

    @Override public void slotsChanged(@Nonnull Container container) {
        super.slotsChanged(container);
        if (bench == null) return;
        access.execute((level, pos) -> {
            Slot result = getSlot(RESULT);
            if (result.hasItem()) result.set(multiplied(result.getItem(), bench.tier()));
            cost.set((int) ConstructionRules.cost(result.getItem().getCount()));
            broadcastChanges();
        });
    }
}
