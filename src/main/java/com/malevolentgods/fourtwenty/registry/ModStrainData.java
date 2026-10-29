package com.malevolentgods.fourtwenty.registry;

import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nonnull;
import java.util.List;

/** How strains survive harvests (a loot function) and crafting (a shaped recipe that passes the strain on). */
public final class ModStrainData {
    private ModStrainData() {}

    // ---- loot: drops of a weed crop take the crop's strain

    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, "fourtwenty");

    public static class CopyStrain extends LootItemConditionalFunction {
        public static final MapCodec<CopyStrain> CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).apply(i, CopyStrain::new));

        protected CopyStrain(List<LootItemCondition> conditions) { super(conditions); }

        @Override
        protected @Nonnull ItemStack run(@Nonnull ItemStack stack, @Nonnull LootContext context) {
            BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
            if (state != null && state.hasProperty(WeedCropBlock.STRAIN)) ModDataComponents.withStrain(stack, state.getValue(WeedCropBlock.STRAIN));
            return stack;
        }

        @Override
        public @Nonnull LootItemFunctionType<CopyStrain> getType() { return COPY_STRAIN.get(); }
    }

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<CopyStrain>> COPY_STRAIN =
            LOOT_FUNCTIONS.register("copy_strain", () -> new LootItemFunctionType<>(CopyStrain.CODEC));

    // ---- crafting: a shaped recipe whose result takes the strain of the strained ingredient (the bud in a joint)

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "fourtwenty");

    public static class StrainShaped implements CraftingRecipe {
        final ShapedRecipe inner;

        StrainShaped(ShapedRecipe inner) { this.inner = inner; }

        @Override public boolean matches(@Nonnull CraftingInput input, @Nonnull Level level) { return inner.matches(input, level) && oneStrain(input); }

        /** Buds of two different strains don't make one joint. */
        private static boolean oneStrain(CraftingInput input) {
            Strain seen = null;
            for (int i = 0; i < input.size(); i++) {
                ItemStack s = input.getItem(i);
                if (!s.is(ModItems.WEED_BUD.get())) continue;
                Strain st = ModDataComponents.strain(s);
                if (seen != null && seen != st) return false;
                seen = st;
            }
            return true;
        }

        @Override
        public @Nonnull ItemStack assemble(@Nonnull CraftingInput input, @Nonnull HolderLookup.Provider registries) {
            ItemStack out = inner.assemble(input, registries);
            for (int i = 0; i < input.size(); i++) {
                ItemStack s = input.getItem(i);
                if (s.is(ModItems.WEED_BUD.get())) return ModDataComponents.withStrain(out, ModDataComponents.strain(s));
            }
            return out;
        }

        @Override public boolean canCraftInDimensions(int w, int h) { return inner.canCraftInDimensions(w, h); }
        @Override public @Nonnull ItemStack getResultItem(@Nonnull HolderLookup.Provider registries) { return inner.getResultItem(registries); }
        @Override public @Nonnull NonNullList<Ingredient> getIngredients() { return inner.getIngredients(); }
        @Override public @Nonnull String getGroup() { return inner.getGroup(); }
        @Override public @Nonnull CraftingBookCategory category() { return inner.category(); }
        @Override public boolean showNotification() { return inner.showNotification(); }
        @Override public @Nonnull RecipeSerializer<?> getSerializer() { return STRAIN_SHAPED.get(); }
    }

    public static class StrainShapedSerializer implements RecipeSerializer<StrainShaped> {
        private static final MapCodec<StrainShaped> CODEC = ShapedRecipe.Serializer.CODEC.xmap(StrainShaped::new, r -> r.inner);
        private static final StreamCodec<RegistryFriendlyByteBuf, StrainShaped> STREAM = ShapedRecipe.Serializer.STREAM_CODEC.map(StrainShaped::new, r -> r.inner);

        @Override public @Nonnull MapCodec<StrainShaped> codec() { return CODEC; }
        @Override public @Nonnull StreamCodec<RegistryFriendlyByteBuf, StrainShaped> streamCodec() { return STREAM; }
    }

    public static final DeferredHolder<RecipeSerializer<?>, StrainShapedSerializer> STRAIN_SHAPED =
            RECIPE_SERIALIZERS.register("strain_shaped", StrainShapedSerializer::new);
}
