package com.malevolentgods.fourtwenty.compat.jei;

import com.malevolentgods.fourtwenty.processing.ConstructionMenu;
import com.malevolentgods.fourtwenty.registry.ModPower;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/** Lets JEI fill a Construction bench's grid as it does a crafting table's. JEI finds this itself; nothing else touches this class, so the mod runs without JEI. */
@JeiPlugin
public class FourTwentyJeiPlugin implements IModPlugin {
    private static final int GRID_START = 1, GRID_SLOTS = 9, INVENTORY_START = 10, INVENTORY_SLOTS = 36;

    @Override public @Nonnull ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("fourtwenty", "jei"); }

    @Override public void registerRecipeTransferHandlers(@Nonnull IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(ConstructionMenu.class, ModPower.CONSTRUCTION_MENU.get(), RecipeTypes.CRAFTING, GRID_START, GRID_SLOTS, INVENTORY_START, INVENTORY_SLOTS);
    }

    @Override public void registerRecipeCatalysts(@Nonnull IRecipeCatalystRegistration registration) {
        ModPower.CONSTRUCTION_BENCHES.forEach(bench -> registration.addRecipeCatalyst(new ItemStack(bench.get()), RecipeTypes.CRAFTING));
    }
}
