package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.registry.ModStrainData;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nonnull;

/** A written Bit Chit and a blank one make two written ones. */
public class BitChitCopyRecipe extends CustomRecipe {
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<BitChitCopyRecipe>> SERIALIZER =
            ModStrainData.RECIPE_SERIALIZERS.register("bit_chit_copy", () -> new SimpleCraftingRecipeSerializer<>(BitChitCopyRecipe::new));

    public BitChitCopyRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** Loads the class so the serializer is registered with the rest. */
    public static void init() {}

    private static ItemStack[] parts(CraftingInput input) {
        ItemStack written = ItemStack.EMPTY, blank = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (!s.is(ModLights.BIT_CHIT.get())) return null;
            if (BitChitItem.program(s) != null) {
                if (!written.isEmpty()) return null;
                written = s;
            } else {
                if (!blank.isEmpty()) return null;
                blank = s;
            }
        }
        return written.isEmpty() || blank.isEmpty() ? null : new ItemStack[]{written, blank};
    }

    @Override public boolean matches(@Nonnull CraftingInput input, @Nonnull Level level) { return parts(input) != null; }

    @Override public @Nonnull ItemStack assemble(@Nonnull CraftingInput input, @Nonnull HolderLookup.Provider registries) {
        ItemStack[] p = parts(input);
        if (p == null) return ItemStack.EMPTY;
        ItemStack out = BitChitItem.written(BitChitItem.program(p[0]));
        out.setCount(2);
        return out;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override public @Nonnull RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
}
