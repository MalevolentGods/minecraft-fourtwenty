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
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.malevolentgods.fourtwenty.processing.ConstructionBlockEntity.*;

/**
 * A Construction bench: a crafting grid that needs power, and makes more of the mod's building blocks, the better the tier. It's the crafting table's own menu
 * underneath, so the recipe book and JEI work as they do there. With no power there's no result.
 */
public class ConstructionMenu extends CraftingMenu {
    private final ContainerLevelAccess access;
    private final @Nullable ConstructionBlockEntity bench;
    private final ContainerData data;
    private boolean wasReady;

    public ConstructionMenu(int id, Inventory inventory, ContainerLevelAccess access, @Nullable ConstructionBlockEntity bench, ContainerData data) {
        super(id, inventory, access);
        this.access = access;
        this.bench = bench;
        this.data = data;
        addDataSlots(data);
        Slot old = slots.get(RESULT);
        Slot paid = new ResultSlot(inventory.player, (CraftingContainer) slots.get(RESULT + 1).container, old.container, old.getContainerSlot(), old.x, old.y) {
            @Override public void onTake(@Nonnull Player player, @Nonnull ItemStack stack) {
                if (bench != null) bench.pay();
                super.onTake(player, stack);
            }
        };
        paid.index = RESULT;
        slots.set(RESULT, paid);
    }

    public ConstructionMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL, null, new SimpleContainerData(DATA_COUNT));
    }

    private static final int RESULT = 0;

    public boolean ready() { return data.get(DATA_READY) != 0; }

    public int factor() { return data.get(DATA_YIELD); }

    public boolean overloaded() { return data.get(DATA_OVERLOADED) != 0; }

    /** What a tier's bench makes of a recipe's result. */
    public static ItemStack multiplied(ItemStack result, BIRules.Tier tier) {
        if (!result.is(ModPower.CONSTRUCTION)) return result;
        return result.copyWithCount(ConstructionRules.batch(result.getCount(), result.getMaxStackSize(), tier));
    }

    @Override public @Nonnull MenuType<?> getType() { return ModPower.CONSTRUCTION_MENU.get(); }

    @Override public boolean stillValid(@Nonnull Player player) {
        return bench == null || !bench.isRemoved() && player.distanceToSqr(bench.getBlockPos().getCenter()) <= 64;
    }

    /** Power can come and go with a recipe sitting in the grid. */
    @Override public void broadcastChanges() {
        if (bench != null && bench.ready() != wasReady) {
            wasReady = bench.ready();
            slotsChanged(getSlot(RESULT + 1).container);
        }
        super.broadcastChanges();
    }

    @Override public void slotsChanged(@Nonnull Container container) {
        super.slotsChanged(container);
        if (bench == null) return;
        access.execute((level, pos) -> {
            Slot result = getSlot(RESULT);
            if (!result.hasItem()) return;
            result.set(bench.ready() ? multiplied(result.getItem(), bench.tier()) : ItemStack.EMPTY);
            broadcastChanges();
        });
    }
}
