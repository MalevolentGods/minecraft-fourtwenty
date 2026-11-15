package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Each player's own Midnight Machine volume and mute (fourtwenty-midnight-machine-client.toml). Only config values,
 * nothing from the game client: the mod registers it on both sides, and a dedicated server that loaded the pause-menu
 * controls with it would not start (the colony game tests caught it, 2026-11-14). {@link MidnightMachineSettings} has
 * the controls.
 */
public final class MidnightMachineClientConfig {
    private MidnightMachineClientConfig() {}

    public static final ModConfigSpec SPEC;
    static final ModConfigSpec.DoubleValue VOLUME;
    static final ModConfigSpec.BooleanValue MUTED;

    static {
        var b = new ModConfigSpec.Builder();
        b.push("midnightMachine");
        VOLUME = b.comment("The Midnight Machine's music, 0 to 1, on top of the Jukebox/Note Blocks slider.").defineInRange("volume", 1.0, 0, 1);
        MUTED = b.comment("Mute the Midnight Machine for you (the boombox and dancing stay).").define("muted", false);
        b.pop();
        SPEC = b.build();
    }

    /** The gain to apply to her music: 0 when muted, 1 before the config has loaded. */
    public static double gain() {
        try {
            return MUTED.get() ? 0 : VOLUME.get();
        } catch (IllegalStateException notLoaded) {
            return 1;
        }
    }
}
