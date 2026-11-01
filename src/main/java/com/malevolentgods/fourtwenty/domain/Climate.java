package com.malevolentgods.fourtwenty.domain;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;
import java.util.Locale;

/** The climates that make biome strains. Landrace grown in one sometimes gives a climate-touched seed, which a Pheno Chamber settles into the climate's strain. */
public enum Climate implements StringRepresentable {
    DESERT(Strain.DUST_DEVIL, "minecraft:sand"),
    SNOW(Strain.SNOWBLIND, "minecraft:snow_block"),
    SWAMP(Strain.BOG_BUTTER, "minecraft:lily_pad"),
    JUNGLE(Strain.CANOPY_CREEPER, "minecraft:jungle_sapling");

    public static final Codec<Climate> CODEC = StringRepresentable.fromEnum(Climate::values);
    public static final StreamCodec<ByteBuf, Climate> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);
    /** Chance per Landrace harvest of a climate-touched seed. A Cosmic Kush Pod's copy of a climate gives half. */
    public static final double CHANCE = 0.10, COPIED = 0.5;

    private final Strain strain;
    private final String sample;

    Climate(Strain strain, String sample) {
        this.strain = strain;
        this.sample = sample;
    }

    public Strain strain() { return strain; }

    public String translationKey() { return "climate.fourtwenty." + getSerializedName(); }

    /** The climate a Cosmic Kush Pod copies from this item's registry id, or null. */
    public static Climate bySample(String itemId) {
        for (Climate c : values()) if (c.sample.equals(itemId)) return c;
        return null;
    }

    @Override public @Nonnull String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
