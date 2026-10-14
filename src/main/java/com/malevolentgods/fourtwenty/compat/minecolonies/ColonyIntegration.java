package com.malevolentgods.fourtwenty.compat.minecolonies;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.DemandRules;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Loaded only after the core entrypoint checks MineColonies presence. No client-only references. */
public final class ColonyIntegration {
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, DispensaryConfig.SPEC, "fourtwenty-dispensary-server.toml");
        NeoForge.EVENT_BUS.addListener(ColonyIntegration::tick);
        com.mojang.logging.LogUtils.getLogger().info("FourTwenty dispensary foundation enabled (MineColonies 1.1.1343)");
    }
    /** Guard housing is the native home assignment, including work-at-home guard quarters. Never use workplace fallback. */
    public static boolean eligible(ICitizenData citizen) {
        var home = citizen.getHomeBuilding();
        return DemandRules.eligible(!citizen.isChild(), home != null, home == null ? 0 : home.getBuildingLevel());
    }
    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.tickRateManager().runsNormally()) return;
        var data = DispensarySavedData.get(level);
        data.advance();
        var rules = DispensaryConfig.rules();
        for (var colony : IColonyManager.getInstance().getColonies(level)) {
            for (var citizen : colony.getCitizenManager().getCitizens()) {
                data.observe(colony.getID(), citizen.getUUID(), eligible(citizen), rules);
            }
        }
    }
    private ColonyIntegration() {}
}
