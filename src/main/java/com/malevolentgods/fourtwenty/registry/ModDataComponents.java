package com.malevolentgods.fourtwenty.registry;

import com.malevolentgods.fourtwenty.domain.Climate;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Traits;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components. A stack without a strain is Landrace, and one without traits is rated 2 in all of them. */
public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "fourtwenty");

    /** The strain of seeds, buds and joints. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Strain>> STRAIN = COMPONENTS.register("strain",
            () -> DataComponentType.<Strain>builder().persistent(Strain.CODEC).networkSynchronized(Strain.STREAM_CODEC).build());

    /** The traits of seeds, buds and joints. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Traits>> TRAITS = COMPONENTS.register("traits",
            () -> DataComponentType.<Traits>builder().persistent(Traits.CODEC).networkSynchronized(Traits.STREAM_CODEC).build());

    /** On a Landrace seed: the climate it picked up, which a Pheno Chamber settles into a biome strain. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Climate>> CLIMATE = COMPONENTS.register("climate",
            () -> DataComponentType.<Climate>builder().persistent(Climate.CODEC).networkSynchronized(Climate.STREAM_CODEC).build());

    /** What a pipe is loaded with: buds of one strain, used one per hit. */
    public record PipeLoad(Strain strain, int buds, Traits traits) {
        public static final int CAPACITY = 16;
        public static final Codec<PipeLoad> CODEC = RecordCodecBuilder.create(i -> i.group(
                Strain.CODEC.fieldOf("strain").forGetter(PipeLoad::strain),
                Codec.intRange(0, CAPACITY).fieldOf("buds").forGetter(PipeLoad::buds),
                Traits.CODEC.optionalFieldOf("traits", Traits.DEFAULT).forGetter(PipeLoad::traits)).apply(i, PipeLoad::new));
        public static final StreamCodec<ByteBuf, PipeLoad> STREAM_CODEC = StreamCodec.composite(
                Strain.STREAM_CODEC, PipeLoad::strain, ByteBufCodecs.VAR_INT, PipeLoad::buds, Traits.STREAM_CODEC, PipeLoad::traits, PipeLoad::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PipeLoad>> PIPE_LOAD = COMPONENTS.register("pipe_load",
            () -> DataComponentType.<PipeLoad>builder().persistent(PipeLoad.CODEC).networkSynchronized(PipeLoad.STREAM_CODEC).build());

    /** A Kush Suit piece, the Blazer or a Dab Cell: its BI, the terpene colours in its slots (by code), and the Blazer's mode. */
    public record GearState(int charge, java.util.List<String> terps, int mode) {
        public static final GearState EMPTY = new GearState(0, java.util.List.of(), 0);
        public static final Codec<GearState> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("charge", 0).forGetter(GearState::charge),
                Codec.STRING.listOf().optionalFieldOf("terps", java.util.List.of()).forGetter(GearState::terps),
                Codec.INT.optionalFieldOf("mode", 0).forGetter(GearState::mode)).apply(i, GearState::new));
        public static final StreamCodec<ByteBuf, GearState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, GearState::charge, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)), GearState::terps,
                ByteBufCodecs.VAR_INT, GearState::mode, GearState::new);

        public GearState { terps = java.util.List.copyOf(terps); }

        public GearState withCharge(int value) { return new GearState(value, terps, mode); }

        public GearState withTerps(java.util.List<String> value) { return new GearState(charge, value, mode); }

        public GearState withMode(int value) { return new GearState(charge, terps, value); }
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GearState>> GEAR = COMPONENTS.register("gear",
            () -> DataComponentType.<GearState>builder().persistent(GearState.CODEC).networkSynchronized(GearState.STREAM_CODEC).build());

    public static Strain strain(ItemStack stack) {
        Strain s = stack.get(STRAIN.get());
        return s == null ? Strain.LANDRACE : s;
    }

    public static Traits traits(ItemStack stack) {
        Traits t = stack.get(TRAITS.get());
        return t == null ? Traits.DEFAULT : t;
    }

    /** The stack carrying the traits (rated 2 is stored as no component, so plain stacks still stack with it). */
    public static ItemStack withTraits(ItemStack stack, Traits traits) {
        if (traits.equals(Traits.DEFAULT)) stack.remove(TRAITS.get());
        else stack.set(TRAITS.get(), traits);
        return stack;
    }

    /** The stack bred like {@code parent}: same strain, same traits. */
    public static ItemStack bredFrom(ItemStack stack, ItemStack parent) {
        return withTraits(withStrain(stack, strain(parent)), traits(parent));
    }

    /** A copy of the stack carrying the strain (Landrace is stored as no component, so plain stacks still stack with it). */
    public static ItemStack withStrain(ItemStack stack, Strain strain) {
        if (strain == Strain.LANDRACE) stack.remove(STRAIN.get());
        else stack.set(STRAIN.get(), strain);
        return stack;
    }
}
