package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.ProductionRules;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.crafting.*;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.modules.AbstractCraftingBuildingModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

public final class JointCraftingModule extends AbstractCraftingBuildingModule.Crafting {
    public JointCraftingModule() { super(DispensaryRegistry.JOB.get()); }

    /** Only the exact menu recipes are teachable, for items already on this Dispensary's menu. */
    @Override public boolean isRecipeCompatible(IGenericRecipe recipe) {
        if (!super.isRecipeCompatible(recipe) || recipe.getPrimaryOutput().getCount() != 1 || recipe.getGridSize() != 3
                || !recipe.getAdditionalOutputs().isEmpty() || recipe.getLootTable() != null) return false;
        var item = menuItem(recipe.getPrimaryOutput());
        if (item == null || item.level > Math.max(1, building.getBuildingLevel())) return false;
        var inputs = new java.util.HashMap<String, Integer>();
        for (var alternatives : recipe.getInputs()) {
            if (alternatives.isEmpty()) continue;
            if (alternatives.size() != 1) return false;
            var stack = alternatives.getFirst();
            if (stack.isEmpty()) continue;
            inputs.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
        }
        var expected = new java.util.HashMap<String, Integer>();
        for (var input : ProductionRules.recipe(item)) expected.merge(input.itemId(), input.count(), Integer::sum);
        return inputs.equals(expected);
    }

    private static MenuItem menuItem(ItemStack stack) {
        for (var item : MenuItem.values()) if (stack.is(item(item))) return item;
        return null;
    }
    static Item item(MenuItem item) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(item.itemId)); }

    /** Native recipe improvement must never discount the agreed material cost. */
    @Override public void improveRecipe(IRecipeStorage recipe, int count, ICitizenData citizen) {}

    /** Teach every recipe on the current menu; an upgrade adds the new level's items. */
    @Override public void checkForWorkerSpecificRecipes() {
        if (building.getBuildingLevel() < 1) return;
        for (var entry : MenuItem.menu(building.getBuildingLevel())) {
            Item output = item(entry);
            if (getFirstRecipe(stack -> stack.is(output)) != null) continue;
            var inputs = ProductionRules.recipe(entry).stream()
                    .map(input -> new ItemStorage(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(input.itemId())), input.count())))
                    .toList();
            var recipe = RecipeStorage.builder().withGridSize(3).withIntermediate(Blocks.AIR)
                    .withInputs(inputs).withPrimaryOutput(new ItemStack(output)).build();
            addRecipe(IColonyManager.getInstance().getRecipeManager().checkOrAddRecipe(recipe));
        }
    }

    /** One persisted native request tree per menu item supplies ingredients, crafts, and retains the output locally. */
    @Override public void onColonyTick(IColony colony) {
        if (!WorldUtil.isBlockLoaded(colony.getWorld(), building.getPosition())) return;
        super.onColonyTick(colony);
        if (building.getBuildingLevel() < 1 || building.getAllAssignedCitizen().isEmpty() || recipes.isEmpty()) return;
        var open = building.getOpenRequestsByRequestableType().values().stream().flatMap(java.util.Collection::stream)
                .distinct().map(colony.getRequestManager()::getRequestForToken).toList();
        for (var entry : MenuItem.menu(building.getBuildingLevel())) {
            Item output = item(entry);
            if (getFirstRecipe(stack -> stack.is(output)) == null) continue;
            var sample = new ItemStack(output);
            boolean outstanding = open.stream().anyMatch(request -> request != null && request.getRequest() instanceof Stack stack && stack.matches(sample));
            int held = building.getHandlers().stream().mapToInt(handler ->
                    InventoryUtils.getItemCountInItemHandler(handler, stack -> stack.is(output))).sum();
            int count = ProductionRules.batch(entry, building.getBuildingLevel(), held, outstanding);
            if (count > 0) {
                var request = new Stack(new ItemStack(output), count, count);
                // Existing local stock must not satisfy a request for additional stock.
                request.setCanBeResolvedByBuilding(false);
                building.createRequest(request, true);
            }
        }
    }
}
