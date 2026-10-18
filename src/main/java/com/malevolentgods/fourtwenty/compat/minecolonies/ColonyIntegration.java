package com.malevolentgods.fourtwenty.compat.minecolonies;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.DemandRules;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Loaded only after the core entrypoint checks MineColonies presence. No client-only references. */
public final class ColonyIntegration {
    public static void register(net.neoforged.bus.api.IEventBus bus, ModContainer container) {
        DispensaryRegistry.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, DispensaryConfig.SPEC, "fourtwenty-dispensary-server.toml");
        NeoForge.EVENT_BUS.addListener(ColonyIntegration::tick);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                com.minecolonies.api.IMinecoloniesAPI.getInstance().getEventBus().subscribe(
                        com.minecolonies.api.eventbus.events.colony.ColonyDeletedModEvent.class, deleted -> {
                            if (deleted.getColony().getWorld() instanceof ServerLevel level) {
                                DispensarySavedData.get(level).removeColony(deleted.getColony().getID());
                            }
                        })));
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            // Native voice lookup is keyed by job path and has no fallback for addon professions.
            var voices = com.minecolonies.api.sounds.ModSoundEvents.CITIZEN_SOUND_EVENTS;
            voices.putIfAbsent("budtender", java.util.Map.copyOf(java.util.Objects.requireNonNull(voices.get("unemployed"))));
        }));
        // Builders pay a craftable substitute for froglights; see BuilderCostSubstitutions.
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers.add(
                    new com.malevolentgods.fourtwenty.compat.minecolonies.placement.SubstituteCostPlacementHandler());
            com.mojang.logging.LogUtils.getLogger().info("FourTwenty builder cost substitutions enabled (froglights cost a sea lantern)");
        }));
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
                var state = data.observe(colony.getID(), citizen.getUUID(), eligible(citizen), rules);
                com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CitizenBenefits.refresh(citizen, state, data.time());
            }
            // This listener runs on each simulation tick; native building callbacks do not.
            if (data.time() % 100 == 0) {
                for (var building : colony.getServerBuildingManager().getBuildings().values()) {
                    if (building instanceof DispensaryBuilding hut) hut.getModule(DispensaryRegistry.STATUS).refresh();
                }
            }
        }
    }
    private ColonyIntegration() {}
}
