package com.malevolentgods.fourtwenty.util;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-player settings, for anyone whose machine would rather not. */
public final class ClientConfig {
    private ClientConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue COLOURED_LIGHT = BUILDER.comment("Lit lamps tint the surfaces they reach.").define("colouredLight", true);
    public static final ModConfigSpec SPEC = BUILDER.build();
}
