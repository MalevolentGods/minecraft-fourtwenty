package com.malevolentgods.fourtwenty.paving;

import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModStrainData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nonnull;

/** A shapeless recipe that keeps a Pave Pen's settings, charge and paid-for materials on the pen it makes. */
public class PenUpgradeRecipe extends ShapelessRecipe {
    private final ItemStack output;

    public PenUpgradeRecipe(String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
        super(group, category, result, ingredients);
        this.output = result;
    }

    @Override public @Nonnull ItemStack assemble(@Nonnull CraftingInput input, @Nonnull HolderLookup.Provider registries) {
        ItemStack out = super.assemble(input, registries);
        for (int i = 0; i < input.size(); i++) {
            ItemStack in = input.getItem(i);
            if (in.getItem() instanceof PavePenItem && in.has(ModDataComponents.PEN.get())) {
                PavePenItem.set(out, PavePenItem.state(in));
                break;
            }
        }
        return out;
    }

    @Override public @Nonnull RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }

    public static class Serializer implements RecipeSerializer<PenUpgradeRecipe> {
        private static final MapCodec<PenUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.output),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(list -> {
                    Ingredient[] all = list.toArray(Ingredient[]::new);
                    if (all.length == 0) return DataResult.error(() -> "No ingredients");
                    if (all.length > 9) return DataResult.error(() -> "Too many ingredients");
                    return DataResult.success(NonNullList.of(Ingredient.EMPTY, all));
                }, DataResult::success).forGetter(ShapelessRecipe::getIngredients)).apply(i, PenUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PenUpgradeRecipe> STREAM = StreamCodec.of((buf, r) -> {
            buf.writeUtf(r.getGroup());
            buf.writeEnum(r.category());
            buf.writeVarInt(r.getIngredients().size());
            for (Ingredient ingredient : r.getIngredients()) Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            ItemStack.STREAM_CODEC.encode(buf, r.output);
        }, buf -> {
            String group = buf.readUtf();
            CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
            NonNullList<Ingredient> ingredients = NonNullList.withSize(buf.readVarInt(), Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new PenUpgradeRecipe(group, category, ItemStack.STREAM_CODEC.decode(buf), ingredients);
        });

        @Override public @Nonnull MapCodec<PenUpgradeRecipe> codec() { return CODEC; }

        @Override public @Nonnull StreamCodec<RegistryFriendlyByteBuf, PenUpgradeRecipe> streamCodec() { return STREAM; }
    }

    public static final DeferredHolder<RecipeSerializer<?>, Serializer> SERIALIZER =
            ModStrainData.RECIPE_SERIALIZERS.register("pen_upgrade", Serializer::new);

    /** Loads the class so the serializer is registered with the rest. */
    public static void init() {}
}
