package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.malevolentgods.fourtwenty.domain.college.ColonyResearch;
import com.malevolentgods.fourtwenty.domain.college.Study;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.research.GlobalResearchEffect;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What happens when a study is written up, beyond being marked done (specs/017-weed-college FR-022, FR-024, FR-025). Colours and colony
 * effects are read straight from the record ({@link CollegeResearch#knownColours}, {@link CollegeResearch#factor}); this applies the rest.
 *
 * <p>Unlock studies (Armory, Glassworks; research R6) apply a MineColonies research effect, {@code fourtwenty:effects/<study>}, that our
 * crafter recipes name as their {@code research-id}. MineColonies honours the id only because data/fourtwenty/researches/effects defines
 * it. The effect manager is rebuilt from the University's own research on load and whenever a player undoes a University research, which
 * drops ours, so {@link #keepApplied} puts back any that are missing on every colony tick: a strength lookup per finished unlock study.
 */
public final class CollegeEffects {
    private CollegeEffects() {}

    private static final Map<String, ResourceLocation> IDS = new ConcurrentHashMap<>();

    public static void finished(IColony colony, Study study) {
        apply(colony, study);
        deliverSeeds(colony, study);
    }

    /**
     * A Botany strain study's harvest (FR-022, research R8): a batch of seeds of each strain it bred, plain (no traits), into the colony's
     * warehouse closest to a College. What doesn't fit goes into a College for the couriers to carry over, and only if that is full too is
     * it dropped at the College's door, so a batch is never lost. Returns how many seeds reached the warehouse.
     */
    public static int deliverSeeds(IColony colony, Study study) {
        Study.Gives g = study.gives();
        if (g.strains().isEmpty() || g.seedsEach() <= 0) return 0;
        CollegeBuilding college = null;
        for (var b : colony.getServerBuildingManager().getBuildings().values()) {
            if (b instanceof CollegeBuilding c) { college = c; break; }
        }
        var near = college != null ? college.getPosition() : colony.getCenter();
        var warehouse = colony.getServerBuildingManager().getClosestWarehouseInColony(near);
        int stored = 0;
        for (String id : g.strains()) {
            var strain = com.malevolentgods.fourtwenty.domain.Strain.byId(id);
            if (!strain.getSerializedName().equals(id)) {
                com.mojang.logging.LogUtils.getLogger().error("Weed College: {} names an unknown strain {}; no seeds for it", study.id(), id);
                continue;
            }
            var seeds = com.malevolentgods.fourtwenty.registry.ModDataComponents.withStrain(
                    new net.minecraft.world.item.ItemStack(com.malevolentgods.fourtwenty.registry.ModItems.WEED_SEEDS.get(), g.seedsEach()), strain);
            int count = seeds.getCount();
            var handler = warehouse == null ? null : warehouse.getItemHandlerCap();
            if (handler != null) seeds = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(handler, seeds, false);
            stored += count - seeds.getCount();
            if (seeds.isEmpty() || college == null) {
                if (!seeds.isEmpty()) drop(colony, near, seeds);
                continue;
            }
            var hut = college.getItemHandlerCap();
            if (hut != null) seeds = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(hut, seeds, false);
            college.createPickupRequest(com.minecolonies.api.colony.requestsystem.requestable.deliveryman.AbstractDeliverymanRequestable
                    .getMaxBuildingPriority(true));
            if (!seeds.isEmpty()) drop(colony, near, seeds);
        }
        return stored;
    }

    /**
     * The chance of an extra bud when a weed crop's loot is rolled (FR-022, research R8): the Botany yield study strength of the harvesting
     * citizen's colony. A player harvesting gets nothing extra. MineColonies workers break blocks through {@code BlockPosUtil.getBlockDrops},
     * which puts no entity in the loot context, only the worker's own position as its origin, so a citizen standing exactly there is the
     * harvester.
     */
    public static double farmBonusChance(net.minecraft.world.level.storage.loot.LootContext context) {
        var entity = context.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY);
        com.minecolonies.api.entity.citizen.AbstractEntityCitizen citizen = null;
        if (entity instanceof com.minecolonies.api.entity.citizen.AbstractEntityCitizen c) {
            citizen = c;
        } else if (entity == null) {
            var origin = context.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
            if (origin != null) {
                var found = context.getLevel().getEntitiesOfClass(com.minecolonies.api.entity.citizen.AbstractEntityCitizen.class,
                        new net.minecraft.world.phys.AABB(origin, origin).inflate(0.01), c -> c.position().distanceToSqr(origin) < 1.0E-4);
                if (!found.isEmpty()) citizen = found.getFirst();
            }
        }
        if (citizen == null) return 0;
        IColony colony = citizen.getCitizenColonyHandler().getColony();
        if (colony == null) return 0;
        return com.malevolentgods.fourtwenty.domain.college.ColonyEffects.factor(
                com.malevolentgods.fourtwenty.domain.college.ColonyEffects.Effect.FARM_BONUS_BUD, CollegeResearch.doneStudies(colony));
    }

    private static void drop(IColony colony, net.minecraft.core.BlockPos at, net.minecraft.world.item.ItemStack stack) {
        var level = colony.getWorld();
        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, stack));
    }

    /** Re-applies the research effect of every finished unlock study the colony's effect manager has lost. */
    public static void keepApplied(IColony colony) {
        ColonyResearch r = CollegeResearch.record(colony);
        if (r.done().isEmpty()) return;
        var board = CollegeStudies.board();
        for (String id : r.done().keySet()) {
            Study s = board.get(id);
            if (s != null) apply(colony, s);
        }
    }

    /** Whether the colony has the study's research effect now (the colony tests check this). */
    public static boolean applied(IColony colony, Study study) {
        String effect = study.gives().researchEffect();
        return effect != null && colony.getResearchManager().getResearchEffects().getEffectStrength(id(effect)) > 0;
    }

    private static void apply(IColony colony, Study study) {
        String effect = study.gives().researchEffect();
        if (effect == null) return;
        var effects = colony.getResearchManager().getResearchEffects();
        ResourceLocation id = id(effect);
        if (effects.getEffectStrength(id) > 0) return;
        String name = "college.fourtwenty.effect." + id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
        effects.applyEffect(new GlobalResearchEffect(id, name, "", 1, 1));
    }

    private static ResourceLocation id(String effect) { return IDS.computeIfAbsent(effect, ResourceLocation::parse); }
}
