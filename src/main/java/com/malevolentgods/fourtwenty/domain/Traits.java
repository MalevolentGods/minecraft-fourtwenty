package com.malevolentgods.fourtwenty.domain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.DoubleSupplier;

/** What a seed passes on, each rated 1 to 5. A 2 is plain weed: it adds nothing and takes nothing away. */
public record Traits(int potency, int yield, int length) {
    public static final int MIN = 1, MAX = 5;
    public static final Traits DEFAULT = new Traits(2, 2, 2);

    private static final int[] POTENCY_BONUS = {-1, 0, 1, 1, 2};
    private static final int[] YIELD_BONUS = {-1, 0, 1, 2, 3};
    private static final double[] LENGTH_FACTOR = {0.5, 1, 1.5, 2, 3};

    private static final Codec<Integer> RATING = Codec.intRange(MIN, MAX);
    public static final Codec<Traits> CODEC = RecordCodecBuilder.create(i -> i.group(
            RATING.fieldOf("potency").forGetter(Traits::potency),
            RATING.fieldOf("yield").forGetter(Traits::yield),
            RATING.fieldOf("length").forGetter(Traits::length)).apply(i, Traits::new));
    public static final StreamCodec<ByteBuf, Traits> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Traits::potency, ByteBufCodecs.VAR_INT, Traits::yield, ByteBufCodecs.VAR_INT, Traits::length, Traits::new);

    /** Levels added to a smoked effect. */
    public int potencyBonus() { return POTENCY_BONUS[potency - MIN]; }

    /** Rough buds added to a harvest. */
    public int yieldBonus() { return YIELD_BONUS[yield - MIN]; }

    /** How much longer a high lasts. */
    public double lengthFactor() { return LENGTH_FACTOR[length - MIN]; }

    /** A child: each trait the parents' average, then maybe one up or down. {@code rolls} gives uniform values in 0..1. */
    public static Traits cross(Traits a, Traits b, DoubleSupplier rolls) {
        return new Traits(child(a.potency, b.potency, rolls), child(a.yield, b.yield, rolls), child(a.length, b.length, rolls));
    }

    private static int child(int a, int b, DoubleSupplier rolls) {
        int sum = a + b;
        int middle = sum / 2 + (sum % 2 != 0 && rolls.getAsDouble() < 0.5 ? 1 : 0);
        double drift = rolls.getAsDouble();
        return Math.clamp(middle + (drift < 0.25 ? -1 : drift >= 0.75 ? 1 : 0), MIN, MAX);
    }
}
