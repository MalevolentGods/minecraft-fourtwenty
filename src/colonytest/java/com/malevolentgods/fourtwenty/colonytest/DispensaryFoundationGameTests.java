package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.ColonyIntegration;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.DemandRules;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.colony.buildings.modules.GuardBuildingModule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class DispensaryFoundationGameTests {
    @GameTest(template = "empty", batch = "dispensary_foundation")
    public static void nativeHomeAndGuardHousing(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        var colony = IColonyManager.getInstance().createColony(level, pos, FakePlayerFactory.getMinecraft(level), "Dispensary rules", "Colonial");
        try {
            var citizen = colony.getCitizenManager().createAndRegisterCivilianData();
            citizen.setIsChild(false);
            level.setBlockAndUpdate(pos, com.minecolonies.api.blocks.ModBlocks.blockHutHome.defaultBlockState());
            var home = colony.getServerBuildingManager().addNewBuilding(
                    (com.minecolonies.core.tileentities.TileEntityColonyBuilding) level.getBlockEntity(pos), level);
            var workPos = pos.offset(2, 0, 0);
            level.setBlockAndUpdate(workPos, com.minecolonies.api.blocks.ModBlocks.blockHutFarmer.defaultBlockState());
            var work = colony.getServerBuildingManager().addNewBuilding(
                    (com.minecolonies.core.tileentities.TileEntityColonyBuilding) level.getBlockEntity(workPos), level);
            work.setBuildingLevel(5);
            var workers = work.getModules().stream().filter(com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule.class::isInstance)
                    .map(com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule.class::cast).findFirst().orElseThrow();
            h.assertTrue(workers.assignCitizen(citizen), "Native farmer hiring succeeds");
            h.assertTrue(!ColonyIntegration.eligible(citizen), "High workplace does not replace a missing home");
            citizen.setHomeBuilding(home);
            for (int completed = 0; completed <= 5; completed++) {
                home.setBuildingLevel(completed);
                h.assertTrue(ColonyIntegration.eligible(citizen) == (completed >= 3), "Native completed home level " + completed);
            }
            home.setBuildingLevel(2);
            home.setBlueprintPath("wooden/residence3.blueprint");
            h.assertTrue(!ColonyIntegration.eligible(citizen), "Planned upgrade path does not change completed level");
            home.setBuildingLevel(3);
            citizen.setIsChild(true);
            h.assertTrue(!ColonyIntegration.eligible(citizen), "Native child excluded");
            citizen.setIsChild(false);
            citizen.setHomeBuilding(null);
            workers.removeCitizen(citizen);
            var towerPos = pos.offset(4, 0, 0);
            level.setBlockAndUpdate(towerPos, com.minecolonies.api.blocks.ModBlocks.blockHutGuardTower.defaultBlockState());
            var tower = colony.getServerBuildingManager().addNewBuilding(
                    (com.minecolonies.core.tileentities.TileEntityColonyBuilding) level.getBlockEntity(towerPos), level);
            tower.setBuildingLevel(3);
            tower.onUpgradeComplete(null, 3);
            var guards = tower.getModules().stream().filter(GuardBuildingModule.class::isInstance)
                    .map(GuardBuildingModule.class::cast).findFirst().orElseThrow();
            h.assertTrue(guards.assignCitizen(citizen), "Native guard hiring succeeds");
            h.assertTrue(citizen.getHomeBuilding() == tower, "Guard work-at-home module assigns actual home");
            h.assertTrue(ColonyIntegration.eligible(citizen), "Completed level 3 guard home qualifies");
            tower.setBuildingLevel(2);
            h.assertTrue(!ColonyIntegration.eligible(citizen), "Level 2 guard home does not qualify");
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, level);
        }
    }
    @GameTest(template = "empty", batch = "dispensary_clock")
    public static void daylightJumpsCannotAdvanceDemandClock(GameTestHelper h) {
        var level = h.getLevel();
        var data = DispensarySavedData.get(level);
        long before = data.time();
        long originalDay = level.getDayTime();
        var id = java.util.UUID.randomUUID();
        var state = data.observe(Integer.MAX_VALUE, id, true, DemandRules.defaults());
        h.assertTrue(data.recordConsumption(Integer.MAX_VALUE, id, state.revision(), DemandRules.defaults()), "Paid checkpoint");
        // Sleep and /time alter the world's day time; neither is a simulation-clock input.
        level.setDayTime(24_000 * 100L);
        level.setDayTime(0);
        level.setDayTime(originalDay);
        h.assertTrue(data.time() == before, "Forward and backward day-time jumps add no elapsed ticks");
        h.runAfterDelay(5, () -> {
            h.assertTrue(data.time() == before + 5, "Exactly five server ticks elapse");
            h.assertTrue(!data.citizen(Integer.MAX_VALUE, id).due(data.time()), "No midnight bonus dose");
            h.succeed();
        });
    }
}
