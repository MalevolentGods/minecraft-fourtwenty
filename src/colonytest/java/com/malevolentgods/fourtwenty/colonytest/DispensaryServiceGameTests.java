package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.service.*;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.colony.buildings.modules.GuardBuildingModule;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import java.util.*;
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

@GameTestHolder("fourtwenty_servicetests")
@PrefixGameTestTemplate(false)
public final class DispensaryServiceGameTests {
    private static CitizenSmokingTask task(EntityCitizen entity) { return ((DispensaryCustomer)entity).fourtwenty$smokingTask(); }
    private static int joints(net.neoforged.neoforge.items.IItemHandler inventory) {
        return InventoryUtils.getItemCountInItemHandler(inventory, s -> s.is(ModItems.WEED_JOINT.get()));
    }
    private static long consumed(EntityCitizen entity) {
        return DispensarySavedData.get((net.minecraft.server.level.ServerLevel)entity.level())
                .citizen(entity.getCitizenData().getColony().getID(),entity.getUUID()).lastConsumedAt();
    }
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final IColony colony;
        final BlockPos origin;
        final List<EntityCitizen> actors = new ArrayList<>();
        final Set<net.minecraft.world.level.ChunkPos> forced = new HashSet<>();
        final long originalTime;
        final boolean daylight;
        Fixture(GameTestHelper h) {
            this.h=h; var world=h.getLevel(); origin=h.absolutePos(new BlockPos(0,1,0));
            originalTime=world.getDayTime(); daylight=world.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
            world.setDayTime(6000); world.setWeatherParameters(0,24000,false,false);
            world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,world.getServer());
            for(int x=0;x<32;x++) for(int z=0;z<24;z++) world.setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int x=origin.getX()>>4;x<=(origin.getX()+31)>>4;x++) for(int z=origin.getZ()>>4;z<=(origin.getZ()+23)>>4;z++) {
                var chunk=new net.minecraft.world.level.ChunkPos(x,z);
                if(!world.getForcedChunks().contains(chunk.toLong())) {world.setChunkForced(x,z,true);forced.add(chunk);}
            }
            colony=IColonyManager.getInstance().createColony(world,origin.offset(12,0,8),FakePlayerFactory.getMinecraft(world),"Dispensary service","FourTwenty");
        }
        IBuilding building(net.minecraft.world.level.block.Block block, int x,int z,int level) {
            var pos=origin.offset(x,0,z);h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
            var b=colony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding)h.getLevel().getBlockEntity(pos),h.getLevel());
            b.setBuildingLevel(level);b.onUpgradeComplete(null,level);return b;
        }
        EntityCitizen actor(int x,int z,IBuilding home) {
            var e=DispensaryProductionGameTests.actor(h,colony,origin.offset(x,0,z));
            e.getCitizenData().setHomeBuilding(home); actors.add(e);return e;
        }
        DispensaryBuilding hut(int x,int z) {
            var b=DispensaryProductionGameTests.hut(h,colony,origin.offset(x,0,z));
            b.setBuildingLevel(1);b.onUpgradeComplete(null,1);return b;
        }
        DispensaryBuilding blueprint() throws Exception {
            var pos=origin.offset(12,0,8);
            try(var input=DispensaryBlock.class.getResourceAsStream("/blueprints/fourtwenty/dispensary/dispensary/dispensary1.blueprint")) {
                var blueprint=com.ldtteam.structurize.blueprints.v1.BlueprintUtil.readBlueprintFromNBT(
                        net.minecraft.nbt.NbtIo.readCompressed(input,net.minecraft.nbt.NbtAccounter.unlimitedHeap()),h.getLevel().registryAccess());
                var start=pos.subtract(blueprint.getPrimaryBlockOffset());
                for(var info:blueprint.getBlockInfoAsList()) {
                    var at=start.offset(info.getPos());h.getLevel().setBlockAndUpdate(at,info.getState());
                    if(info.hasTileEntityData() && h.getLevel().getBlockEntity(at)!=null) h.getLevel().getBlockEntity(at).loadWithComponents(info.getTileEntityData(),h.getLevel().registryAccess());
                }
            }
            var hut=(DispensaryBuilding)colony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding)h.getLevel().getBlockEntity(pos),h.getLevel());
            hut.setBuildingLevel(1);hut.onUpgradeComplete(null,1);
            hut.getModule(DispensaryRegistry.WORK).setHiringMode(com.minecolonies.api.colony.buildings.HiringMode.MANUAL);return hut;
        }
        void stock(DispensaryBuilding b,int count) {
            h.assertTrue(ItemHandlerHelper.insertItemStacked(b.getItemHandlerCap(),new ItemStack(ModItems.WEED_JOINT.get(),count),false).isEmpty(),"Fixture stock fits");
        }
        @Override public void close() {
            actors.forEach(EntityCitizen::discard);IColonyManager.getInstance().deleteColonyByWorld(colony.getID(),false,h.getLevel());
            forced.forEach(c->h.getLevel().setChunkForced(c.x,c.z,false));
            h.getLevel().setDayTime(originalTime);
            h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(daylight,h.getLevel().getServer());
        }
    }

    @GameTest(template="production",batch="service_transactions")
    public static void lastJointFullInventoryAndRevisionConservation(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var hut=f.hut(12,8);var home=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome,24,20,3);
            var first=f.actor(12,7,home);var second=f.actor(13,7,home);f.stock(hut,1);
            for(int slot=0;slot<first.getInventoryCitizen().getSlots();slot++) first.getInventoryCitizen().setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));
            h.assertTrue(!JointService.collect(first,hut,hut.getItemHandlerCap()) && joints(hut.getItemHandlerCap())==1,"Full inventory cannot delete last joint");
            first.getInventoryCitizen().setStackInSlot(0,ItemStack.EMPTY);
            h.assertTrue(JointService.collect(first,hut,hut.getItemHandlerCap()),"First customer receives exactly one");
            h.assertTrue(!JointService.collect(second,hut,hut.getItemHandlerCap()),"Competing customer cannot receive last joint twice");
            h.assertTrue(consumed(first)==-1 && joints(first.getInventoryCitizen())==1,"Collection has no consumption credit");
            var saved=DispensarySavedData.get(h.getLevel());var before=saved.citizen(f.colony.getID(),first.getUUID());
            double saturation=first.getCitizenData().getSaturation();
            h.assertTrue(JointService.consume(first,before.revision()),"Validated completion consumes one");
            h.assertTrue(!JointService.consume(first,before.revision()) && joints(first.getInventoryCitizen())==0,"Repeated callback cannot consume or credit twice");
            h.assertTrue(first.getCitizenData().getSaturation()==saturation,"Cannabis does not change nutrition");
            f.stock(hut,1);second.getCitizenData().setIsChild(true);
            h.assertTrue(!JointService.collect(second,hut,hut.getItemHandlerCap()),"Child rejected at handoff");
            second.getCitizenData().setIsChild(false);home.setBuildingLevel(2);
            h.assertTrue(!JointService.collect(second,hut,hut.getItemHandlerCap()),"Downgraded home rejected at handoff");
            home.setBuildingLevel(3);second.setPos(f.origin.getX()+25,f.origin.getY(),f.origin.getZ()+15);
            h.assertTrue(!JointService.collect(second,hut,hut.getItemHandlerCap()),"Remote collection rejected");
            h.assertTrue(joints(hut.getItemHandlerCap())==1,"Rejected handoffs preserve stock");
            h.succeed();
        }
    }

    @GameTest(template="production",batch="service_autonomous",timeoutTicks=4000)
    public static void employedUnemployedAndGuardVisitAndReturn(GameTestHelper h) throws Exception {
        var f=new Fixture(h);var hut=f.blueprint();f.stock(hut,3);
        var home=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome,24,20,3);
        var farmer=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutFarmer,4,18,1);
        var tower=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutGuardTower,24,4,3);
        var worker=f.actor(12,7,null);hut.getModule(DispensaryRegistry.WORK).assignCitizen(worker.getCitizenData());
        var employed=f.actor(4,13,home);farmer.getFirstModuleOccurance(WorkerBuildingModule.class).assignCitizen(employed.getCitizenData());
        var idle=f.actor(4,6,home);var guard=f.actor(24,6,null);tower.getFirstModuleOccurance(GuardBuildingModule.class).assignCitizen(guard.getCitizenData());
        var child=f.actor(3,3,home);child.getCitizenData().setIsChild(true);
        var lowHome=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome,27,18,2);var low=f.actor(3,4,lowHome);
        var customers=List.of(employed,idle,guard);
        Map<UUID,net.minecraft.world.phys.Vec3> start=new HashMap<>();customers.forEach(e->start.put(e.getUUID(),e.position()));
        Set<UUID> moved=new HashSet<>(),returned=new HashSet<>(),smoked=new HashSet<>(),served=new HashSet<>();boolean[] seated={false};
        Map<UUID,Long> waitStarted=new HashMap<>();
        f.actors.forEach(e->e.setNoAi(false));
        h.onEachTick(()->{
            for(var e:customers) {
                if(task(e)==null) continue;
                if(task(e).step()==CitizenSmokingTask.Step.WAIT)waitStarted.putIfAbsent(e.getUUID(),h.getTick());
                if(waitStarted.containsKey(e.getUUID()) && joints(e.getInventoryCitizen())>0
                        && h.getTick()-waitStarted.get(e.getUUID())<CitizenSmokingTask.WAIT_TICKS-5)served.add(e.getUUID());
                if(e.position().distanceTo(start.get(e.getUUID()))>3)moved.add(e.getUUID());
                if(task(e).step()==CitizenSmokingTask.Step.SMOKE)smoked.add(e.getUUID());
                seated[0]|=e.getVehicle() instanceof com.minecolonies.core.entity.other.SittingEntity;
                if(consumed(e)>=0 && task(e).step()==CitizenSmokingTask.Step.INACTIVE
                        && e.getCitizenAI().getState()!=CitizenSmokingTask.State.VISIT)returned.add(e.getUUID());
            }
            if(h.getTick()%400==0)com.mojang.logging.LogUtils.getLogger().info("DISPENSARY_SERVICE tick={} actors={}",h.getTick(),customers.stream().map(e->e.getCitizenData().getJob()+":"+e.position()+":"+e.getCitizenAI().getState()+":"+(task(e)==null?"INIT":task(e).step())+":"+consumed(e)).toList());
            if(returned.size()==3) {
                h.assertTrue(moved.size()==3 && smoked.size()==3,"All three adults navigated and smoked autonomously");
                h.assertTrue(seated[0],"Native blueprint seat used");
                h.assertTrue(!served.isEmpty(),"Budtender served a customer before the self-service deadline");
                h.assertTrue(customers.stream().allMatch(e->joints(e.getInventoryCitizen())==0),"Exactly one joint per completed action");
                h.assertTrue(joints(hut.getItemHandlerCap())+joints(worker.getInventoryCitizen())==0,"Three supplied joints conserved");
                h.assertTrue(consumed(child)==-1 && consumed(low)==-1,"Child and level-two resident do not participate");
                com.mojang.logging.LogUtils.getLogger().info("DISPENSARY_SERVICE PASS employed/unemployed/guard, seating, conservation and return at tick {}",h.getTick());
                f.close();h.succeed();
            }
        });
    }

    @GameTest(template="production",batch="service_interrupt",timeoutTicks=2000)
    public static void interruptedSmokingRetainsJointAndCleansUp(GameTestHelper h) {
        var f=new Fixture(h);var home=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome,24,20,3);
        var actor=f.actor(4,6,home);actor.getInventoryCitizen().setStackInSlot(0,new ItemStack(ModItems.WEED_JOINT.get(),2));
        actor.setNoAi(false);boolean[] interrupted={false};long[] restart={-1};
        h.onEachTick(()->{
            if(task(actor)==null) return;
            if(!interrupted[0] && task(actor).step()==CitizenSmokingTask.Step.SMOKE && !actor.getMainHandItem().isEmpty()) {
                actor.getCitizenData().setSaturation(0);interrupted[0]=true;
            }
            if(interrupted[0] && restart[0]<0 && task(actor).step()==CitizenSmokingTask.Step.INACTIVE) {
                h.assertTrue(joints(actor.getInventoryCitizen())==2 && consumed(actor)==-1,"Interrupted smoke neither consumes nor credits");
                h.assertTrue(actor.getMainHandItem().isEmpty() && !actor.isPassenger(),"Held visual and seat cleared");
                actor.getCitizenData().setSaturation(100);actor.getCitizenData().setJustAte(true);restart[0]=h.getTick();
            }
            if(consumed(actor)>=0) {
                h.assertTrue(interrupted[0] && h.getTick()-restart[0]>=CitizenSmokingTask.RETRY_TICKS,"Bounded retry then fresh timed action: tick="+h.getTick()+" restart="+restart[0]);
                h.assertTrue(joints(actor.getInventoryCitizen())==1,"Carried stack consumes exactly one, without a dispensary");
                f.close();h.succeed();
            }
        });
    }

    @GameTest(template="production",batch="service_fallback",timeoutTicks=1800)
    public static void noStaffNoSeatsFullInventoryAndEmptyStockRelease(GameTestHelper h) {
        var f=new Fixture(h);var home=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome,24,20,3);
        var empty=f.hut(4,8);var stocked=f.hut(16,8);f.stock(stocked,2);
        var customer=f.actor(7,8,home);var full=f.actor(16,6,home);
        for(int slot=0;slot<full.getInventoryCitizen().getSlots();slot++)full.getInventoryCitizen().setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));
        customer.setNoAi(false);full.setNoAi(false);boolean[] fullVisited={false},fullReleased={false};
        h.onEachTick(()->{
            if(task(full)==null || task(customer)==null) return;
            if(h.getTick()%200==0) com.mojang.logging.LogUtils.getLogger().info("SERVICE_FALLBACK tick={} customer={}:{}:{} full={}:{} available={}/{} due={}/{}",h.getTick(),customer.position(),task(customer).step(),consumed(customer),full.position(),task(full).step(),CitizenSmokingTask.available(customer),CitizenSmokingTask.available(full),JointService.due(customer),JointService.due(full));
            fullVisited[0]|=task(full).step()!=CitizenSmokingTask.Step.INACTIVE;
            fullReleased[0]|=fullVisited[0] && task(full).step()==CitizenSmokingTask.Step.INACTIVE;
            if(consumed(customer)>=0 && fullReleased[0]) {
                h.assertTrue(joints(stocked.getItemHandlerCap())==1 && joints(empty.getItemHandlerCap())==0,"Stocked alternative serves exactly one without staff/seats");
                h.assertTrue(consumed(full)==-1 && joints(full.getInventoryCitizen())==0,"Full customer released without item loss or benefits");
                f.close();h.succeed();
            }
        });
    }

    @GameTest(template="production",batch="service_removal",timeoutTicks=1500)
    public static void removalDuringSmokingAndUnreachableVisitRelease(GameTestHelper h) {
        var f=new Fixture(h);var home=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome,24,20,3);
        var hut=f.hut(12,8);f.stock(hut,2);var removed=f.actor(12,6,home);var blocked=f.actor(3,18,home);
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) for(int y=0;y<3;y++) {
            if(x!=0 || z!=0)h.getLevel().setBlockAndUpdate(blocked.blockPosition().offset(x,y,z),Blocks.BEDROCK.defaultBlockState());
        }
        removed.setNoAi(false);blocked.setNoAi(false);boolean[] demolished={false},visited={false},released={false};
        h.onEachTick(()->{
            if(task(blocked)==null || task(removed)==null) return;
            if(h.getTick()%200==0) com.mojang.logging.LogUtils.getLogger().info("SERVICE_REMOVAL tick={} actor={}:{}:{} blocked={}:{}",h.getTick(),removed.position(),task(removed).step(),consumed(removed),blocked.position(),task(blocked).step());
            visited[0]|=task(blocked).step()==CitizenSmokingTask.Step.TRAVEL;
            if(!demolished[0] && task(removed).step()==CitizenSmokingTask.Step.SMOKE) {
                h.getLevel().setBlockAndUpdate(hut.getPosition(),Blocks.AIR.defaultBlockState());demolished[0]=true;
            }
            released[0]|=visited[0] && task(blocked).step()==CitizenSmokingTask.Step.INACTIVE;
            if(demolished[0] && released[0] && task(removed).step()==CitizenSmokingTask.Step.INACTIVE) {
                h.assertTrue(joints(removed.getInventoryCitizen())==1 && consumed(removed)==-1,"Removed hut interrupts smoke and keeps transferred joint");
                h.assertTrue(consumed(blocked)==-1 && joints(blocked.getInventoryCitizen())==0,"Unreachable actor released without remote collection");
                f.close();h.succeed();
            }
        });
    }

    @GameTest(template="production",batch="service_priorities")
    public static void nativePrioritiesAndSeatReservations(GameTestHelper h) throws Exception {
        try(var f=new Fixture(h)) {
            var hut=f.blueprint();var home=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome,24,20,3);
            var one=f.actor(12,7,home);var two=f.actor(13,7,home);var three=f.actor(11,7,home);
            long now=DispensarySavedData.get(h.getLevel()).time();
            var a=hut.reserveSeat(one,now);var b=hut.reserveSeat(two,now);
            h.assertTrue(a!=null && b!=null && !a.equals(b) && hut.reserveSeat(three,now)==null,"Two distinct reservations; occupied seating falls back");
            hut.releaseSeat(one.getUUID());h.assertTrue(hut.reserveSeat(three,now).equals(a),"Interrupted reservation released");
            h.assertTrue(hut.reserveSeat(one,now+41)!=null,"Unloaded customer lease expires");
            hut.releaseSeat(one.getUUID());hut.releaseSeat(two.getUUID());hut.releaseSeat(three.getUUID());
            h.assertTrue(com.minecolonies.core.entity.other.SittingEntity.sitDown(a,two,100),"Native occupied seat");
            h.assertTrue(!hut.reserveSeat(three,now+41).equals(a),"Native occupied seat excluded");
            two.stopRiding();
            one.getInventoryCitizen().setStackInSlot(0,new ItemStack(ModItems.WEED_JOINT.get()));
            new com.minecolonies.core.entity.ai.workers.CitizenAI(one);
            var foreignState=new com.minecolonies.api.entity.ai.statemachine.states.IState() {};
            for(var priority:List.of(CitizenAIState.EATING,CitizenAIState.SICK,CitizenAIState.SLEEP,foreignState)) {
                h.assertTrue(task(one).choose(priority)==priority,"Essential and foreign ritual decisions remain in control");
            }
            one.getCitizenData().setSaturation(0);
            h.assertTrue(task(one).choose(CitizenAIState.WORK)==CitizenAIState.WORK,"Hungry guard-like work decision cannot begin visit");
            one.getCitizenData().setSaturation(100);
            h.assertTrue(task(one).choose(CitizenAIState.IDLE)==CitizenSmokingTask.State.VISIT,"Personal stock begins timed action");
            task(one).choose(foreignState);
            h.assertTrue(task(one).step()==CitizenSmokingTask.Step.INACTIVE && joints(one.getInventoryCitizen())==1 && consumed(one)==-1,"Foreign decision preempts without unearned credit");
            h.succeed();
        }
    }

    @GameTest(template="production",batch="service_unreachable",timeoutTicks=3600)
    public static void unreachableHutTimesOutThenTriesAnother(GameTestHelper h) {
        var f=new Fixture(h);var home=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome,26,20,3);
        var inaccessible=f.hut(12,8);var accessible=f.hut(24,16);f.stock(inaccessible,1);f.stock(accessible,1);
        for(int x=8;x<=16;x++)for(int z=4;z<=12;z++)for(int y=0;y<3;y++) {
            if(x==8 || x==16 || z==4 || z==12)h.getLevel().setBlockAndUpdate(f.origin.offset(x,y,z),Blocks.BEDROCK.defaultBlockState());
        }
        var actor=f.actor(5,8,home);actor.setNoAi(false);boolean[] visited={false},released={false};long[] releasedAt={-1};
        h.onEachTick(()->{
            if(task(actor)==null)return;
            visited[0]|=task(actor).step()==CitizenSmokingTask.Step.TRAVEL;
            if(visited[0] && !released[0] && task(actor).step()==CitizenSmokingTask.Step.INACTIVE) {
                released[0]=true;releasedAt[0]=h.getTick();
                h.assertTrue(consumed(actor)==-1 && joints(actor.getInventoryCitizen())==0,"Unreachable visit releases without collection");
                h.assertTrue(h.getTick()>=CitizenSmokingTask.TRAVEL_TICKS,"Travel timeout reached");
            }
            if(consumed(actor)>=0) {
                h.assertTrue(released[0] && h.getTick()-releasedAt[0]>=CitizenSmokingTask.RETRY_TICKS,"Normal activity during retry delay");
                h.assertTrue(joints(inaccessible.getItemHandlerCap())==1 && joints(accessible.getItemHandlerCap())==0,"Retry skips failed hut and consumes accessible stock");
                f.close();h.succeed();
            }
        });
    }

    @GameTest(template="production",batch="service_self",timeoutTicks=900)
    public static void budtenderCollectsWithoutWaitingForSelf(GameTestHelper h) {
        var f=new Fixture(h);var home=f.building(com.minecolonies.api.blocks.ModBlocks.blockHutHome,24,20,3);
        var hut=f.hut(12,8);f.stock(hut,1);var worker=f.actor(12,7,home);
        hut.getModule(DispensaryRegistry.WORK).assignCitizen(worker.getCitizenData());worker.setNoAi(false);
        long[] waitedAt={-1};
        h.onEachTick(()->{
            if(task(worker)==null)return;
            if(task(worker).step()==CitizenSmokingTask.Step.WAIT && waitedAt[0]<0)waitedAt[0]=h.getTick();
            if(consumed(worker)>=0) {
                h.assertTrue(waitedAt[0]>=0 && h.getTick()-waitedAt[0]<CitizenSmokingTask.WAIT_TICKS,"Self-collection bypasses own service queue");
                h.assertTrue(joints(worker.getInventoryCitizen())==0 && joints(hut.getItemHandlerCap())==0,"Self-service consumes exactly one");
                f.close();h.succeed();
            }
        });
    }
}
