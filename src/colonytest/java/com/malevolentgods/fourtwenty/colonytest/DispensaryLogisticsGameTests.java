package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.*;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.buildings.modules.CourierAssignmentModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingWareHouse;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Real native navigation and delivery in a disposable, chunk-loaded fixture; no actor teleportation after setup. */
@GameTestHolder("fourtwenty_logisticstests")
@PrefixGameTestTemplate(false)
public final class DispensaryLogisticsGameTests {
    private static java.util.List<ItemStack> contents(net.neoforged.neoforge.items.IItemHandler handler) {
        return java.util.stream.IntStream.range(0,handler.getSlots()).mapToObj(handler::getStackInSlot).filter(stack->!stack.isEmpty()).toList();
    }
    @GameTest(template="production",timeoutTicks=12000,batch="dispensary_logistics")
    public static void courierSuppliesAutonomousBudtender(GameTestHelper h) throws Exception {
        var world=h.getLevel();
        var origin=h.absolutePos(new BlockPos(0,1,0));
        for(int x=0;x<32;x++) for(int z=0;z<24;z++) world.setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        var forced=new java.util.HashSet<net.minecraft.world.level.ChunkPos>();
        for(int x=origin.getX()>>4;x<=(origin.getX()+31)>>4;x++) for(int z=origin.getZ()>>4;z<=(origin.getZ()+23)>>4;z++) {
            var chunk=new net.minecraft.world.level.ChunkPos(x,z);
            if(!world.getForcedChunks().contains(chunk.toLong())) {world.setChunkForced(x,z,true);forced.add(chunk);}
        }
        var pos=origin.offset(7,0,6);
        var colony=IColonyManager.getInstance().createColony(world,pos,FakePlayerFactory.getMinecraft(world),"Dispensary logistics","Sensi Heights");
        try(var input=DispensaryBlock.class.getResourceAsStream("/blueprints/fourtwenty/sensiheights/craftsmanship/luxury/dispensary1.blueprint")) {
            var blueprint=com.ldtteam.structurize.blueprints.v1.BlueprintUtil.readBlueprintFromNBT(
                    net.minecraft.nbt.NbtIo.readCompressed(input,net.minecraft.nbt.NbtAccounter.unlimitedHeap()),world.registryAccess());
            var start=pos.subtract(blueprint.getPrimaryBlockOffset());
            for(var info:blueprint.getBlockInfoAsList()) {
                var at=start.offset(info.getPos());world.setBlockAndUpdate(at,info.getState());
                if(info.hasTileEntityData() && world.getBlockEntity(at)!=null) world.getBlockEntity(at).loadWithComponents(info.getTileEntityData(),world.registryAccess());
            }
        }
        var hut=(DispensaryBuilding)colony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding)world.getBlockEntity(pos),world);
        hut.setBuildingLevel(1);hut.onUpgradeComplete(null,1);
        // This fixture covers production logistics, not customer sessions. A visiting courier would
        // otherwise count as a customer and move one reserved bud into the service bong.
        for(var at:BlockPos.betweenClosed(hut.getCorners().getA(),hut.getCorners().getB())) {
            if(world.getBlockEntity(at) instanceof com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity) {
                world.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
            }
        }
        ItemHandlerHelper.insertItemStacked(hut.getItemHandlerCap(),new ItemStack(ModItems.WEED_BUD.get(),4),false);
        hut.getModule(DispensaryRegistry.WORK).setHiringMode(com.minecolonies.api.colony.buildings.HiringMode.MANUAL);
        var warehousePos=origin.offset(22,0,6);
        world.setBlockAndUpdate(warehousePos,com.minecolonies.api.blocks.ModBlocks.blockHutWareHouse.defaultBlockState());
        var warehouse=(BuildingWareHouse)colony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding)world.getBlockEntity(warehousePos),world);
        warehouse.setBuildingLevel(1);warehouse.onUpgradeComplete(null,1);
        var rackPos=warehousePos.offset(0,0,2);
        world.setBlockAndUpdate(rackPos,com.minecolonies.api.blocks.ModBlocks.blockRack.defaultBlockState());
        warehouse.registerBlockPosition(com.minecolonies.api.blocks.ModBlocks.blockRack,rackPos,world);
        var rack=(com.minecolonies.core.tileentities.TileEntityRack)world.getBlockEntity(rackPos);
        ItemHandlerHelper.insertItemStacked(rack.getItemHandlerCap(),new ItemStack(ModItems.WEED_BUD.get(),16),false);
        ItemHandlerHelper.insertItemStacked(rack.getItemHandlerCap(),new ItemStack(Items.PAPER,32),false);
        var courierPos=origin.offset(22,0,13);
        world.setBlockAndUpdate(courierPos,com.minecolonies.api.blocks.ModBlocks.blockHutDeliveryman.defaultBlockState());
        var courierHut=colony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding)world.getBlockEntity(courierPos),world);
        courierHut.setBuildingLevel(1);courierHut.onUpgradeComplete(null,1);
        var worker=DispensaryProductionGameTests.actor(h,colony,pos.offset(0,0,-2));
        var courier=DispensaryProductionGameTests.actor(h,colony,courierPos.offset(0,0,-1));
        hut.getModule(DispensaryRegistry.WORK).assignCitizen(worker.getCitizenData());
        var courierWork=courierHut.getFirstModuleOccurance(WorkerBuildingModule.class);
        courierWork.setHiringMode(com.minecolonies.api.colony.buildings.HiringMode.MANUAL);
        h.assertTrue(courierWork.assignCitizen(courier.getCitizenData()),"Courier hired");
        h.assertTrue(warehouse.getFirstModuleOccurance(CourierAssignmentModule.class).assignCitizen(courier.getCitizenData()),"Courier assigned to warehouse");
        worker.setNoAi(false);courier.setNoAi(false);
        long originalTime=world.getDayTime();
        boolean daylight=world.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
        boolean weatherCycle=world.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE);
        world.setDayTime(6000);world.setWeatherParameters(0,24000,false,false);
        world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,world.getServer());
        world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false,world.getServer());
        var startWorker=worker.position();var startCourier=courier.position();
        var moved=new boolean[2];
        h.onEachTick(()->{
            // A GameTest has no real visiting player: explicitly tick the native building/request services.
            if(h.getTick()%20==0) {hut.onColonyTick(colony);warehouse.onColonyTick(colony);courierHut.onColonyTick(colony);}
            colony.getRequestManager().tick();
            moved[0]|=worker.position().distanceTo(startWorker)>2;
            moved[1]|=courier.position().distanceTo(startCourier)>3;
            int joints=InventoryUtils.getItemCountInItemHandler(hut.getItemHandlerCap(),s->s.is(ModItems.WEED_JOINT.get()));
            if(h.getTick()%1000==0) com.mojang.logging.LogUtils.getLogger().info("DISPENSARY_LOGISTICS tick={} joints={} worker={} courier={} workerState={} courierState={} requests={}",h.getTick(),joints,worker.position(),courier.position(),worker.getCitizenAI().getState(),courier.getCitizenAI().getState(),DispensaryProductionGameTests.requests(hut));
            if(h.getTick()%1000==0) com.mojang.logging.LogUtils.getLogger().info("DISPENSARY_DETAIL level={} workers={} recipes={} total={} workerInv={} workAI={} courierAI={} courierInv={} hutInv={} rack={}",hut.getBuildingLevel(),hut.getAllAssignedCitizen().size(),hut.getModule(DispensaryRegistry.CRAFT).getRecipes().size(),InventoryUtils.getCountFromBuilding(hut,s->s.is(ModItems.WEED_JOINT.get())),contents(worker.getInventoryCitizen()),((BudtenderJob)worker.getCitizenData().getJob()).getWorkerAI().getState(),((com.minecolonies.core.colony.jobs.JobDeliveryman)courier.getCitizenData().getJob()).getWorkerAI().getState(),contents(courier.getInventoryCitizen()),contents(hut.getItemHandlerCap()),contents(rack.getItemHandlerCap()));
            int openRequests=(int)hut.getOpenRequestsByRequestableType().values().stream().flatMap(java.util.Collection::stream).distinct().count();
            boolean suppliesInTransit=InventoryUtils.getItemCountInItemHandler(rack.getItemHandlerCap(),s->s.is(ModItems.WEED_BUD.get())||s.is(Items.PAPER))>0
                    || InventoryUtils.getItemCountInItemHandler(courier.getInventoryCitizen(),s->s.is(ModItems.WEED_BUD.get())||s.is(Items.PAPER))>0;
            if(joints>=16 && openRequests==0 && !suppliesInTransit) {
                h.assertTrue(moved[0] && moved[1],"Both workers navigated naturally");
                h.assertTrue(InventoryUtils.getItemCountInItemHandler(rack.getItemHandlerCap(),s->s.is(ModItems.WEED_BUD.get())||s.is(Items.PAPER))==0,"All 16 warehouse buds + 32 paper were delivered");
                int leftoverBuds=0,leftoverPaper=0;
                for(var handler:hut.getHandlers()) {
                    leftoverBuds+=InventoryUtils.getItemCountInItemHandler(handler,s->s.is(ModItems.WEED_BUD.get()));
                    leftoverPaper+=InventoryUtils.getItemCountInItemHandler(handler,s->s.is(Items.PAPER));
                }
                leftoverBuds+=InventoryUtils.getItemCountInItemHandler(courier.getInventoryCitizen(),s->s.is(ModItems.WEED_BUD.get()));
                leftoverPaper+=InventoryUtils.getItemCountInItemHandler(courier.getInventoryCitizen(),s->s.is(Items.PAPER));
                h.assertTrue(leftoverBuds==4 && leftoverPaper==0,"16 buds became joints and the 4-bud reserve remains; leftoverBuds="+leftoverBuds+" leftoverPaper="+leftoverPaper);
                com.mojang.logging.LogUtils.getLogger().info("DISPENSARY_LOGISTICS PASS: 4 reserve buds plus 16 warehouse buds and 32 paper produced and stored 16 joints with a 4-bud reserve at tick {}",h.getTick());
                worker.discard();courier.discard();IColonyManager.getInstance().deleteColonyByWorld(colony.getID(),false,world);
                forced.forEach(c->world.setChunkForced(c.x,c.z,false));
                world.setDayTime(originalTime);
                world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(daylight,world.getServer());
                world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(weatherCycle,world.getServer());
                h.succeed();
            }
        });
    }
}
