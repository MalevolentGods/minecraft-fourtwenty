package com.malevolentgods.fourtwenty.colonytest;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.blueprints.v1.BlueprintUtil;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.*;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class DispensaryPresentationGameTests {
    public static Blueprint blueprint(GameTestHelper h, int level) throws Exception {
        try (var input = DispensaryBlock.class.getResourceAsStream("/blueprints/fourtwenty/dispensary/dispensary/dispensary" + level + ".blueprint")) {
            return BlueprintUtil.readBlueprintFromNBT(NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap()), h.getLevel().registryAccess());
        }
    }
    @GameTest(template="empty", batch="dispensary_blueprint", timeoutTicks=200)
    public static void allLevelsAndNativeTransforms(GameTestHelper h) throws Exception {
        for (int level=1;level<=5;level++) for (var transform : RotationMirror.values()) {
            var bp=blueprint(h,level);
            bp.setRotationMirror(transform,h.getLevel());
            var anchor=bp.getPrimaryBlockOffset();
            h.assertTrue(bp.getBlockState(anchor).is(DispensaryRegistry.HUT.get()),"Anchor survives "+level+" "+transform);
            h.assertTrue(bp.getBlockInfoAsList().stream().filter(info->info.getState().is(DispensaryRegistry.HUT.get())).count()==1,"Exactly one hut");
            var tile=bp.getBlockInfoAsMap().get(anchor).getTileEntityData();
            var tags=tile.getCompound("blueprintDataProvider").getList("posTagMap",10);
            int seats=0;
            var entrance=anchor.offset(transform.applyToPos(new BlockPos(0,0,-3)));
            var reachable=reachable(bp,entrance);
            for (var tag : tags) {
                var entry=(CompoundTag)tag;
                var p=entry.getCompound("tagPos");
                var pos=anchor.offset(p.getInt("x"),p.getInt("y"),p.getInt("z"));
                var name=entry.getList("tagNameList",10).getCompound(0).getString("tagName");
                if (name.equals("sit_in")) {
                    seats++;
                    h.assertTrue(bp.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.StairBlock,"Seat state preserved");
                    h.assertTrue(bp.getBlockState(pos.above()).isAir(),"Seat headroom");
                    boolean covered=false;
                    for(int y=pos.getY()+2;y<bp.getSizeY();y++) if(!bp.getBlockState(new BlockPos(pos.getX(),y,pos.getZ())).isAir()) covered=true;
                    h.assertTrue(covered,"Indoor seat has a roof");
                    h.assertTrue(adjacent(reachable,pos),"Seat approach reachable "+level+" "+transform+" "+pos);
                } else h.assertTrue(adjacent(reachable,pos),"Marker reachable "+name+" "+level+" "+transform+" "+pos);
            }
            h.assertTrue(seats>=2,"Indoor seats at every level");
            for(var info:bp.getBlockInfoAsList()) {
                if(info.getState().is(Blocks.CRAFTING_TABLE) || info.getState().is(Blocks.BARREL)
                        || info.getState().getBlock() instanceof com.minecolonies.core.blocks.BlockMinecoloniesRack)
                    h.assertTrue(adjacent(reachable,info.getPos()),"Storage/work approach reachable");
            }
            h.assertTrue(reachable.contains(anchor.offset(transform.applyToPos(new BlockPos(0,0,-1)))),"Counter reachable");
        }
        h.succeed();
    }
    /** Citizens sit, work and use racks from up to two blocks away (seats behind tables, racks behind counters). */
    private static boolean adjacent(Set<BlockPos> reachable,BlockPos pos) {
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) for(int dy=-1;dy<=1;dy++) if(reachable.contains(pos.offset(dx,dy,dz))) return true;
        return false;
    }
    /** Walk-through blocks: doors, gates, carpets, plates, torches, signs (as tools/import_dispensary_blueprints.py). */
    private static boolean open(Blueprint bp,BlockPos pos) {
        if(pos.getX()<0||pos.getY()<0||pos.getZ()<0||pos.getX()>=bp.getSizeX()||pos.getY()>=bp.getSizeY()||pos.getZ()>=bp.getSizeZ()) return true;
        var state=bp.getBlockState(pos);
        if(state.isAir()) return true;
        var path=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        for(var part:List.of("_door","fence_gate","carpet","pressure_plate","button","torch","_sign","rail","flower","vine","lever","snow","tripwire","redstone_wire","blocksubstitution"))
            if(path.contains(part)&&!path.contains("solidsubstitution")) return true;
        return false;
    }
    /** Two-high walkable cells from the entrance, stepping one block up or down (stairs, upper floors). */
    private static Set<BlockPos> reachable(Blueprint bp,BlockPos entrance) {
        var result=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();result.add(entrance);queue.add(entrance);
        while(!queue.isEmpty()) {
            var pos=queue.remove();
            for(var side:List.of(pos.north(),pos.south(),pos.east(),pos.west())) for(int dy:new int[]{0,1,-1}) {
                var next=side.above(dy);
                if(next.getX()<0||next.getZ()<0||next.getY()<1||next.getX()>=bp.getSizeX()||next.getZ()>=bp.getSizeZ()||next.getY()>=bp.getSizeY()||result.contains(next))continue;
                boolean walkable=open(bp,next)&&open(bp,next.above())&&!open(bp,next.below());
                if(walkable&&(dy!=1||open(bp,pos.above(2)))) {result.add(next);queue.add(next);break;}
            }
        }
        return result;
    }
    /** Share the status fixture so this pure lookup check does not add a world-time-changing test batch. */
    private static void verifyVoiceLookup(GameTestHelper h,com.minecolonies.api.colony.ICitizenData data) {
        for(boolean female:new boolean[]{false,true}) for(int voice=0;voice<4;voice++) {
            final int profile=voice;
            var speaker=(com.minecolonies.api.colony.ICitizenData)java.lang.reflect.Proxy.newProxyInstance(
                    DispensaryPresentationGameTests.class.getClassLoader(),new Class[]{com.minecolonies.api.colony.ICitizenData.class},
                    (proxy,method,args)->switch(method.getName()) {
                        case "isFemale" -> female;
                        case "getVoiceProfile" -> profile;
                        default -> method.invoke(data,args);
                    });
            for(var event:com.minecolonies.api.sounds.EventType.values()) {
                // Chance zero suppresses playback; the native method still performs every voice lookup first.
                com.minecolonies.api.util.SoundUtils.playSoundAtCitizenWith(h.getLevel(),h.absolutePos(BlockPos.ZERO),event,speaker,0,0);
            }
        }
    }
    /** Exercise the registered level-tick listener with unchanged inventory and independent world time. */
    @GameTest(template="empty",batch="dispensary_status_refresh")
    public static void levelTicksRefreshMovementAndQueueWithoutInventoryChanges(GameTestHelper h) throws Exception {
        var colony=IColonyManager.getInstance().createColony(h.getLevel(),h.absolutePos(new BlockPos(1,1,1)),FakePlayerFactory.getMinecraft(h.getLevel()),"Status refresh","FourTwenty");
        EntityCitizen worker=null;
        try {
            var hut=DispensaryProductionGameTests.hut(h,colony,h.absolutePos(new BlockPos(1,1,1)));
            hut.setBuildingLevel(1);hut.onUpgradeComplete(null,1);
            worker=DispensaryProductionGameTests.actor(h,colony,hut.getPosition().east());
            hut.getModule(DispensaryRegistry.WORK).assignCitizen(worker.getCitizenData());
            ItemHandlerHelper.insertItemStacked(worker.getInventoryCitizen(),new ItemStack(ModItems.WEED_JOINT.get(),4),false);
            var module=hut.getModule(DispensaryRegistry.STATUS);
            var saved=DispensarySavedData.get(h.getLevel());
            while(saved.time()%100!=99) saved.advance();
            hut.clearDirty();
            levelTicks(h,1);
            h.assertTrue(module.checkDirty(),"Level tick schedules the initial status packet");
            hut.clearDirty();

            // Neither movement nor a customer lease changes the inventory or marks the building dirty.
            worker.setPos(hut.getPosition().getX()+20,hut.getPosition().getY(),hut.getPosition().getZ());
            var field=DispensaryBuilding.class.getDeclaredField("customers");field.setAccessible(true);
            @SuppressWarnings("unchecked") var customers=(Map<UUID,Long>)field.get(hut);
            customers.put(UUID.randomUUID(),saved.time()+150);
            h.assertTrue(!hut.isDirty(),"Movement and queue changes alone do not dirty the hut");
            levelTicks(h,99);
            h.assertTrue(!module.checkDirty(),"Status waits for the bounded refresh interval");
            levelTicks(h,1);
            h.assertTrue(module.checkDirty()&&hut.isDirty(),"Movement and queue changes schedule a native packet within 100 ticks");
            var s=statusPacket(h,module);
            h.assertTrue(s.joints()==4&&s.available()==0&&s.customers()==1&&s.conditions().contains("away"),"Packet reflects remote stock and the waiting customer");
            hut.clearDirty();

            levelTicks(h,100);
            h.assertTrue(module.checkDirty(),"Lease expiry schedules a packet without inventory changes");
            h.assertTrue(statusPacket(h,module).customers()==0,"Packet excludes the expired customer");
            hut.clearDirty();
            levelTicks(h,100);
            h.assertTrue(!module.checkDirty(),"Unchanged snapshots do not schedule redundant packets");

            worker.setPos(hut.getPosition().getX()+1.5,hut.getPosition().getY(),hut.getPosition().getZ()+.5);
            levelTicks(h,100);
            h.assertTrue(module.checkDirty(),"Returning worker schedules a packet without changing stock");
            s=statusPacket(h,module);
            h.assertTrue(s.joints()==4&&s.available()==4&&!s.conditions().contains("away"),"Returning worker stock becomes available");
            h.succeed();
        } finally {if(worker!=null)worker.discard();IColonyManager.getInstance().deleteColonyByWorld(colony.getID(),false,h.getLevel());}
    }
    /** Dispatch real server-level events without advancing game time or invoking the native slow-colony hook. */
    private static void levelTicks(GameTestHelper h,int count) {
        for(int n=0;n<count;n++) NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(() -> true,h.getLevel()));
    }
    private static DispensaryStatus statusPacket(GameTestHelper h,DispensaryStatusModule module) {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        try {module.serializeToView(buffer);return DispensaryStatus.read(buffer);} finally {buffer.release();}
    }
    @GameTest(template="empty",batch="dispensary_status")
    public static void inventoryStatusAndNativePacket(GameTestHelper h) throws Exception {
        var colony=IColonyManager.getInstance().createColony(h.getLevel(),h.absolutePos(new BlockPos(1,1,1)),FakePlayerFactory.getMinecraft(h.getLevel()),"Status","FourTwenty");
        EntityCitizen worker=null;
        try {
            var hut=DispensaryProductionGameTests.hut(h,colony,h.absolutePos(new BlockPos(1,1,1)));
            var module=hut.getModule(DispensaryRegistry.STATUS);
            h.assertTrue(module.snapshot().conditions().containsAll(List.of("unfinished","unstaffed","buds","paper","empty")),"Unbuilt diagnostics");
            hut.setBuildingLevel(3);hut.onUpgradeComplete(null,3);
            ItemHandlerHelper.insertItemStacked(hut.getItemHandlerCap(),new ItemStack(ModItems.WEED_JOINT.get(),7),false);
            ItemHandlerHelper.insertItemStacked(hut.getItemHandlerCap(),new ItemStack(ModItems.WEED_BUD.get(),3),false);
            ItemHandlerHelper.insertItemStacked(hut.getItemHandlerCap(),new ItemStack(Items.PAPER,6),false);
            var s=module.snapshot();
            h.assertTrue(s.joints()==7&&s.available()==7&&s.buds()==3&&s.paper()==6&&s.target()==64,"Unstaffed storage is visible");
            worker=DispensaryProductionGameTests.actor(h,colony,hut.getPosition().east());
            hut.getModule(DispensaryRegistry.WORK).assignCitizen(worker.getCitizenData());
            var jobBuffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
            try {
                worker.getCitizenData().getJob().serializeToView(jobBuffer);jobBuffer.writeInt(0x420abc);
                var jobView=com.minecolonies.api.colony.jobs.registry.IJobDataManager.getInstance().createViewFrom(null,null,jobBuffer);
                h.assertTrue(jobView instanceof com.minecolonies.core.colony.jobs.views.CrafterJobView
                        && jobBuffer.readInt()==0x420abc && jobBuffer.readableBytes()==0,"Registered crafter reader consumes the complete job packet");
            } finally {jobBuffer.release();}
            ItemHandlerHelper.insertItemStacked(worker.getInventoryCitizen(),new ItemStack(ModItems.WEED_JOINT.get(),4),false);
            s=module.snapshot();
            h.assertTrue(s.joints()==11&&s.available()==11&&!s.conditions().contains("away"),"Worker inventory counted exactly once at counter");
            worker.setPos(hut.getPosition().getX()+20, hut.getPosition().getY(),hut.getPosition().getZ());
            s=module.snapshot();
            h.assertTrue(s.joints()==11&&s.available()==7&&s.conditions().contains("away"),"Remote stock is total, not counter stock");
            hut.getModule(DispensaryRegistry.CRAFT).onColonyTick(colony);
            s=module.snapshot();h.assertTrue(s.conditions().contains("request"),"Existing native request shown");
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
            try {module.serializeToView(buffer);h.assertTrue(DispensaryStatus.read(buffer).equals(s)&&buffer.readableBytes()==0,"Native packet round trip");} finally {buffer.release();}
            var rackPos=hut.getPosition().south(2);
            h.getLevel().setBlockAndUpdate(rackPos,com.minecolonies.api.blocks.ModBlocks.blockRack.defaultBlockState());
            hut.registerBlockPosition(com.minecolonies.api.blocks.ModBlocks.blockRack,rackPos,h.getLevel());
            var rack=(com.minecolonies.core.tileentities.TileEntityRack)h.getLevel().getBlockEntity(rackPos);
            ItemHandlerHelper.insertItemStacked(rack.getItemHandlerCap(),new ItemStack(ModItems.WEED_JOINT.get(),5),false);
            // Native tile tick invalidates the combined capability after rack registration.
            ((com.minecolonies.core.tileentities.TileEntityColonyBuilding)hut.getTileEntity()).tick();
            s=module.snapshot();
            h.assertTrue(s.joints()==16&&s.available()==12,"Registered rack counted once in combined hut capability");
            var second=DispensaryProductionGameTests.hut(h,colony,h.absolutePos(new BlockPos(4,1,1)));
            h.assertTrue(second.getModule(DispensaryRegistry.STATUS).snapshot().joints()==0,"Independent hut status");
            // Lease expiry is tested without introducing a new gameplay mutation API.
            var field=DispensaryBuilding.class.getDeclaredField("customers");field.setAccessible(true);
            @SuppressWarnings("unchecked") var customers=(Map<UUID,Long>)field.get(hut);
            customers.put(UUID.randomUUID(),40L);customers.put(UUID.randomUUID(),41L);
            h.assertTrue(hut.waitingCustomers(40)==1&&hut.waitingCustomers(41)==0,"Queue expiry excludes stale customers");
            verifyVoiceLookup(h,worker.getCitizenData());
            h.succeed();
        } finally {if(worker!=null)worker.discard();IColonyManager.getInstance().deleteColonyByWorld(colony.getID(),false,h.getLevel());}
    }
}
