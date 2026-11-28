package com.malevolentgods.fourtwenty.registry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nonnull;
import java.util.function.ToDoubleFunction;

/**
 * The Weed College's Botany yield studies (specs/017-weed-college FR-022, research R8): a ripe weed crop harvested for a colony with Soil
 * Science, Selective Breeding or Master Grower can give one extra rough bud, of the crop's strain. Registered always, so the data that names
 * it loads with or without MineColonies; the chance comes from {@link #bonusChance}, which the MineColonies integration sets and which is
 * otherwise 0, so without MineColonies this changes nothing.
 */
public final class FarmYieldModifier extends LootModifier {
    public static final MapCodec<FarmYieldModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).apply(i, FarmYieldModifier::new));
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "fourtwenty");
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<FarmYieldModifier>> FARM_YIELD =
            SERIALIZERS.register("college_farm_yield", () -> CODEC);

    /** The chance (0 to 1) of an extra bud for this harvest; 0 unless a colony's farmer is harvesting for a colony that has studied it. */
    public static volatile ToDoubleFunction<LootContext> bonusChance = context -> 0;

    public FarmYieldModifier(LootItemCondition[] conditions) { super(conditions); }

    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }

    @Override protected @Nonnull ObjectArrayList<ItemStack> doApply(@Nonnull ObjectArrayList<ItemStack> loot, @Nonnull LootContext context) {
        ItemStack bud = null;
        for (ItemStack s : loot) {
            if (s.is(ModItems.ROUGH_BUD.get())) { bud = s; break; }
        }
        if (bud == null) return loot;   // not ripe: no bud, no bonus
        double chance = bonusChance.applyAsDouble(context);
        if (chance > 0 && context.getRandom().nextDouble() < chance) loot.add(bud.copyWithCount(1));
        return loot;
    }

    @Override public @Nonnull MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
