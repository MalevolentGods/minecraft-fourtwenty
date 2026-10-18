package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.benefits.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.CitizenDemand.Status;
import com.malevolentgods.fourtwenty.compat.minecolonies.service.JointService;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.*;
import com.minecolonies.api.entity.citizen.happiness.*;
import com.minecolonies.api.research.util.ResearchConstants;
import com.minecolonies.core.colony.CitizenData;
import com.minecolonies.core.colony.CitizenDataView;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenHappinessHandler;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import com.minecolonies.core.util.AttributeModifierUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("fourtwenty_benefitstests")
@PrefixGameTestTemplate(false)
public final class DispensaryBenefitsGameTests {
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final IColony colony;
        EntityCitizen actor;
        final CitizenData citizen;
        Fixture(GameTestHelper h) {
            this.h = h;
            var pos = h.absolutePos(new BlockPos(1, 1, 1));
            h.getLevel().setDayTime(6000);
            colony = IColonyManager.getInstance().createColony(h.getLevel(), pos, FakePlayerFactory.getMinecraft(h.getLevel()), "Dispensary benefits", "Colonial");
            actor = DispensaryProductionGameTests.actor(h, colony, pos.offset(1, 0, 0));
            citizen = (CitizenData)actor.getCitizenData();
            h.getLevel().setBlockAndUpdate(pos, com.minecolonies.api.blocks.ModBlocks.blockHutHome.defaultBlockState());
            var home = colony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding)h.getLevel().getBlockEntity(pos), h.getLevel());
            home.setBuildingLevel(3); citizen.setHomeBuilding(home); Exposures.dailySmoker(citizen);
            CitizenBenefits.refresh(citizen);
        }
        @Override public void close() {
            actor.discard(); IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }
    private static void near(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Double.isFinite(actual) && Math.abs(actual - expected) < 0.00001, message + ": " + actual + " expected " + expected);
    }
    private static CannabisHappinessModifier modifier(CitizenData citizen) {
        return (CannabisHappinessModifier)citizen.getCitizenHappinessHandler().getModifier(DispensaryIds.SATISFACTION);
    }
    private static void advance(DispensarySavedData saved, long until) { while (saved.time() < until) saved.advance(); }
    private static void research(IColony colony, double strength) {
        colony.getResearchManager().getResearchEffects().removeAllEffects();
        colony.getResearchManager().getResearchEffects().applyEffect(new com.minecolonies.core.research.GlobalResearchEffect(
                ResearchConstants.HAPPINESS, "fixture", "fixture", strength, strength));
    }
    private static void membership(Fixture f, boolean member) throws Exception {
        if (!net.neoforged.fml.ModList.get().isLoaded("minecolonies_cult")) return;
        var service = Class.forName("com.kingdomsgate.cult.runtime.CultService");
        var state = service.getMethod("state", IColony.class).invoke(null, f.colony);
        if (!member) { state.getClass().getMethod("remove", java.util.UUID.class).invoke(state, f.citizen.getUUID()); return; }
        var rulesClass = Class.forName("com.kingdomsgate.cult.domain.CultRules");
        var rules = rulesClass.getMethod("defaults").invoke(null);
        state.getClass().getMethod("observe", long.class, int.class, java.util.UUID.class, rulesClass)
                .invoke(state, 0L, 3, java.util.UUID.randomUUID(), rules);
        for (long day = 0; day < 6; day++) state.getClass().getMethod("hear", java.util.UUID.class, long.class, rulesClass)
                .invoke(state, f.citizen.getUUID(), day, rules);
    }

    @GameTest(template = "empty", batch = "benefits_calibration")
    public static void weightedHappinessDirectionWithResearchAndMembership(GameTestHelper h) throws Exception {
        try (var f = new Fixture(h)) {
            var handler = f.citizen.getCitizenHappinessHandler();
            var cult = handler.getModifier("minecolonies_cult_membership");
            for (boolean member : new boolean[]{false, true}) {
                membership(f, member);
                for (double research : new double[]{0, .25}) for (double weight : new double[]{4, 20.5}) for (double baseline : new double[]{0, .2, .6, .9, 1.2}) {
                    research(f.colony, research);
                    handler.happinessFactors.clear();
                    handler.addModifier(new StaticHappinessModifier("food", weight, new StaticHappinessSupplier(baseline)));
                    if (cult != null) handler.addModifier(cult);
                    handler.addModifier(new CannabisHappinessModifier(Status.GRACE, 0));
                    double cultFactor = cult == null ? 1 : cult.getFactor(f.citizen);
                    h.assertTrue(!member || cult == null || cultFactor > 1, "Actual Kingdom's Gate membership contributes its own factor");
                    double total = baseline * weight + (cultFactor == 1 ? 0 : cultFactor * cult.getWeight());
                    double weights = weight + (cultFactor == 1 ? 0 : cult.getWeight());
                    double before = handler.getHappiness(f.colony, f.citizen);
                    near(h, before, Math.min(10, 10 * total / weights * (1 + research)), "Native neutral baseline");
                    handler.addModifier(new CannabisHappinessModifier(Status.SATISFIED, .1));
                    double satisfied = handler.getHappiness(f.colony, f.citizen);
                    near(h, satisfied, Math.min(10, 10 * (total + .11) / (weights + .1) * (1 + research)), "Native satisfied magnitude");
                    h.assertTrue(satisfied >= before && (before == 10 || satisfied > before), "Bonus improves uncapped happiness");
                    handler.addModifier(new CannabisHappinessModifier(Status.UNMET, .1));
                    double unmet = handler.getHappiness(f.colony, f.citizen);
                    near(h, unmet, Math.min(10, 10 * total / (weights + .1) * (1 + research)), "Native penalty magnitude");
                    h.assertTrue(unmet <= before && (before == 0 || before == 10 || unmet < before), "Penalty lowers positive uncapped happiness");
                    h.assertTrue(Math.abs(satisfied - before) < .34 && Math.abs(unmet - before) < .34, "Default weight stays modest in representative native states");
                    com.mojang.logging.LogUtils.getLogger().info("Cannabis calibration member={} research={} weight={} baseline={}: neutral={} satisfied={} unmet={}", member, research, weight, baseline, before, satisfied, unmet);
                    h.assertTrue(handler.getModifier("food").getFactor(f.citizen) == baseline && handler.getModifier("minecolonies_cult_membership") == cult, "Other modifiers remain intact");
                }
            }
            h.succeed();
        }
    }

    @GameTest(template = "empty", batch = "benefits_health")
    public static void oneHeartRefreshRecalculationExpiryAndOtherBuffs(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var saved = DispensarySavedData.get(h.getLevel());
            var state = saved.citizen(f.colony.getID(), f.citizen.getUUID());
            var attribute = f.actor.getAttribute(Attributes.MAX_HEALTH);
            var other = new AttributeModifier(ResourceLocation.parse("fourtwenty_colonytests:unrelated"), 4, AttributeModifier.Operation.ADD_VALUE);
            attribute.addTransientModifier(other);
            double base = f.actor.getMaxHealth(); f.actor.setHealth(12);
            h.assertTrue(saved.recordConsumption(f.colony.getID(), f.citizen.getUUID(), state.revision(), new DemandRules(5, 5, 20)), "Fixture completed consumption");
            CitizenBenefits.refresh(f.citizen);
            near(h, f.actor.getMaxHealth(), base + 2, "Exactly one extra heart"); near(h, f.actor.getHealth(), 12, "No healing on grant");
            long firstExpiry = saved.citizen(f.colony.getID(), f.citizen.getUUID()).healthUntil();
            advance(saved, saved.time() + 5);
            state = saved.citizen(f.colony.getID(), f.citizen.getUUID());
            h.assertTrue(saved.recordConsumption(f.colony.getID(), f.citizen.getUUID(), state.revision(), new DemandRules(5, 5, 20)), "Second completed dose");
            CitizenBenefits.refresh(f.citizen);
            near(h, f.actor.getMaxHealth(), base + 2, "Refresh never stacks"); near(h, f.actor.getHealth(), 12, "Refresh never heals");
            h.assertTrue(attribute.getModifier(other.id()).equals(other), "Other buff preserved");
            advance(saved, firstExpiry); CitizenBenefits.refresh(f.citizen);
            h.assertTrue(attribute.hasModifier(CitizenBenefits.HEALTH_ID), "Refresh extends entitlement past original expiry");
            // Exercise the native bulk reset at elevated health: restore before its clamp, not a tick later.
            attribute.removeModifier(other.id()); f.actor.setHealth(f.actor.getMaxHealth());
            float beforeReset = f.actor.getHealth();
            AttributeModifierUtils.removeAllHealthModifiers(f.actor);
            near(h, f.actor.getHealth(), beforeReset, "Native reset does not discard the earned heart");
            h.assertTrue(attribute.hasModifier(CitizenBenefits.HEALTH_ID), "Bulk recalculation retains entitlement");
            f.citizen.initEntityValues(); f.citizen.applyResearchEffects(); f.actor.getCitizenExperienceHandler().updateLevel();
            long count = attribute.getModifiers().stream().filter(m -> m.id().equals(CitizenBenefits.HEALTH_ID)).count();
            h.assertTrue(count == 1, "Job/research/level initialization retains exactly one modifier");
            attribute.addTransientModifier(other);
            f.citizen.setHomeBuilding(null); CitizenBenefits.refresh(f.citizen);
            h.assertTrue(modifier(f.citizen).status() == Status.INELIGIBLE && attribute.hasModifier(CitizenBenefits.HEALTH_ID), "Eligibility loss removes demand but preserves paid health until expiry");
            f.actor.setHealth(f.actor.getMaxHealth());
            double boosted = f.actor.getMaxHealth();
            advance(saved, saved.citizen(f.colony.getID(), f.citizen.getUUID()).healthUntil()); CitizenBenefits.refresh(f.citizen);
            near(h, f.actor.getMaxHealth(), boosted - 2, "Exact health expiry"); near(h, f.actor.getHealth(), f.actor.getMaxHealth(), "Safely clamps elevated health");
            h.assertTrue(attribute.getModifier(other.id()).equals(other), "Expiry preserves other buffs");
            h.succeed();
        }
    }

    /** The actual native packet reader, with no GUI, job view, or client-world dependencies for an unemployed adult. */
    private static final class View extends CitizenDataView { View(int id) { super(id, null); } }
    @GameTest(template = "empty", batch = "benefits_tick", timeoutTicks = 100)
    public static void levelTicksRefreshExpiriesAndEligibility(GameTestHelper h) {
        var f = new Fixture(h);
        var saved = DispensarySavedData.get(h.getLevel());
        var state = saved.citizen(f.colony.getID(), f.citizen.getUUID());
        h.assertTrue(saved.recordConsumption(f.colony.getID(), f.citizen.getUUID(), state.revision(), new DemandRules(5, 5, 5)), "Fixture short entitlement");
        CitizenBenefits.refresh(f.citizen);
        h.runAfterDelay(7, () -> {
            h.assertTrue(modifier(f.citizen).status() == Status.GRACE, "Level tick expires satisfaction without manual reconciliation");
            h.assertTrue(!f.actor.getAttribute(Attributes.MAX_HEALTH).hasModifier(CitizenBenefits.HEALTH_ID), "Level tick expires health");
            verifyView(h, f.citizen);
        });
        h.runAfterDelay(12, () -> {
            h.assertTrue(modifier(f.citizen).status() == Status.UNMET, "Level tick applies overdue penalty");
            verifyView(h, f.citizen);
            f.citizen.setIsChild(true);
        });
        h.runAfterDelay(14, () -> {
            try {
                h.assertTrue(modifier(f.citizen).status() == Status.INELIGIBLE, "Level tick removes ineligible demand");
                verifyView(h, f.citizen); h.succeed();
            } finally { f.close(); }
        });
    }
    private static void verifyView(GameTestHelper h, CitizenData citizen) {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            citizen.serializeViewNetworkData(buf);
            var view = new View(citizen.getId()); view.deserialize(buf);
            var actual = (CannabisHappinessModifier)view.getHappinessHandler().getModifier(DispensaryIds.SATISFACTION);
            h.assertTrue(actual.status() == modifier(citizen).status(), "Fresh client view receives authoritative status");
            near(h, actual.getFactor(null), modifier(citizen).getFactor(citizen), "Visible factor agrees with server");
            near(h, actual.getWeight(), modifier(citizen).getWeight(), "Visible weight agrees with server");
            near(h, view.getHappiness(), citizen.getCitizenHappinessHandler().getHappiness(citizen.getColony(), citizen), "Packet carries refreshed total");
        } finally { buf.release(); }
    }
    @GameTest(template = "empty", batch = "benefits_sync")
    public static void completionExpiryEligibilityAndNativeViewReload(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var saved = DispensarySavedData.get(h.getLevel());
            var handler = f.citizen.getCitizenHappinessHandler();
            handler.addModifier(new StaticHappinessModifier("food", 3, new StaticHappinessSupplier(.5)));
            f.citizen.clearDirty(); CitizenBenefits.refresh(f.citizen);
            h.assertTrue(!f.citizen.isDirty(), "Unchanged benefits do not flood view synchronization");
            verifyView(h, f.citizen);
            f.actor.getInventoryCitizen().setStackInSlot(0, new ItemStack(ModItems.WEED_JOINT.get()));
            double before = f.actor.getMaxHealth(); CitizenBenefits.refresh(f.citizen);
            near(h, f.actor.getMaxHealth(), before, "Carried/collected joint gives no benefit");
            h.assertTrue(modifier(f.citizen).status() == Status.GRACE, "Collection alone does not satisfy demand");
            var state = saved.citizen(f.colony.getID(), f.citizen.getUUID());
            h.assertTrue(JointService.consume(f.actor, state.revision()), "Actual item extraction completes consumption");
            h.assertTrue(f.citizen.isDirty() && modifier(f.citizen).status() == Status.SATISFIED, "Completion refreshes and schedules synchronization immediately");
            near(h, f.actor.getMaxHealth(), before + 2, "Completion grants health immediately"); verifyView(h, f.citizen);
            var registries = h.getLevel().registryAccess();
            var restored = CitizenData.loadFromNBT(f.colony, f.citizen.serializeNBT(registries), registries);
            h.assertTrue(modifier(restored).status() == Status.SATISFIED, "Server modifier installed before citizen save read");
            verifyView(h, restored);
            var durable = DispensarySavedData.load(saved.save(new CompoundTag(), registries), registries);
            h.assertTrue(durable.citizen(f.colony.getID(), f.citizen.getUUID()).equals(saved.citizen(f.colony.getID(), f.citizen.getUUID())), "Both benefit deadlines and revision survive persisted round trip");
            state = saved.citizen(f.colony.getID(), f.citizen.getUUID());
            advance(saved, state.satisfiedUntil()); f.citizen.clearDirty(); CitizenBenefits.refresh(f.citizen);
            h.assertTrue(f.citizen.isDirty() && modifier(f.citizen).status() == Status.GRACE, "Satisfaction expiry invalidates cache and sends pending state"); verifyView(h, f.citizen);
            advance(saved, state.graceUntil()); CitizenBenefits.refresh(f.citizen);
            h.assertTrue(modifier(f.citizen).status() == Status.UNMET, "Grace expiry starts penalty"); verifyView(h, f.citizen);
            f.citizen.setHomeBuilding(null); CitizenBenefits.refresh(f.citizen);
            h.assertTrue(modifier(f.citizen).status() == Status.INELIGIBLE, "Housing loss removes penalty"); verifyView(h, f.citizen);
            h.succeed();
        }
    }

    @GameTest(template = "empty", batch = "benefits_unload")
    public static void entityUnloadRestoresRemainingEntitlementWithoutReplay(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var saved = DispensarySavedData.get(h.getLevel());
            var state = saved.citizen(f.colony.getID(), f.citizen.getUUID());
            h.assertTrue(saved.recordConsumption(f.colony.getID(), f.citizen.getUUID(), state.revision(), new DemandRules(30, 10, 30)), "Fixture completed consumption");
            CitizenBenefits.refresh(f.citizen); f.actor.setHealth(f.actor.getMaxHealth());
            float savedHealth = f.actor.getHealth();
            var entityTag = f.actor.saveWithoutId(new CompoundTag());
            long revision = saved.citizen(f.colony.getID(), f.citizen.getUUID()).revision();
            f.actor.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
            advance(saved, saved.time() + 10);
            f.actor = (EntityCitizen)com.minecolonies.api.entity.ModEntities.CITIZEN.create(h.getLevel());
            f.actor.load(entityTag); h.getLevel().addFreshEntity(f.actor);
            f.actor.getCitizenColonyHandler().registerWithColony(f.colony.getID(), f.citizen.getId());
            f.citizen.setEntity(f.actor); f.citizen.initEntityValues();
            CitizenBenefits.refresh(f.citizen);
            h.assertTrue(f.actor.getAttribute(Attributes.MAX_HEALTH).hasModifier(CitizenBenefits.HEALTH_ID), "Reload reconciles attribute with durable entitlement");
            near(h, f.actor.getHealth(), savedHealth, "Reload preserves a filled extra heart without healing");
            h.assertTrue(saved.citizen(f.colony.getID(), f.citizen.getUUID()).revision() == revision, "Reload never replays consumption");
            var expiry = saved.citizen(f.colony.getID(), f.citizen.getUUID()).healthUntil();
            f.actor.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK); advance(saved, expiry);
            f.actor = (EntityCitizen)com.minecolonies.api.entity.ModEntities.CITIZEN.create(h.getLevel());
            f.actor.load(entityTag); h.getLevel().addFreshEntity(f.actor);
            f.actor.getCitizenColonyHandler().registerWithColony(f.colony.getID(), f.citizen.getId());
            f.citizen.setEntity(f.actor); f.citizen.initEntityValues(); CitizenBenefits.refresh(f.citizen);
            h.assertTrue(!f.actor.getAttribute(Attributes.MAX_HEALTH).hasModifier(CitizenBenefits.HEALTH_ID), "Expiry while unloaded prevents stale restoration");
            near(h, f.actor.getHealth(), f.actor.getMaxHealth(), "Expired cached attribute safely clamps on reload");
            h.succeed();
        }
    }
}
