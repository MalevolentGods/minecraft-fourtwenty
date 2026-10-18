package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;

/** Native request queue, ingredient pickup, work-position navigation, crafting and inventory dumping. */
public final class BudtenderAI extends AbstractEntityAICrafting<BudtenderJob, DispensaryBuilding> {
    private long nextServiceTick;
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> BONG_FUEL = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.parse("fourtwenty:bong_fuel"));
    private final com.malevolentgods.fourtwenty.compat.minecolonies.service.BongTending bongs =
            new com.malevolentgods.fourtwenty.compat.minecolonies.service.BongTending();
    /** Native requests: checks worker and hut stock and open requests before creating one. */
    private final com.malevolentgods.fourtwenty.compat.minecolonies.service.BongTending.Supplies supplies =
            new com.malevolentgods.fourtwenty.compat.minecolonies.service.BongTending.Supplies() {
                @Override public void request(net.minecraft.world.item.ItemStack stack, int count) {
                    checkIfRequestForItemExistOrCreateAsync(stack, count, 1);
                }
                @Override public void requestFuel(int count) { checkIfRequestForTagExistOrCreateAsync(BONG_FUEL, count); }
            };
    public BudtenderAI(BudtenderJob job) { super(job); }
    @Override public Class<DispensaryBuilding> getExpectedBuildingClass() { return DispensaryBuilding.class; }

    /** Called only while native citizen AI allows work; essential tasks still preempt service and production. */
    @Override public void tick() {
        long now = com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData.get(world).time();
        // Keep the hut's bongs set up (and lit while customers are around); queued customers are served first.
        // While walking to or refilling a bong, skip the native AI so the two don't both steer the worker.
        if (currentRecipeStorage == null && worker instanceof com.minecolonies.core.entity.citizen.EntityCitizen tender
                && bongs.tick(tender, building, now, supplies)) return;
        if (currentRecipeStorage == null && now >= nextServiceTick
                && worker instanceof com.minecolonies.core.entity.citizen.EntityCitizen citizen) {
            nextServiceTick = now + 20;
            building.serveCustomer(citizen, now);
        }
        super.tick();
    }

    /** Native completion/pickup may leave a finished batch carried after its request is closed. */
    @Override protected boolean wantInventoryDumped() {
        return super.wantInventoryDumped() || (currentRecipeStorage == null
                && com.minecolonies.api.util.InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(),
                    stack -> stack.is(com.malevolentgods.fourtwenty.registry.ModItems.WEED_JOINT.get())) > 0);
    }

    /** The pinned native dumper discards an insertion remainder; reserve capacity before it extracts anything. */
    @Override protected com.minecolonies.api.colony.buildings.IBuilding getBuildingToDump() {
        return canDumpInventory(worker.getInventoryCitizen(), building.getItemHandlerCap()) ? building : null;
    }

    public static boolean canDumpInventory(net.neoforged.neoforge.items.IItemHandler source,
            net.neoforged.neoforge.items.IItemHandler destination) {
        if (destination == null) return false;
        var preview = new net.neoforged.neoforge.items.ItemStackHandler(destination.getSlots()) {
            @Override public int getSlotLimit(int slot) { return destination.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
                return destination.isItemValid(slot, stack);
            }
        };
        for (int slot = 0; slot < destination.getSlots(); slot++) preview.setStackInSlot(slot, destination.getStackInSlot(slot).copy());
        for (int slot = 0; slot < source.getSlots(); slot++) {
            if (!net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(preview, source.getStackInSlot(slot).copy(), false).isEmpty()) return false;
        }
        return true;
    }

    /** Keep the production request pending until its output has actually reached storage. */
    @Override public com.minecolonies.api.entity.ai.statemachine.states.IAIState afterDump() {
        if (com.minecolonies.api.util.InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(),
                stack -> stack.is(com.malevolentgods.fourtwenty.registry.ModItems.WEED_JOINT.get())) > 0) {
            setDelay(100);
            return com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.INVENTORY_FULL;
        }
        return super.afterDump();
    }

    /** Do not consume ingredients when the native output insertion would have no room. */
    @Override public com.minecolonies.api.entity.ai.statemachine.states.IAIState executeCraftingAction(int toolSlot) {
        if (currentRecipeStorage != null && !net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(
                worker.getInventoryCitizen(), currentRecipeStorage.getPrimaryOutput().copy(), true).isEmpty()) {
            return com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.INVENTORY_FULL;
        }
        return super.executeCraftingAction(toolSlot);
    }
}
