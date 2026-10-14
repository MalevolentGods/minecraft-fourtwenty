package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.DemandRules;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Persistence codec tests also run without any MineColonies classes on the runtime classpath. */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class DemandPersistenceGameTests {
    @net.minecraft.gametest.framework.GameTestGenerator
    public static java.util.Collection<net.minecraft.gametest.framework.TestFunction> diskCheckpoint() {
        String stage = System.getProperty("fourtwenty.persistenceStage", "");
        if (stage.isEmpty()) return java.util.List.of();
        return java.util.List.of(new net.minecraft.gametest.framework.TestFunction("dispensary_persistence",
                "demand_disk_" + stage, "fourtwenty_tests:empty", 100, 0, true, h -> {
            var data = DispensarySavedData.get(h.getLevel());
            var id = UUID.fromString("c8d5df27-550f-4429-b46b-33aafc46fcb1");
            int colony = Integer.MAX_VALUE - 1;
            var rules = DemandRules.defaults();
            if (stage.equals("write")) {
                h.assertTrue(data.citizen(colony, id).revision() == 0, "Fresh checkpoint required");
                data.advance();
                var initial = data.observe(colony, id, true, rules);
                h.assertTrue(data.recordConsumption(colony, id, initial.revision(), rules), "Write one consumption");
                h.getLevel().getDataStorage().save();
            } else {
                var paid = data.citizen(colony, id);
                h.assertTrue(paid.revision() == 2 && paid.lastConsumedAt() >= 1, "Fresh JVM restored completion token");
                h.assertTrue(paid.dueAt() == paid.lastConsumedAt() + 24_000 && paid.graceUntil() == paid.lastConsumedAt() + 48_000
                        && paid.healthUntil() == paid.lastConsumedAt() + 24_000, "All persisted deadlines retained");
                h.assertTrue(!data.recordConsumption(colony, id, 1, rules), "Old completion rejected after process restart");
                h.assertTrue(!paid.due(data.time()), "No immediate extra dose after restart");
            }
            h.succeed();
        }));
    }
    @GameTest(template = "empty")
    public static void durableConsumptionAndIdentityIsolation(GameTestHelper h) {
        var data = new DispensarySavedData();
        var id = UUID.randomUUID();
        var rules = DemandRules.defaults();
        data.advance();
        var before = data.observe(1, id, true, rules);
        h.assertTrue(data.recordConsumption(1, id, before.revision(), rules), "First completion accepted");
        var tag = data.save(new CompoundTag(), h.getLevel().registryAccess());
        var restored = DispensarySavedData.load(tag, h.getLevel().registryAccess());
        h.assertTrue(restored.time() == 1 && restored.citizen(1, id).equals(data.citizen(1, id)), "Clock and record round trip exactly");
        h.assertTrue(!restored.recordConsumption(1, id, before.revision(), rules), "Reload cannot replay completed consumption");
        h.assertTrue(!restored.citizen(2, id).eligible(), "Colony identity separates the same UUID");
        h.assertTrue(!restored.citizen(1, UUID.randomUUID()).eligible(), "Citizen UUID separates records");
        h.assertTrue(new DispensarySavedData().time() == 0, "Another world owns an independent clock");
        restored.advance();
        h.assertTrue(restored.citizen(1, id).healthActive(restored.time()), "Reload retains entitlement without re-awarding it");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void malformedAndFutureSavesFailClosed(GameTestHelper h) {
        var data = new DispensarySavedData();
        data.observe(1, UUID.randomUUID(), true, DemandRules.defaults());
        var good = data.save(new CompoundTag(), h.getLevel().registryAccess());
        var future = good.copy(); future.putInt("version", 2);
        rejects(h, future);
        var missing = good.copy(); missing.getList("citizens", 10).getCompound(0).remove("revision");
        rejects(h, missing);
        var duplicate = good.copy(); duplicate.getList("citizens", 10).add(duplicate.getList("citizens", 10).getCompound(0).copy());
        rejects(h, duplicate);
        h.succeed();
    }
    private static void rejects(GameTestHelper h, CompoundTag tag) {
        boolean rejected = false;
        try { DispensarySavedData.load(tag, h.getLevel().registryAccess()); }
        catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected, "Invalid saves are rejected rather than reset");
    }
}
