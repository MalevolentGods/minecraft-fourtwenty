package com.malevolentgods.fourtwenty.domain;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Weed strains. Landrace is ordinary weed (wild plants, the starting seeds); 16 are bred in the Inspiration Infuser, one
 * per terpene accord (hue row), and named after it. Above those is the ladder: biome landraces, then hybrids of rising
 * grade from the Pheno Chamber, up to Golden God. A strain travels with seeds, buds, joints and pipe loads as the
 * {@code fourtwenty:strain} component, and a planted crop keeps it in its block state. New strains go on the end: item
 * models pick their art by ordinal.
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
    THIRD_EYE_TONIC("lav_lav", "Third-Eye Tonic", 0x2BB5E8, Family.COOL),
    DUST_DEVIL("dust_devil", "Dust Devil", 0xD9B36A, Family.BIOME),
    SNOWBLIND("snowblind", "Snowblind", 0xBFE6F5, Family.BIOME),
    BOG_BUTTER("bog_butter", "Bog Butter", 0x5C7A3A, Family.BIOME),
    CANOPY_CREEPER("canopy_creeper", "Canopy Creeper", 0x1F9E4B, Family.BIOME),
    TROPICAL_THUNDER("tropical_thunder", "Tropical Thunder", 0x00C2A8, Family.CROSS),
    RAZZLE_DAZZLE("razzle_dazzle", "Razzle Dazzle", 0xFF3D81, Family.CROSS),
    ALARM_CLOCK_ASSASSIN("alarm_clock_assassin", "Alarm Clock Assassin", 0xFFB000, Family.CROSS),
    COUCH_FORT("couch_fort", "Couch Fort", 0x7A5230, Family.CROSS),
    MOSS_BOSS("moss_boss", "Moss Boss", 0x4F7F1F, Family.CROSS),
    DEEP_THOUGHTS("deep_thoughts", "Deep Thoughts", 0x3D5AFE, Family.CROSS),
    LUCID_LAGOON("lucid_lagoon", "Lucid Lagoon", 0x6FA8FF, Family.CROSS),
    ROCKET_LIME("rocket_lime", "Rocket Lime", 0xC6FF00, Family.CROSS),
    HURRICANE_HAZE("hurricane_haze", "Hurricane Haze", 0x5E8CA8, Family.DOUBLE_CROSS),
    SLEDGEHAMMER_SERENADE("sledgehammer_serenade", "Sledgehammer Serenade", 0xB0122B, Family.DOUBLE_CROSS),
    CAFFEINE_COMET("caffeine_comet", "Caffeine Comet", 0xFF6A00, Family.DOUBLE_CROSS),
    OLD_GROWTH("old_growth", "Old Growth", 0x2E5B2E, Family.DOUBLE_CROSS),
    ORACLE_OF_SNACKS("oracle_of_snacks", "Oracle of Snacks", 0xB388FF, Family.DOUBLE_CROSS),
    POLAR_VORTEX("polar_vortex", "Polar Vortex", 0x8FE3FF, Family.DOUBLE_CROSS),
    COSMIC_COUCH("cosmic_couch", "Cosmic Couch", 0x5B2A9D, Family.ULTRA),
    STORM_SHEPHERD("storm_shepherd", "Storm Shepherd", 0x00E5FF, Family.ULTRA),
    THE_ELDER_BUD("the_elder_bud", "The Elder Bud", 0x9E7C3C, Family.ULTRA),
    GOLDEN_GOD("golden_god", "Golden God", 0xFFD700, Family.GOLDEN);

    /** The Terpinator turns a family's weed into that family's terpenes. Grade is its rung on the ladder. */
    public enum Family {
        LANDRACE(0), NEUTRALS(1), WARM(1), GREEN(1), COOL(1), BIOME(1), CROSS(2), DOUBLE_CROSS(3), ULTRA(4), GOLDEN(5);

        private final int grade;

        Family(int grade) { this.grade = grade; }

        public int grade() { return grade; }
    }

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

    public int grade() { return family.grade(); }

    public String translationKey() { return "strain.fourtwenty." + id; }

    @Override
    public @Nonnull String getSerializedName() { return id; }

    public static Strain byId(String id) {
        for (Strain s : values()) if (s.id.equals(id.toLowerCase(Locale.ROOT))) return s;
        return LANDRACE;
    }
}
