package com.malevolentgods.fourtwenty.domain;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Weed strains. Landrace is ordinary weed (wild plants, the starting seeds); the other 16 are bred in the Inspiration
 * Infuser, one per terpene accord (hue row), and named after it. A strain travels with seeds, buds, joints and pipe
 * loads as the {@code fourtwenty:strain} component, and a planted crop keeps it in its block state.
 */
public enum Strain implements StringRepresentable {
    LANDRACE("landrace", "Landrace", 0x7DAA3C, Family.LANDRACE),
    COUCHLOCK_HUM("myr_myr", "Couchlock Hum", 0xB8A897, Family.NEUTRALS),
    WAKE_AND_BAKE("myr_lim", "Wake & Bake", 0xE8D7B0, Family.NEUTRALS),
    SPACEBAR_STATIC("myr_pin", "Spacebar Static", 0xC9C9C7, Family.NEUTRALS),
    CLOUD_NINE_DRIFT("myr_lav", "Cloud Nine Drift", 0xB7C0C8, Family.NEUTRALS),
    BROWNIE_BATCH("lim_myr", "Brownie Batch", 0xA0673A, Family.WARM),
    SUNSHINE_DAYDREAM("lim_lim", "Sunshine Daydream", 0xF2C80F, Family.WARM),
    CHERRY_PIE("lim_pin", "Cherry Pie Accord", 0xD7262E, Family.WARM),
    TANGIE_TANGO("lim_lav", "Tangie Tango", 0xF07F1A, Family.WARM),
    MOSSY_PHILOSOPHER("pin_myr", "Mossy Philosopher", 0x8C8C2E, Family.GREEN),
    LIME_IN_THE_COCONUT("pin_lim", "Lime in the Coconut", 0x9BD42A, Family.GREEN),
    STICKY_ICKY("pin_pin", "Sticky Icky Accord", 0x2EAA4A, Family.GREEN),
    MERMAID_HOTBOX("pin_lav", "Mermaid Hotbox", 0x17A398, Family.GREEN),
    PURPLE_HAZE("lav_myr", "Purple Haze", 0x8A3FD1, Family.COOL),
    COTTON_CANDY_KUSH("lav_lim", "Cotton Candy Kush", 0xF05AA0, Family.COOL),
    BLUE_DREAM("lav_pin", "Blue Dream", 0x2F6BE0, Family.COOL),
    THIRD_EYE_TONIC("lav_lav", "Third-Eye Tonic", 0x2BB5E8, Family.COOL);

    /** The Terpinator turns a family's weed into that family's terpenes. */
    public enum Family { LANDRACE, NEUTRALS, WARM, GREEN, COOL }

    public static final Codec<Strain> CODEC = StringRepresentable.fromEnum(Strain::values);
    public static final StreamCodec<ByteBuf, Strain> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    private final String id;
    private final String displayName;
    private final int color;
    private final Family family;

    Strain(String id, String displayName, int color, Family family) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.family = family;
    }

    /** "landrace", or the accord code ("lav_pin"). */
    public String id() { return id; }

    public String displayName() { return displayName; }

    /** The strain's colour (its accord's bright tone): smoke, screen tint and tooltip. */
    public int color() { return color; }

    public Family family() { return family; }

    public String translationKey() { return "strain.fourtwenty." + id; }

    @Override
    public @Nonnull String getSerializedName() { return id; }

    public static Strain byId(String id) {
        for (Strain s : values()) if (s.id.equals(id.toLowerCase(Locale.ROOT))) return s;
        return LANDRACE;
    }
}
