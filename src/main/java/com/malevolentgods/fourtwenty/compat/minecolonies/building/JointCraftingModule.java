package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

public final class JointCraftingModule extends AbstractCraftingBuildingModule.Crafting {
    public JointCraftingModule() { super(DispensaryRegistry.JOB.get()); }

    /** Only the exact existing recipe is teachable; native large crafting is available at level one. */
    @Override public boolean isRecipeCompatible(IGenericRecipe recipe) {
        if (!super.isRecipeCompatible(recipe) || !recipe.getPrimaryOutput().is(ModItems.WEED_JOINT.get())
                || recipe.getPrimaryOutput().getCount() != 1 || recipe.getGridSize() != 3
                || !recipe.getAdditionalOutputs().isEmpty() || recipe.getLootTable() != null) return false;
        int buds = 0, paper = 0;
        for (var alternatives : recipe.getInputs()) {
            if (alternatives.isEmpty()) continue;
            if (alternatives.size() != 1) return false;
            var stack = alternatives.getFirst();
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.WEED_BUD.get())) buds += stack.getCount();
            else if (stack.is(Items.PAPER)) paper += stack.getCount();
            else return false;
        }
        return buds == 1 && paper == 2;
    }

    /** Native recipe improvement must never discount the agreed material cost. */
    @Override public void improveRecipe(IRecipeStorage recipe, int count, ICitizenData citizen) {}

    @Override public void checkForWorkerSpecificRecipes() {
        if (building.getBuildingLevel() < 1 || !recipes.isEmpty()) return;
        var source = building.getColony().getWorld().getRecipeManager().byKey(ResourceLocation.parse("fourtwenty:weed_joint"));
        if (source.isEmpty() || !isRecipeCompatible(GenericRecipe.of(source.get(), building.getColony().getWorld()))) return;
        var recipe = RecipeStorage.builder().withGridSize(3).withIntermediate(Blocks.AIR)
                .withInputs(List.of(new ItemStorage(new ItemStack(Items.PAPER)), new ItemStorage(new ItemStack(ModItems.WEED_BUD.get())),
                        new ItemStorage(new ItemStack(Items.PAPER))))
                .withPrimaryOutput(new ItemStack(ModItems.WEED_JOINT.get())).build();
        addRecipe(IColonyManager.getInstance().getRecipeManager().checkOrAddRecipe(recipe));
    }

    /** One persisted native request tree per hut supplies ingredients, crafts, and retains the output locally. */
    @Override public void onColonyTick(IColony colony) {
        if (!WorldUtil.isBlockLoaded(colony.getWorld(), building.getPosition())) return;
        super.onColonyTick(colony);
        if (building.getBuildingLevel() < 1 || building.getAllAssignedCitizen().isEmpty() || recipes.isEmpty()) return;
        boolean outstanding = building.getOpenRequestsByRequestableType().values().stream().flatMap(java.util.Collection::stream)
                .distinct().map(colony.getRequestManager()::getRequestForToken)
                .anyMatch(request -> request != null && request.getRequest() instanceof Stack stack && stack.matches(new ItemStack(ModItems.WEED_JOINT.get())));
        int held = building.getHandlers().stream().mapToInt(handler ->
                InventoryUtils.getItemCountInItemHandler(handler, stack -> stack.is(ModItems.WEED_JOINT.get()))).sum();
        int count = ProductionRules.batch(building.getBuildingLevel(), held, outstanding);
        if (count > 0) {
            var request = new Stack(new ItemStack(ModItems.WEED_JOINT.get()), count, count);
            // Existing local stock must not satisfy a request for additional stock.
            request.setCanBeResolvedByBuilding(false);
            building.createRequest(request, true);
        }
    }
}
