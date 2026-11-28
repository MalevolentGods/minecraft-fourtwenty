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
        BudBarnRegistry.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, DispensaryConfig.SPEC, "fourtwenty-dispensary-server.toml");
        container.registerConfig(ModConfig.Type.SERVER, com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderConfig.SPEC, "fourtwenty-builders-server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, com.malevolentgods.fourtwenty.compat.minecolonies.client.MidnightMachineClientConfig.SPEC, "fourtwenty-midnight-machine-client.toml");
        NeoForge.EVENT_BUS.addListener(ColonyIntegration::tick);
        NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderWatchdog::tick);
        com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedPayload.register(bus);
        com.malevolentgods.fourtwenty.compat.minecolonies.builder.MidnightMachinePayload.register(bus);
        com.malevolentgods.fourtwenty.compat.minecolonies.builder.HighStatusPayload.register(bus);
        com.malevolentgods.fourtwenty.compat.minecolonies.builder.MidnightMachineService.register();
        NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedFlight::tick);
        NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedFlight::leave);
        NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedFlight::join);
        NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.compat.minecolonies.service.PlayerSales::interact);
        NeoForge.EVENT_BUS.addListener(DispensaryCommands::register);
        // The Weed College (specs/017-weed-college)
        com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeRegistry.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeConfig.SPEC, "fourtwenty-college-server.toml");
        NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeStudies::register);
        NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeCommands::register);
        com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeNetwork.register(bus);
        // The Tie Dye Shack (specs/018-tie-dye-shack)
        com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.TieDyeRegistry.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.TieDyeConfig.SPEC, "fourtwenty-tiedye-server.toml");
        com.malevolentgods.fourtwenty.registry.FarmYieldModifier.bonusChance =
                com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeEffects::farmBonusChance;
        com.malevolentgods.fourtwenty.power.ColonyFactor.lookup = com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeResearch::factor;
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) ->
                com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeResearch.forgetEffects());
        com.malevolentgods.fourtwenty.infuser.Infuser.caughtListener =
                com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeResearch::memberColours;
        NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeResearch::login);
        com.malevolentgods.fourtwenty.util.SmokeEvents.smokedListener =
                com.malevolentgods.fourtwenty.compat.minecolonies.service.Exposure::secondhand;
        com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity.burningListener =
                com.malevolentgods.fourtwenty.compat.minecolonies.benefits.BongSessionBenefits::burning;
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                com.minecolonies.api.IMinecoloniesAPI.getInstance().getEventBus().subscribe(
                        com.minecolonies.api.eventbus.events.colony.ColonyDeletedModEvent.class, deleted -> {
                            if (deleted.getColony().getWorld() instanceof ServerLevel level) {
                                DispensarySavedData.get(level).removeColony(deleted.getColony().getID());
                                com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeSavedData.get(level).removeColony(deleted.getColony().getID());
                                com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.TieDyeSavedData.get(level).removeColony(deleted.getColony().getID());
                            }
                        })));
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            // Native voice lookup is keyed by job path and has no fallback for addon professions.
            var voices = com.minecolonies.api.sounds.ModSoundEvents.CITIZEN_SOUND_EVENTS;
            voices.putIfAbsent("budtender", java.util.Map.copyOf(java.util.Objects.requireNonNull(voices.get("unemployed"))));
            voices.putIfAbsent("trimdaddy", java.util.Map.copyOf(java.util.Objects.requireNonNull(voices.get("unemployed"))));
            voices.putIfAbsent("weedologist", java.util.Map.copyOf(java.util.Objects.requireNonNull(voices.get("unemployed"))));
            // the Tie Dyer sounds like MineColonies' own Dyer (without a voice entry her AI throws every tick)
            voices.putIfAbsent("tie_dyer", java.util.Map.copyOf(java.util.Objects.requireNonNull(voices.getOrDefault("dyer", voices.get("unemployed")))));
        }));
        // Builders pay a craftable substitute for froglights and the architect's cutter; see BuilderCostSubstitutions.
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers.add(
                    new com.malevolentgods.fourtwenty.compat.minecolonies.placement.SubstituteCostPlacementHandler());
            com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers.add(
                    new com.malevolentgods.fourtwenty.compat.minecolonies.placement.MicroblockPlacementHandler());
            com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers.add(
                    new com.malevolentgods.fourtwenty.compat.minecolonies.placement.BedExtensionPlacementHandler());
            com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers.add(
                    new com.malevolentgods.fourtwenty.compat.minecolonies.placement.PottedPotPotPlacementHandler());
            com.mojang.logging.LogUtils.getLogger().info("FourTwenty builder cost substitutions enabled (froglights cost a sea lantern; the architect's cutter a cobblestone; CB microblocks cost their source blocks; a king bed's extension is free; a Potted Pot Pot costs its Pot Pot)");
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
            // before MineColonies' own colony tick checks crafter recipes against them (research R6)
            com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeEffects.keepApplied(colony);
            for (var citizen : colony.getCitizenManager().getCitizens()) {
                data.updateAffinity(colony.getID(), citizen.getUUID(),
                        affinity -> com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules.tick(affinity, data.time()));
                var state = data.observe(colony.getID(), citizen.getUUID(), qualifies(citizen, data), rules(citizen, data));
                com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CitizenBenefits.refresh(citizen, state, data.time());
                com.malevolentgods.fourtwenty.compat.minecolonies.benefits.BongSessionBenefits.refresh(citizen, data.time());
                com.malevolentgods.fourtwenty.compat.minecolonies.service.BongSessions.refresh(citizen, data);
            }
            // This listener runs on each simulation tick; native building callbacks do not.
            if (data.time() % 100 == 0) {
                for (var building : colony.getServerBuildingManager().getBuildings().values()) {
                    if (building instanceof DispensaryBuilding hut) hut.getModule(DispensaryRegistry.STATUS).refresh();
                    else if (building instanceof com.malevolentgods.fourtwenty.compat.minecolonies.building.BudBarnBuilding barn) barn.getModule(BudBarnRegistry.STATUS).refresh();
                    else if (building instanceof com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeBuilding college) {
                        college.getModule(com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeRegistry.STATUS).refresh();
                        // the level, the members' colours and the features can change without the research changing
                        college.getModule(com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeRegistry.BOARD).refresh();
                    }
                    else if (building instanceof com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.ShackBuilding shack)
                        shack.getModule(com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.TieDyeRegistry.ORDERS).refresh();
                }
            }
        }
    }
    private ColonyIntegration() {}
}
