package com.malevolentgods.fourtwenty.compat.minecolonies.tiedye;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The Tie Dye Shack's tunable numbers (specs/018-tie-dye-shack research R5; Principle VI). Server config, fourtwenty-tiedye-server.toml. */
public final class TieDyeConfig {
    private TieDyeConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue VAT_TICKS = BUILDER
            .comment("Ticks a Tie Dyer spends dipping one batch at a vat.").defineInRange("vatTicks", 200, 10, 12_000);
    private static final ModConfigSpec.IntValue RACK_TICKS = BUILDER
            .comment("Ticks a batch hangs at a drying rack.").defineInRange("rackTicks", 400, 10, 24_000);
    private static final ModConfigSpec.DoubleValue BATCH_SCALE = BUILDER
            .comment("Multiplies the batch size by level (4, 8, 8, 16, 16).").defineInRange("batchScale", 1.0, 0.05, 8);
    private static final ModConfigSpec.IntValue ITEMS_PER_BUD = BUILDER
            .comment("Items dyed per Cured Bud of each colour in the colourway.").defineInRange("itemsPerBud", 8, 1, 64);
    private static final ModConfigSpec.IntValue WOOL_PER_SHIRT = BUILDER
            .comment("White wool a tie-dyed shirt is made from.").defineInRange("woolPerShirt", 3, 1, 16);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean loaded() { return SPEC.isLoaded(); }

    public static int vatTicks() { return loaded() ? VAT_TICKS.get() : 200; }
    public static int rackTicks() { return loaded() ? RACK_TICKS.get() : 400; }
    public static double batchScale() { return loaded() ? BATCH_SCALE.get() : 1.0; }
    public static int itemsPerBud() { return loaded() ? ITEMS_PER_BUD.get() : 8; }
    public static int woolPerShirt() { return loaded() ? WOOL_PER_SHIRT.get() : 3; }
}
