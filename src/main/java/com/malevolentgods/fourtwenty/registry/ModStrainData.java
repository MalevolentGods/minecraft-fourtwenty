package com.malevolentgods.fourtwenty.registry;

import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.block.WeedCropBlockEntity;
import com.malevolentgods.fourtwenty.domain.Traits;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
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
            if (state == null || !state.hasProperty(WeedCropBlock.STRAIN)) return stack;
            Traits traits = context.getParamOrNull(LootContextParams.BLOCK_ENTITY) instanceof WeedCropBlockEntity crop ? crop.traits() : Traits.DEFAULT;
            // a harvest's rough buds: the strain family's range (specs/012-progression-balance), plus the yield trait, at least 1
            if (stack.is(ModItems.ROUGH_BUD.get())) {
                stack.setCount(com.malevolentgods.fourtwenty.processing.ProcessingConfig.strainStats()
                        .rollYield(state.getValue(WeedCropBlock.STRAIN).family(), traits, context.getRandom().nextDouble()));
            }
            return ModDataComponents.withTraits(ModDataComponents.withStrain(stack, state.getValue(WeedCropBlock.STRAIN)), traits);
        }

        @Override
        public @Nonnull LootItemFunctionType<CopyStrain> getType() { return COPY_STRAIN.get(); }
    }

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<CopyStrain>> COPY_STRAIN =
            LOOT_FUNCTIONS.register("copy_strain", () -> new LootItemFunctionType<>(CopyStrain.CODEC));

    // ---- crafting: a shaped recipe whose result takes the strain of the strained ingredient (the bud in a joint)

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "fourtwenty");

    /** A real ShapedRecipe: JEI and other mods read the shape from that class, and show anything else as shapeless, in a layout that wouldn't craft. */
    public static class StrainShaped extends ShapedRecipe {
        final ShapedRecipePattern shape;
        final ItemStack output;

        StrainShaped(String group, CraftingBookCategory category, ShapedRecipePattern shape, ItemStack output, boolean notify) {
            super(group, category, shape, output, notify);
            this.shape = shape;
            this.output = output;
        }

        @Override public boolean matches(@Nonnull CraftingInput input, @Nonnull Level level) { return super.matches(input, level) && oneStrain(input); }

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
            ItemStack out = super.assemble(input, registries);
            for (int i = 0; i < input.size(); i++) {
                ItemStack s = input.getItem(i);
                if (s.is(ModItems.WEED_BUD.get())) return ModDataComponents.bredFrom(out, s);
            }
            return out;
        }

        @Override public @Nonnull RecipeSerializer<?> getSerializer() { return STRAIN_SHAPED.get(); }
    }

    public static class StrainShapedSerializer implements RecipeSerializer<StrainShaped> {
        private static final MapCodec<StrainShaped> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.shape),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.output),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification)).apply(i, StrainShaped::new));
        /** The same bytes as a plain shaped recipe. */
        private static final StreamCodec<RegistryFriendlyByteBuf, StrainShaped> STREAM = StreamCodec.of((buf, r) -> {
            buf.writeUtf(r.getGroup());
            buf.writeEnum(r.category());
            ShapedRecipePattern.STREAM_CODEC.encode(buf, r.shape);
            ItemStack.STREAM_CODEC.encode(buf, r.output);
            buf.writeBoolean(r.showNotification());
        }, buf -> new StrainShaped(buf.readUtf(), buf.readEnum(CraftingBookCategory.class), ShapedRecipePattern.STREAM_CODEC.decode(buf),
                ItemStack.STREAM_CODEC.decode(buf), buf.readBoolean()));

        @Override public @Nonnull MapCodec<StrainShaped> codec() { return CODEC; }
        @Override public @Nonnull StreamCodec<RegistryFriendlyByteBuf, StrainShaped> streamCodec() { return STREAM; }
    }

    public static final DeferredHolder<RecipeSerializer<?>, StrainShapedSerializer> STRAIN_SHAPED =
            RECIPE_SERIALIZERS.register("strain_shaped", StrainShapedSerializer::new);
}
