package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.buildings.modules.SettingsModule;
import com.minecolonies.core.colony.buildings.modules.settings.BoolSetting;
import com.minecolonies.core.colony.buildings.modules.settings.SettingKey;
import net.minecraft.resources.ResourceLocation;

/**
 * "Fly when Lifted" on every Builder's hut (specs/013-lifted-builders FR-009, research R5): a MineColonies setting in
 * the hut's own Settings tab, on by default. Added as each Builder's hut is made, before its save loads, so a saved
 * value loads normally and older saves without it read on.
 */
public final class LiftedSetting {
    private LiftedSetting() {}

    public static final ResourceLocation BUILDER = ResourceLocation.fromNamespaceAndPath("minecolonies", "builder");
    public static final SettingKey<BoolSetting> FLY_WHEN_LIFTED =
            new SettingKey<>(BoolSetting.class, ResourceLocation.fromNamespaceAndPath("fourtwenty", "flywhenlifted"));

    /** Adds the switch to a Builder's hut (called as MineColonies makes the building). */
    public static void addTo(IBuilding building) {
        var module = building.getFirstModuleOccurance(SettingsModule.class);
        if (module != null) module.with(FLY_WHEN_LIFTED, new BoolSetting(true));
    }

    /** Whether this hut lets its Builder fly when Lifted (on if the hut has no such setting). */
    public static boolean on(IBuilding hut) {
        if (hut == null) return false;
        var module = hut.getFirstModuleOccurance(SettingsModule.class);
        return module == null || module.getSettingValueOrDefault(FLY_WHEN_LIFTED, true);
    }
}
