package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CitizenBenefits;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.service.*;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import java.util.*;

/** Fixture checks are deliberately distinct from connected-player and ordinary-server acceptance. */
@GameTestHolder("fourtwenty_lifecycletests")
@PrefixGameTestTemplate(false)
public final class DispensaryLifecycleGameTests {
    private static final BlockPos CHECKPOINT = new BlockPos(1200, 100, 1200);
    private static final String NAME = "FourTwenty Phase 6 checkpoint";

    @GameTestGenerator
    public static Collection<TestFunction> restartCheckpoint() {
        String stage = System.getProperty("fourtwenty.dispensaryLifecycleStage", "");
        if (stage.isEmpty()) return List.of();
        if (!List.of("write", "read").contains(stage)) throw new IllegalArgumentException("dispensaryLifecycleStage must be write or read");
        return List.of(new TestFunction("dispensary_restart", "colony_restart_" + stage,
                "fourtwenty_restarttests:empty", 200, 0, true, h -> checkpoint(h, stage)));
    }

    private static void checkpoint(GameTestHelper h, String stage) {
        try {
            var level = h.getLevel();
            var manager = IColonyManager.getInstance();
            var saved = DispensarySavedData.get(level);
            var path = level.getServer().getWorldPath(LevelResource.ROOT).resolve("dispensary-checkpoint.nbt");
            level.getChunkAt(CHECKPOINT);
            if (stage.equals("write")) {
                h.assertTrue(!java.nio.file.Files.exists(path), "Write requires a fresh disposable world");
                var colony = manager.createColony(level, CHECKPOINT, FakePlayerFactory.getMinecraft(level), NAME, "FourTwenty");
                colony.setName(NAME);
                var hut = DispensaryProductionGameTests.hut(h, colony, CHECKPOINT);
                hut.setBuildingLevel(3); hut.onUpgradeComplete(null, 3);
                var worker = colony.getCitizenManager().createAndRegisterCivilianData();
                worker.setIsChild(false);
                h.assertTrue(hut.getModule(DispensaryRegistry.WORK).assignCitizen(worker), "Native worker assigned");
                ItemHandlerHelper.insertItemStacked(hut.getItemHandlerCap(), new ItemStack(ModItems.WEED_JOINT.get(), 7), false);
                ItemHandlerHelper.insertItemStacked(hut.getItemHandlerCap(), new ItemStack(ModItems.WEED_BUD.get(), 3), false);
                ItemHandlerHelper.insertItemStacked(hut.getItemHandlerCap(), new ItemStack(Items.PAPER, 6), false);
                hut.getModule(DispensaryRegistry.CRAFT).onColonyTick(colony);
                h.assertTrue(DispensaryProductionGameTests.requests(hut) == 1, "One pending stock request");
                var expected = new CompoundTag();
                expected.putInt("colony", colony.getID()); expected.putInt("worker", worker.getId());
                var records = new ListTag();
                // Travel/wait have no personal joint; transfer/smoke retain one unpaid joint.
                for (String state : List.of("travel", "wait", "transfer", "smoke", "satisfied", "penalty")) {
                    var citizen = colony.getCitizenManager().createAndRegisterCivilianData();
                    citizen.setIsChild(false);
                    var homePos = CHECKPOINT.offset(2 + records.size() * 2, 0, 2);
                    level.setBlockAndUpdate(homePos, com.minecolonies.api.blocks.ModBlocks.blockHutHome.defaultBlockState());
                    var home = colony.getServerBuildingManager().addNewBuilding(
                            (com.minecolonies.core.tileentities.TileEntityColonyBuilding) level.getBlockEntity(homePos), level);
                    home.setBuildingLevel(3); home.onUpgradeComplete(null, 3);
                    h.assertTrue(home.getModule(com.minecolonies.core.colony.buildings.modules.LivingBuildingModule.class)
                            .assignCitizen(citizen), "Native persistent residence assignment");
                    boolean paid = state.equals("satisfied");
                    var rules = state.equals("penalty") ? new DemandRules(1, 1, 1) : DemandRules.defaults();
                    var demand = saved.observe(colony.getID(), citizen.getUUID(), true, rules);
                    if (paid) h.assertTrue(saved.recordConsumption(colony.getID(), citizen.getUUID(), demand.revision(), rules), "Paid checkpoint");
                    if (state.equals("transfer") || state.equals("smoke")) citizen.getInventory().setStackInSlot(0, new ItemStack(ModItems.WEED_JOINT.get()));
                    var entry = new CompoundTag(); entry.putString("stage", state); entry.putInt("id", citizen.getId());
                    entry.putLong("revision", saved.citizen(colony.getID(), citizen.getUUID()).revision()); records.add(entry);
                }
                expected.put("records", records);
                expected.putLong("clock", saved.time());
                colony.markDirty();
                level.getDataStorage().save();
                level.getChunkSource().save(true);
                NbtIo.writeCompressed(expected, path);
                com.mojang.logging.LogUtils.getLogger().info("FOURTWENTY_COLONY_CHECKPOINT_WRITE colony={} clock={}", colony.getID(), saved.time());
            } else {
                var expected = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                var colony = manager.getColonyByWorld(expected.getInt("colony"), level);
                h.assertTrue(colony != null && colony.getName().equals(NAME), "Native colony restored in a new JVM");
                var hut = (DispensaryBuilding) colony.getServerBuildingManager().getBuilding(CHECKPOINT);
                h.assertTrue(hut != null && hut.getBuildingLevel() == 3, "Completed building restored");
                h.assertTrue(hut.getModule(DispensaryRegistry.WORK).getAssignedCitizen().stream()
                        .anyMatch(c -> c.getId() == expected.getInt("worker") && c.getJob() instanceof BudtenderJob), "Worker and job restored");
                h.assertTrue(count(hut, ModItems.WEED_JOINT.get()) == 7 && count(hut, ModItems.WEED_BUD.get()) == 3
                        && count(hut, Items.PAPER) == 6, "Exact native stock survived restart");
                h.assertTrue(DispensaryProductionGameTests.requests(hut) == 1, "Outstanding native request restored");
                for (int i = 0; i < 20; i++) hut.getModule(DispensaryRegistry.CRAFT).onColonyTick(colony);
                h.assertTrue(DispensaryProductionGameTests.requests(hut) == 1, "Restart does not duplicate requests");
                h.assertTrue(hut.waitingCustomers(saved.time()) == 0, "No restored customer leases");
                for (Tag value : expected.getList("records", Tag.TAG_COMPOUND)) {
                    var entry = (CompoundTag) value;
                    var citizen = colony.getCitizenManager().getCivilian(entry.getInt("id"));
                    h.assertTrue(citizen != null, "Citizen restored: " + entry.getString("stage"));
                    var demand = saved.citizen(colony.getID(), citizen.getUUID());
                    h.assertTrue(demand.revision() == entry.getLong("revision"), "No replay on startup: " + entry.getString("stage") + " expected=" + entry.getLong("revision") + " actual=" + demand);
                    String state = entry.getString("stage");
                    int joints = InventoryUtils.getItemCountInItemHandler(citizen.getInventory(), s -> s.is(ModItems.WEED_JOINT.get()));
                    h.assertTrue(joints == (List.of("transfer", "smoke").contains(state) ? 1 : 0), "Exact personal stock: " + state);
                    if (state.equals("satisfied")) {
                        h.assertTrue(demand.healthActive(saved.time()) && !demand.due(saved.time()), "Paid entitlement retained");
                        h.assertTrue(!saved.recordConsumption(colony.getID(), citizen.getUUID(), demand.revision() - 1, DemandRules.defaults()), "Old completion rejected");
                    } else h.assertTrue(demand.lastConsumedAt() == -1, "Uncompleted action receives no benefit");
                    if (state.equals("penalty")) h.assertTrue(demand.status(saved.time()) == CitizenDemand.Status.UNMET, "Penalty remains due");
                }
                h.assertTrue(saved.time() >= expected.getLong("clock") && saved.time() - expected.getLong("clock") < 1000,
                        "Only running simulation ticks advance deadlines, not wall-clock downtime");
                com.mojang.logging.LogUtils.getLogger().info("FOURTWENTY_COLONY_CHECKPOINT_READ colony={} clock={}", colony.getID(), saved.time());
            }
            h.succeed();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static int count(DispensaryBuilding hut, net.minecraft.world.item.Item item) {
        return InventoryUtils.getItemCountInItemHandler(hut.getItemHandlerCap(), s -> s.is(item));
    }

    @GameTest(template = "production", batch = "lifecycle", timeoutTicks = 200)
    public static void unloadRemovalAndReloadInvalidateOnlyTemporaryState(GameTestHelper h) throws Exception {
        try (var f = new DispensaryServiceGameTests.Fixture(h)) {
            var hut = f.blueprint();
            var home = f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome, 24, 20, 3);
            var actor = f.actor(12, 7, home);
            var data = actor.getCitizenData();
            var saved = DispensarySavedData.get(h.getLevel());
            JointService.due(actor);
            var original = saved.citizen(f.colony.getID(), data.getUUID());
            var seat = hut.reserveSeat(actor, saved.time());
            h.assertTrue(seat != null, "Seat reserved");
            var nbt = hut.serializeNBT(h.getLevel().registryAccess());
            hut.deserializeNBT(h.getLevel().registryAccess(), nbt);
            h.assertTrue(!hut.ownsSeat(actor.getUUID(), seat, saved.time()), "Reload clears previous live seat lease");
            actor.getInventoryCitizen().setStackInSlot(0, new ItemStack(ModItems.WEED_JOINT.get(), 2));
            new com.minecolonies.core.entity.ai.workers.CitizenAI(actor);
            var task = ((DispensaryCustomer)actor).fourtwenty$smokingTask();
            h.assertTrue(task.choose(CitizenAIState.IDLE) == CitizenSmokingTask.State.VISIT, "Carried stock starts smoking");
            // Entity removal is the same hook used by native chunk unloading; it must not delete durable data.
            actor.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            h.assertTrue(task.step() == CitizenSmokingTask.Step.INACTIVE && saved.citizen(f.colony.getID(), data.getUUID()).equals(original),
                    "Unload interrupts visit and retains durable record");
            h.assertTrue(data.getInventory().getStackInSlot(0).getCount() == 2, "Unloaded smoke did not consume");
            f.colony.getCitizenManager().removeCivilian(data);
            h.assertTrue(saved.citizen(f.colony.getID(), data.getUUID()).equals(CitizenDemand.empty()), "Permanent native removal forgets record");
            h.assertTrue(!JointService.consume(actor, original.revision()), "Removed entity cannot replay a callback");
            var departing = f.actor(13, 7, home);
            departing.getInventoryCitizen().setStackInSlot(0, new ItemStack(ModItems.WEED_JOINT.get()));
            JointService.due(departing);
            var departure = saved.citizen(f.colony.getID(), departing.getUUID());
            f.colony.getCitizenManager().removeCivilian(departing.getCitizenData());
            h.assertTrue(!JointService.due(departing) && !JointService.consume(departing, departure.revision()),
                    "Still-live departed entity cannot recreate a record or spend personal stock");
            h.assertTrue(departing.getInventoryCitizen().getStackInSlot(0).getCount() == 1,
                    "Departure does not confiscate inventory");
            h.succeed();
        }
    }

    @GameTest(template = "production", batch = "lifecycle_death", timeoutTicks = 100)
    public static void housingChangesAndNativeDeathCleanUp(GameTestHelper h) {
        var f = new DispensaryServiceGameTests.Fixture(h);
        var home = f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome, 24, 20, 3);
        var other = f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome, 20, 20, 3);
        var actor = f.actor(12, 7, home);
        var citizen = actor.getCitizenData();
        var saved = DispensarySavedData.get(h.getLevel());
        actor.getInventoryCitizen().setStackInSlot(0, new ItemStack(ModItems.WEED_JOINT.get(), 2));
        JointService.due(actor);
        h.assertTrue(JointService.consume(actor, saved.citizen(f.colony.getID(), actor.getUUID()).revision()), "Completed consumption");
        var paid = saved.citizen(f.colony.getID(), actor.getUUID());
        citizen.setHomeBuilding(other); CitizenBenefits.refresh(citizen);
        h.assertTrue(saved.citizen(f.colony.getID(), actor.getUUID()).equals(paid), "Qualifying reassignment preserves deadlines");
        other.setBuildingLevel(2); CitizenBenefits.refresh(citizen);
        h.assertTrue(!JointService.due(actor) && actor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .hasModifier(CitizenBenefits.HEALTH_ID), "Eligibility loss suppresses demand but preserves earned health");
        other.setBuildingLevel(3); CitizenBenefits.refresh(citizen);
        h.assertTrue(!JointService.due(actor) && citizen.getInventory().getStackInSlot(0).getCount() == 1,
                "Reentry cannot award an extra dose");
        // Native citizens cap each hit at 20% of max health. Start below that threshold.
        actor.setHealth(1);
        h.assertTrue(actor.hurt(h.getLevel().damageSources().genericKill(), Float.MAX_VALUE), "Lethal native damage accepted");
        h.runAfterDelay(25, () -> {
            try {
                h.assertTrue(f.colony.getCitizenManager().getCivilian(citizen.getId()) == null, "Native death removes citizen data");
                h.assertTrue(saved.citizen(f.colony.getID(), citizen.getUUID()).equals(CitizenDemand.empty()), "Death releases durable state");
                h.succeed();
            } finally { f.close(); }
        });
    }
}
