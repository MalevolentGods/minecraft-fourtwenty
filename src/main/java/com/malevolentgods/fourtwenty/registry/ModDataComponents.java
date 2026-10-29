package com.malevolentgods.fourtwenty.registry;

import com.malevolentgods.fourtwenty.domain.Strain;
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

/** Item data components. A stack without a strain is Landrace. */
public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "fourtwenty");

    /** The strain of seeds, buds and joints. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Strain>> STRAIN = COMPONENTS.register("strain",
            () -> DataComponentType.<Strain>builder().persistent(Strain.CODEC).networkSynchronized(Strain.STREAM_CODEC).build());

    /** What a pipe is loaded with: buds of one strain, used one per hit. */
    public record PipeLoad(Strain strain, int buds) {
        public static final int CAPACITY = 16;
        public static final Codec<PipeLoad> CODEC = RecordCodecBuilder.create(i -> i.group(
                Strain.CODEC.fieldOf("strain").forGetter(PipeLoad::strain),
                Codec.intRange(0, CAPACITY).fieldOf("buds").forGetter(PipeLoad::buds)).apply(i, PipeLoad::new));
        public static final StreamCodec<ByteBuf, PipeLoad> STREAM_CODEC = StreamCodec.composite(
                Strain.STREAM_CODEC, PipeLoad::strain, ByteBufCodecs.VAR_INT, PipeLoad::buds, PipeLoad::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PipeLoad>> PIPE_LOAD = COMPONENTS.register("pipe_load",
            () -> DataComponentType.<PipeLoad>builder().persistent(PipeLoad.CODEC).networkSynchronized(PipeLoad.STREAM_CODEC).build());

    public static Strain strain(ItemStack stack) {
        Strain s = stack.get(STRAIN.get());
        return s == null ? Strain.LANDRACE : s;
    }

    /** A copy of the stack carrying the strain (Landrace is stored as no component, so plain stacks still stack with it). */
    public static ItemStack withStrain(ItemStack stack, Strain strain) {
        if (strain == Strain.LANDRACE) stack.remove(STRAIN.get());
        else stack.set(STRAIN.get(), strain);
        return stack;
    }
}
