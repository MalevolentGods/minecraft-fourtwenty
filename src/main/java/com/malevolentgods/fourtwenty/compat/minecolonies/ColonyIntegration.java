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
        NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.compat.minecolonies.service.PlayerSales::interact);
        com.malevolentgods.fourtwenty.util.SmokeEvents.smokedListener =
                com.malevolentgods.fourtwenty.compat.minecolonies.service.Exposure::secondhand;
        com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity.burningListener =
                com.malevolentgods.fourtwenty.compat.minecolonies.benefits.BongSessionBenefits::burning;
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
        com.mojang.logging.LogUtils.getLogger().info("FourTwenty dispensary foundation enabled (MineColonies 1.1.1343)");
    }
    /** Guard housing is the native home assignment, including work-at-home guard quarters. Never use workplace fallback. */
    public static boolean eligible(ICitizenData citizen) {
        var home = citizen.getHomeBuilding();
        return DemandRules.eligible(!citizen.isChild(), home != null, home == null ? 0 : home.getBuildingLevel());
    }
    /** Housing plus affinity: a craving exists only after first exposure, with interest, and not on a tolerance break. */
    public static boolean qualifies(ICitizenData citizen, DispensarySavedData saved) {
        return eligible(citizen) && saved.affinity(citizen.getColony().getID(), citizen.getUUID()).wantsDoses(saved.time());
    }
    /** The citizen's own interval (and equal grace), scaled from the configured daily values by their step. */
    public static DemandRules rules(ICitizenData citizen, DispensarySavedData saved) {
        var base = DispensaryConfig.rules();
        int step = saved.affinity(citizen.getColony().getID(), citizen.getUUID()).step();
        if (step < 1 || step >= com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules.MAX_STEP) return base;
        return new DemandRules(com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules.intervalTicks(step, base.intervalTicks()),
                com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules.intervalTicks(step, base.graceTicks()), base.healthTicks());
    }
    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.tickRateManager().runsNormally()) return;
        var data = DispensarySavedData.get(level);
        data.advance();
        for (var colony : IColonyManager.getInstance().getColonies(level)) {
            for (var citizen : colony.getCitizenManager().getCitizens()) {
                data.updateAffinity(colony.getID(), citizen.getUUID(),
                        affinity -> com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules.tick(affinity, data.time()));
                var state = data.observe(colony.getID(), citizen.getUUID(), qualifies(citizen, data), rules(citizen, data));
                com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CitizenBenefits.refresh(citizen, state, data.time());
                com.malevolentgods.fourtwenty.compat.minecolonies.benefits.BongSessionBenefits.refresh(citizen, data.time());
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
