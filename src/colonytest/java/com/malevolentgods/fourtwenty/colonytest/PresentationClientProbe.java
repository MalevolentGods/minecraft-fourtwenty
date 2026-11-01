package com.malevolentgods.fourtwenty.colonytest;

import com.ldtteam.blockui.controls.Text;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryStatus;
import com.malevolentgods.fourtwenty.compat.minecolonies.client.DispensaryStatusView;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in disposable-world presentation probe. Direct placement is not native builder acceptance. */
@EventBusSubscriber(modid="fourtwenty_colonytests", value=Dist.CLIENT)
public final class PresentationClientProbe {
    private static int tick, stage;
    private static volatile int galleryColony;
    private static com.ldtteam.blockui.views.BOWindow liveWindow;
    private static boolean ready;
    private static volatile boolean reloaded;
    private static java.util.concurrent.CompletableFuture<Void> setup;
    private static java.util.concurrent.CompletableFuture<Void> discovery;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(!Boolean.getBoolean("fourtwenty.presentationClientProbe")||mc.player==null||mc.getConnection()==null||mc.getOverlay()!=null)return;
        try {
            if(!ready) {
                ready=true;
                var model=com.minecolonies.api.client.render.modeltype.registry.IModelTypeRegistry.getInstance().getModelType(ResourceLocation.parse("fourtwenty:budtender"));
                if(model==null||model.getMaleModel()==null||model.getFemaleModel()==null)throw new AssertionError("Budtender models missing");
                if(mc.getResourceManager().getResource(ResourceLocation.parse("fourtwenty:textures/entity/budtender.png")).isEmpty())throw new AssertionError("Missing skin");
                showStatus(mc,false);
                setup=mc.getSingleplayerServer().submit(()->placeGallery(mc));
                var futures=new java.util.ArrayList<java.util.concurrent.CompletableFuture<?>>();
                for(int level=1;level<=5;level++) {
                    String path="craftsmanship/luxury/dispensary"+level+".blueprint";
                    futures.add(com.ldtteam.structurize.storage.StructurePacks.getBlueprintFuture("Sensi Heights",path,mc.level.registryAccess())
                            .thenAccept(bp->{if(bp==null)throw new AssertionError("Missing selector blueprint "+path);}));
                }
                discovery=java.util.concurrent.CompletableFuture.allOf(futures.toArray(java.util.concurrent.CompletableFuture[]::new));
                return;
            }
            if(setup!=null&&setup.isCompletedExceptionally())setup.join();
            tick++;
            if(stage>=2&&stage<=6)mc.gui.getChat().clearMessages(true);
            if(stage==0&&tick==40) {
                screenshot(mc,"status-stocked.png");
                mc.reloadResourcePacks().thenRun(()->reloaded=true);
            }
            if(stage==0&&tick>60&&reloaded) {showStatus(mc,true);stage=1;tick=0;}
            else if(stage==1&&tick==40) {screenshot(mc,"status-problems.png");mc.setScreen(null);stage=2;tick=0;camera(mc,1);}
            else if(stage>=2&&stage<=6&&tick==60) {
                screenshot(mc,"dispensary-level-"+(stage-1)+".png");
                stage++;tick=0;
                if(stage<=6)camera(mc,stage-1);
                else cameraDetail(mc);
            }
            else if(stage==7&&tick>=60&&discovery.isDone()) {
                    discovery.join();
                    var colonyView=com.minecolonies.api.colony.IColonyManager.getInstance().getColonyView(galleryColony,mc.level.dimension());
                    if(colonyView==null||colonyView.getCitizens().size()!=5)throw new AssertionError("Missing synchronized gallery citizens");
                    for(var citizen:colonyView.getCitizens().values()) {
                        if(!(citizen.getJobView() instanceof com.minecolonies.core.colony.jobs.views.CrafterJobView))throw new AssertionError("Budtender job view did not synchronize");
                    }
                    screenshot(mc,"dispensary-lounge-and-budtender.png");
                    var building=colonyView.getClientBuildingManager().getBuilding(new BlockPos(296,81,131));
                    if(building==null||building.getBuildingLevel()!=5||building.getAllModuleViews().size()<5)throw new AssertionError("Missing native building management views");
                    liveWindow=building.getModuleView(com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry.STATUS).getWindow();
                    liveWindow.open();stage=8;tick=0;
            }
            else if(stage==8&&tick==40) {
                    screenshot(mc,"status-live-empty.png");
                    mc.getSingleplayerServer().execute(()->{
                        var colony=com.minecolonies.api.colony.IColonyManager.getInstance().getColonyByWorld(galleryColony,mc.getSingleplayerServer().overworld());
                        var building=(com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding)colony.getServerBuildingManager().getBuilding(new BlockPos(296,81,131));
                        net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(building.getItemHandlerCap(),new net.minecraft.world.item.ItemStack(com.malevolentgods.fourtwenty.registry.ModItems.WEED_JOINT.get(),9),false);
                    });
                    // Keep simulation running while inspecting the live synchronized native window.
                    var pause=com.ldtteam.blockui.views.BOWindow.class.getDeclaredField("windowPausesGame");
                    pause.setAccessible(true);pause.setBoolean(liveWindow,false);
                    stage=9;tick=0;
            }
            else if(stage==9&&tick>=140) {
                    if(!liveWindow.findPaneOfTypeByID("joints",Text.class).getTextAsString().equals("Joints: 9 / 128 target"))throw new AssertionError("Live stock change did not synchronize");
                    screenshot(mc,"status-live-stocked.png");
                    com.mojang.logging.LogUtils.getLogger().info("FOURTWENTY_PRESENTATION_CLIENT_PROBE_PASSED: native job and building views, live stock update, widgets, reload, models, five directly placed blueprints");
                    mc.stop();
            }
        } catch(Throwable failure) {
            com.mojang.logging.LogUtils.getLogger().error("FOURTWENTY_PRESENTATION_CLIENT_PROBE_FAILED",failure);mc.stop();
        }
    }
    private static void showStatus(Minecraft mc,boolean problems) {
        var view=new DispensaryStatusView();
        var building=(IBuildingView)java.lang.reflect.Proxy.newProxyInstance(PresentationClientProbe.class.getClassLoader(),new Class[]{IBuildingView.class},
                (proxy,method,args)->switch(method.getName()) {case "getID"->BlockPos.ZERO;case "getAllModuleViews"->List.of(view);default->null;});
        view.setBuildingView(building);view.setProducer(com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry.STATUS);
        var status=problems?new DispensaryStatus(1,"",0,0,0,0,16,0,List.of("unstaffed","buds","paper","empty","request"))
                :new DispensaryStatus(5,"Alexandria the Budtender",32,64,128,112,128,10,List.of("away","request"));
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),mc.level.registryAccess());
        try {status.write(buffer);view.deserialize(buffer);}finally{buffer.release();}
        var window=view.getWindow();window.open();window.onOpened();window.onUpdate();
        String expected=problems?"Joints: 0 / 16 target":"Joints: 128 / 128 target";
        if(!window.findPaneOfTypeByID("joints",Text.class).getTextAsString().equals(expected))throw new AssertionError("Wrong translated stock row");
    }
    private static void screenshot(Minecraft mc,String name) {
        net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->com.mojang.logging.LogUtils.getLogger().info("Presentation screenshot: {}",message.getString()));
    }
    private static void camera(Minecraft mc,int level) {
        mc.getSingleplayerServer().execute(()->{
            var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            player.teleportTo(player.serverLevel(),128+level*32+(7+level*2)+8,91,112,45,25);
        });
    }
    private static void cameraDetail(Minecraft mc) {
        mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            p.teleportTo(p.serverLevel(),128+5*32+8.5,82.5,128+2,0,12);
        });
    }
    private static void placeGallery(Minecraft mc) {
        var world=mc.getSingleplayerServer().overworld();
        world.setDayTime(6000);world.setWeatherParameters(24000,0,false,false);
        world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,world.getServer());
        var colony=com.minecolonies.api.colony.IColonyManager.getInstance().createColony(world,new BlockPos(288,81,128),
                world.getServer().getPlayerList().getPlayers().getFirst(),"Presentation gallery","Sensi Heights");
        galleryColony=colony.getID();
        for(int level=1;level<=5;level++) {
            try(var input=com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBlock.class.getResourceAsStream("/blueprints/fourtwenty/sensiheights/craftsmanship/luxury/dispensary"+level+".blueprint")) {
                var bp=com.ldtteam.structurize.blueprints.v1.BlueprintUtil.readBlueprintFromNBT(net.minecraft.nbt.NbtIo.readCompressed(input,net.minecraft.nbt.NbtAccounter.unlimitedHeap()),world.registryAccess());
                var start=new BlockPos(128+level*32,80,128);
                for(var info:bp.getBlockInfoAsList()) {
                    var at=start.offset(info.getPos());world.setBlockAndUpdate(at,info.getState());
                    if(info.hasTileEntityData()&&world.getBlockEntity(at)!=null)world.getBlockEntity(at).loadWithComponents(info.getTileEntityData(),world.registryAccess());
                }
                var hutPos=start.offset(bp.getPrimaryBlockOffset());
                var hut=(com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding)colony.getServerBuildingManager().addNewBuilding(
                        (com.minecolonies.core.tileentities.TileEntityColonyBuilding)world.getBlockEntity(hutPos),world);
                hut.setStructurePack("Sensi Heights");hut.setBlueprintPath("craftsmanship/luxury/dispensary"+level+".blueprint");
                hut.setBuildingLevel(level);hut.onUpgradeComplete(null,level);
                hut.getModule(com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry.WORK).setHiringMode(com.minecolonies.api.colony.buildings.HiringMode.MANUAL);
                var data=colony.getCitizenManager().createAndRegisterCivilianData();data.setIsChild(false);data.setSaturation(100);
                var actor=(com.minecolonies.core.entity.citizen.EntityCitizen)com.minecolonies.api.entity.ModEntities.CITIZEN.create(world);
                actor.setUUID(data.getUUID());actor.setCitizenId(data.getId());actor.getCitizenColonyHandler().setColonyId(colony.getID());
                actor.setPos(hutPos.getX()+1.5,hutPos.getY(),hutPos.getZ()+1.5);world.addFreshEntity(actor);
                actor.getCitizenColonyHandler().registerWithColony(colony.getID(),data.getId());actor.setNoAi(true);
                hut.getModule(com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry.WORK).assignCitizen(data);
                actor.setModelId(data.getJob().getModel());actor.setFemale(level%2==0);actor.setTexture();
                actor.setYRot(180);actor.setYHeadRot(180);actor.yBodyRot=180;
            } catch(Exception ex){throw new RuntimeException(ex);}
        }
    }
}
