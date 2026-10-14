package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.jobs.JobFarmer;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFarmer;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class FarmerGameTests {
    /** Native farmer planting/recognition contract; field navigation and couriers need a separate live session. */
    @GameTest(template = "empty", batch = "farmer_contract")
    public static void nativeFarmerPlantsThreeCyclesFromRecoveredSeeds(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var hut = new BlockPos(1, 2, 1);
        var pos = h.absolutePos(hut);
        var colony = IColonyManager.getInstance().createColony(level, pos, FakePlayerFactory.getMinecraft(level), "Cannabis fixture", "Colonial");
        EntityCitizen actor = null;
        try {
            h.setBlock(hut, com.minecolonies.api.blocks.ModBlocks.blockHutFarmer);
            var building = colony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding) level.getBlockEntity(pos), level);
            building.setBuildingLevel(1);
            building.onUpgradeComplete(null, 1);
            var citizen = colony.getCitizenManager().createAndRegisterCivilianData();
            citizen.setIsChild(false);
            citizen.setSaturation(100);
            actor = (EntityCitizen) com.minecolonies.api.entity.ModEntities.CITIZEN.create(level);
            actor.setUUID(citizen.getUUID());
            actor.setCitizenId(citizen.getId());
            actor.getCitizenColonyHandler().setColonyId(colony.getID());
            actor.setPos(pos.getX() + 2, pos.getY(), pos.getZ() + 2);
            level.addFreshEntity(actor);
            actor.getCitizenColonyHandler().registerWithColony(colony.getID(), citizen.getId());
            actor.setNoAi(true);
            var module = building.getModules().stream().filter(WorkerBuildingModule.class::isInstance).map(WorkerBuildingModule.class::cast).findFirst().orElseThrow();
            h.assertTrue(module.assignCitizen(citizen), "Native farmer job assigned");
            var ai = new EntityAIWorkFarmer((JobFarmer) citizen.getJob());
            var plant = EntityAIWorkFarmer.class.getDeclaredMethod("plantCrop", ItemStack.class, BlockPos.class);
            plant.setAccessible(true);
            var surface = EntityAIWorkFarmer.class.getDeclaredMethod("findHarvestableSurface", BlockPos.class);
            surface.setAccessible(true);
            var soil = new BlockPos(3, 1, 3);
            var crop = soil.above();
            h.setBlock(soil, Blocks.FARMLAND);
            h.setBlock(crop.above(), Blocks.GLOWSTONE);
            actor.getInventoryCitizen().setStackInSlot(0, new ItemStack(ModItems.WEED_SEEDS.get()));
            for (int cycle = 0; cycle < 3; cycle++) {
                h.assertTrue((boolean) plant.invoke(ai, new ItemStack(ModItems.WEED_SEEDS.get()), h.absolutePos(soil)), "Native farmer accepts seed");
                h.assertTrue(h.getBlockState(crop).is(ModBlocks.WEED_CROP.get()), "Native planting creates cannabis");
                h.assertTrue(actor.getInventoryCitizen().getStackInSlot(0).isEmpty(), "Planting consumes its one seed");
                h.setBlock(crop, ModBlocks.WEED_CROP.get().defaultBlockState().setValue(CropBlock.AGE, 7));
                h.assertTrue(surface.invoke(ai, h.absolutePos(soil)) != null, "Farmer recognizes mature cannabis as harvestable");
                var drops = net.minecraft.world.level.block.Block.getDrops(h.getBlockState(crop), level, h.absolutePos(crop), null, actor, ItemStack.EMPTY);
                h.setBlock(crop, Blocks.AIR);
                // Deliver the native loot to its worker inventory, without injecting new seed stock.
                for (var drop : drops) {
                    var remaining = drop.copy();
                    for (int slot = 0; slot < actor.getInventoryCitizen().getSlots() && !remaining.isEmpty(); slot++)
                        remaining = actor.getInventoryCitizen().insertItem(slot, remaining, false);
                    h.assertTrue(remaining.isEmpty(), "Worker inventory collects every drop");
                }
                // Locate seed stock without assuming the inventory's sort order.
                int slot = actor.getCitizenInventoryHandler().findFirstSlotInInventoryWith(ModItems.WEED_SEEDS.get());
                h.assertTrue(slot >= 0, "Harvest retains seed for next cycle");
                if (slot != 0) {
                    var first = actor.getInventoryCitizen().getStackInSlot(0).copy();
                    var seeds = actor.getInventoryCitizen().getStackInSlot(slot).copy();
                    actor.getInventoryCitizen().setStackInSlot(slot, first);
                    actor.getInventoryCitizen().setStackInSlot(0, seeds);
                }
                // Extra seeds are legitimate rare loot; native planting must deduct exactly one.
                if (cycle < 2 && actor.getInventoryCitizen().getStackInSlot(0).getCount() > 1) {
                    var extra = actor.getInventoryCitizen().extractItem(0, actor.getInventoryCitizen().getStackInSlot(0).getCount() - 1, false);
                    actor.getInventoryCitizen().insertItem(actor.getInventoryCitizen().getSlots() - 1, extra, false);
                }
            }
            h.succeed();
        } finally {
            if (actor != null) actor.discard();
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, level);
        }
    }
}
