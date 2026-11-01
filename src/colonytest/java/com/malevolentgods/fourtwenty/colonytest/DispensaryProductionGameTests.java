package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.*;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class DispensaryProductionGameTests {
    private static IColony colony(GameTestHelper h) {
        return IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1,1,1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "Dispensary production", "Sensi Heights");
    }
    static DispensaryBuilding hut(GameTestHelper h, IColony colony, BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos, DispensaryRegistry.HUT.get().defaultBlockState());
        var building = (DispensaryBuilding) colony.getServerBuildingManager().addNewBuilding(
                (TileEntityColonyBuilding)h.getLevel().getBlockEntity(pos), h.getLevel());
        building.getModule(DispensaryRegistry.WORK).setHiringMode(com.minecolonies.api.colony.buildings.HiringMode.MANUAL);
        return building;
    }
    static EntityCitizen actor(GameTestHelper h, IColony colony, BlockPos pos) {
        var citizen = colony.getCitizenManager().createAndRegisterCivilianData();
        citizen.setIsChild(false); citizen.setSaturation(100);
        var actor = (EntityCitizen)com.minecolonies.api.entity.ModEntities.CITIZEN.create(h.getLevel());
        actor.setUUID(citizen.getUUID()); actor.setCitizenId(citizen.getId());
        actor.getCitizenColonyHandler().setColonyId(colony.getID());
        actor.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        h.getLevel().addFreshEntity(actor);
        actor.getCitizenColonyHandler().registerWithColony(colony.getID(),citizen.getId());
        actor.setNoAi(true);
        return actor;
    }
    private static void delete(GameTestHelper h, IColony colony) {
        IColonyManager.getInstance().deleteColonyByWorld(colony.getID(),false,h.getLevel());
    }
    @GameTest(template="empty", batch="dispensary_registration")
    public static void registrationStaffingCapabilityAndPersistence(GameTestHelper h) {
        var ids = new java.util.HashSet<Integer>();
        var producers = com.minecolonies.api.colony.buildings.registry.BuildingEntry.getALlModuleProducers();
        producers.forEach((key, producer) -> {
            h.assertTrue(com.minecolonies.api.colony.buildings.registry.BuildingEntry.getProducer(key) == producer,
                    "Native producer lookup remains intact: " + key);
            h.assertTrue(ids.add(producer.getRuntimeID()), "Native producer runtime IDs remain unique: " + key);
        });
        for (var producer : List.of(DispensaryRegistry.WORK, DispensaryRegistry.CRAFT, DispensaryRegistry.STATUS)) {
            h.assertTrue(producers.get(producer.key) == producer, "Dispensary producer registered exactly once");
        }
        var colony=colony(h);
        try {
            var building=hut(h,colony,h.absolutePos(new BlockPos(1,1,1)));
            var workers=building.getModule(DispensaryRegistry.WORK);
            var citizen=colony.getCitizenManager().createAndRegisterCivilianData(); citizen.setIsChild(false);
            h.assertTrue(workers.getModuleMax()==0 && !workers.assignCitizen(citizen),"Unbuilt hut cannot hire");
            for(int level=1;level<=5;level++) {
                building.setBuildingLevel(level);building.onUpgradeComplete(null,level);
                h.assertTrue(workers.getModuleMax()==1,"One worker at completed level "+level);
            }
            h.assertTrue(workers.assignCitizen(citizen) && citizen.getJob() instanceof BudtenderJob,"Native hiring assigns Budtender");
            var extra=colony.getCitizenManager().createAndRegisterCivilianData();extra.setIsChild(false);
            h.assertTrue(!workers.assignCitizen(extra),"Second worker rejected");
            for(var side:Direction.values()) h.assertTrue(h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,building.getPosition(),side)!=null,"Capability "+side);
            var storage=building.getItemHandlerCap();
            h.assertTrue(ItemHandlerHelper.insertItemStacked(storage,new ItemStack(ModItems.WEED_JOINT.get(),17),false).isEmpty(),"Hut stores joints");
            var provider=h.getLevel().registryAccess();
            var tile=building.getTileEntity();var tileSave=tile.saveWithoutMetadata(provider);
            var save=building.serializeNBT(provider);
            building.deserializeNBT(provider,save);tile.loadWithComponents(tileSave,provider);
            h.assertTrue(building.getBuildingLevel()==5 && workers.getAssignedCitizen().contains(citizen),"Building level and assignment round trip");
            h.assertTrue(InventoryUtils.getItemCountInItemHandler(building.getItemHandlerCap(),s->s.is(ModItems.WEED_JOINT.get()))==17,"Stock round trip");
            workers.removeCitizen(citizen);
            h.assertTrue(citizen.getJob()==null && citizen.getWorkBuilding()==null,"Native dismissal clears job/workplace");
            h.succeed();
        } finally {delete(h,colony);}
    }
    @GameTest(template="empty", batch="dispensary_requests")
    public static void nativeRequestsAreDeduplicatedAndIndependent(GameTestHelper h) {
        var colony=colony(h);
        try {
            var first=hut(h,colony,h.absolutePos(new BlockPos(1,1,1)));
            var second=hut(h,colony,h.absolutePos(new BlockPos(4,1,1)));
            for(var building:List.of(first,second)) {
                building.setBuildingLevel(1);building.onUpgradeComplete(null,1);
                var citizen=colony.getCitizenManager().createAndRegisterCivilianData();citizen.setIsChild(false);
                h.assertTrue(building.getModule(DispensaryRegistry.WORK).assignCitizen(citizen),"Independent hiring");
                for(int n=0;n<20;n++) building.getModule(DispensaryRegistry.CRAFT).onColonyTick(colony);
                h.assertTrue(requests(building)==1,"One stock request despite repeated ticks; actual="+requests(building));
                var saved=building.serializeNBT(h.getLevel().registryAccess());
                building.deserializeNBT(h.getLevel().registryAccess(),saved);
                building.getModule(DispensaryRegistry.CRAFT).onColonyTick(colony);
                h.assertTrue(requests(building)==1,"Saved request prevents duplication after building reload");
                var kept=new ArrayList<ItemStorage>();
                h.assertTrue(building.buildingRequiresCertainAmountOfItem(new ItemStack(ModItems.WEED_JOINT.get(),20),kept,false,null)==4,"Courier leaves 16-joint target");
                h.assertTrue(building.buildingRequiresCertainAmountOfItem(new ItemStack(ModItems.WEED_BUD.get(),20),new ArrayList<>(),false,null)==4,"Courier retains batch of buds");
                h.assertTrue(building.buildingRequiresCertainAmountOfItem(new ItemStack(Items.PAPER,40),new ArrayList<>(),false,null)==8,"Courier retains two papers per joint");
            }
            h.assertTrue(first.getAllAssignedCitizen().iterator().next()!=second.getAllAssignedCitizen().iterator().next(),"Distinct staff");
            ItemHandlerHelper.insertItemStacked(first.getItemHandlerCap(),new ItemStack(ModItems.WEED_JOINT.get()),false);
            h.assertTrue(InventoryUtils.getItemCountInItemHandler(second.getItemHandlerCap(),s->s.is(ModItems.WEED_JOINT.get()))==0,"Distinct stock");
            h.succeed();
        } finally {delete(h,colony);}
    }
    static int requests(DispensaryBuilding building) {
        return (int)building.getOpenRequestsByRequestableType().values().stream().flatMap(java.util.Collection::stream).distinct()
                .map(building.getColony().getRequestManager()::getRequestForToken).filter(r->r!=null && r.getRequest() instanceof com.minecolonies.api.colony.requestsystem.requestable.Stack s && s.matches(new ItemStack(ModItems.WEED_JOINT.get()))).count();
    }
    @GameTest(template="empty", batch="dispensary_recipe")
    public static void recipeUsesExactMaterialsWithoutImprovement(GameTestHelper h) {
        var colony=colony(h);
        EntityCitizen actor=null;
        try {
            var building=hut(h,colony,h.absolutePos(new BlockPos(1,1,1)));
            building.setBuildingLevel(1);building.onUpgradeComplete(null,1);
            actor=actor(h,colony,building.getPosition().offset(1,0,0));
            building.getModule(DispensaryRegistry.WORK).assignCitizen(actor.getCitizenData());
            var module=building.getModule(DispensaryRegistry.CRAFT);module.checkForWorkerSpecificRecipes();
            var recipe=module.getFirstRecipe(new ItemStack(ModItems.WEED_JOINT.get()));
            h.assertTrue(recipe!=null && recipe.getGridSize()==3,"Three-column recipe automatically taught at level one");
            var inventory=building.getItemHandlerCap();
            ItemHandlerHelper.insertItemStacked(inventory,new ItemStack(ModItems.WEED_BUD.get(),3),false);
            h.assertTrue(!module.fullFillRecipe(recipe),"Missing paper cannot craft");
            ItemHandlerHelper.insertItemStacked(inventory,new ItemStack(Items.PAPER,6),false);
            for(int n=0;n<3;n++) h.assertTrue(module.fullFillRecipe(recipe),"Native recipe fulfills batch "+n);
            int buds=0,paper=0,joints=0;
            for(var handler:building.getHandlers()) {
                buds+=InventoryUtils.getItemCountInItemHandler(handler,s->s.is(ModItems.WEED_BUD.get()));
                paper+=InventoryUtils.getItemCountInItemHandler(handler,s->s.is(Items.PAPER));
                joints+=InventoryUtils.getItemCountInItemHandler(handler,s->s.is(ModItems.WEED_JOINT.get()));
            }
            h.assertTrue(buds==0 && paper==0 && joints==3,"Exactly 3 buds + 6 paper -> 3 joints");
            var token=recipe.getToken();module.improveRecipe(recipe,10000,actor.getCitizenData());
            h.assertTrue(module.getFirstRecipe(new ItemStack(ModItems.WEED_JOINT.get())).getToken().equals(token),"No recipe discounts");
            h.succeed();
        } finally {if(actor!=null)actor.discard();delete(h,colony);}
    }
    @GameTest(template="empty", batch="dispensary_capacity")
    public static void fullStorageCannotLoseOutputOrConsumeIngredients(GameTestHelper h) throws Exception {
        var source=new net.neoforged.neoforge.items.ItemStackHandler(2);
        source.setStackInSlot(0,new ItemStack(ModItems.WEED_JOINT.get(),16));
        var destination=new net.neoforged.neoforge.items.ItemStackHandler(1);
        destination.setStackInSlot(0,new ItemStack(ModItems.WEED_JOINT.get(),15));
        h.assertTrue(!BudtenderAI.canDumpInventory(source,destination),"Partial output capacity blocks native extraction");
        h.assertTrue(source.getStackInSlot(0).getCount()==16 && destination.getStackInSlot(0).getCount()==15,"Capacity preview changes neither inventory");
        destination.setStackInSlot(0,ItemStack.EMPTY);
        h.assertTrue(BudtenderAI.canDumpInventory(source,destination),"Dump resumes when entire output fits");
        source.setStackInSlot(1,new ItemStack(ModItems.WEED_JOINT.get()));
        h.assertTrue(!BudtenderAI.canDumpInventory(source,destination),"Multiple output stacks reserve shared capacity");
        var colony=colony(h);EntityCitizen actor=null;
        try {
            var building=hut(h,colony,h.absolutePos(new BlockPos(1,1,1)));
            building.setBuildingLevel(1);building.onUpgradeComplete(null,1);
            actor=actor(h,colony,building.getPosition().offset(1,0,0));
            building.getModule(DispensaryRegistry.WORK).assignCitizen(actor.getCitizenData());
            var module=building.getModule(DispensaryRegistry.CRAFT);module.checkForWorkerSpecificRecipes();
            var ai=((BudtenderJob)actor.getCitizenData().getJob()).getWorkerAI();
            var recipeField=com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting.class.getDeclaredField("currentRecipeStorage");
            recipeField.setAccessible(true);recipeField.set(ai,module.getFirstRecipe(new ItemStack(ModItems.WEED_JOINT.get())));
            var inventory=actor.getInventoryCitizen();
            for(int slot=0;slot<inventory.getSlots();slot++) inventory.setStackInSlot(slot,new ItemStack(Items.STONE,64));
            inventory.setStackInSlot(0,new ItemStack(ModItems.WEED_BUD.get(),64));
            inventory.setStackInSlot(1,new ItemStack(Items.PAPER,64));
            h.assertTrue(ai.executeCraftingAction(-1)==com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.INVENTORY_FULL,"Full worker output waits for storage");
            h.assertTrue(inventory.getStackInSlot(0).getCount()==64 && inventory.getStackInSlot(1).getCount()==64,"No ingredients consumed without output capacity");
            h.succeed();
        } finally {if(actor!=null)actor.discard();delete(h,colony);}
    }
    @GameTest(template="empty", batch="dispensary_blueprint")
    public static void blueprintLoadsWithHutStorageAndSeating(GameTestHelper h) throws Exception {
        try(var input=DispensaryBlock.class.getResourceAsStream("/blueprints/fourtwenty/sensiheights/craftsmanship/luxury/dispensary1.blueprint")) {
            var nbt=net.minecraft.nbt.NbtIo.readCompressed(input,net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            var blueprint=com.ldtteam.structurize.blueprints.v1.BlueprintUtil.readBlueprintFromNBT(nbt,h.getLevel().registryAccess());
            h.assertTrue(blueprint.getSizeX()==9 && blueprint.getSizeZ()==11,"Prototype footprint");
            h.assertTrue(blueprint.getBlockState(blueprint.getPrimaryBlockOffset()).is(DispensaryRegistry.HUT.get()),"Registered hut anchor");
            var tile=nbt.getList("tile_entities",10).getCompound(0);
            var tags=tile.getCompound("blueprintDataProvider").getList("posTagMap",10);
            h.assertTrue(tags.size()==4,"Work, standing and two indoor seat markers");
            h.assertTrue(blueprint.getBlockState(new BlockPos(2,1,8)).is(net.minecraft.world.level.block.Blocks.BARREL),"Storage barrel");
            h.succeed();
        }
    }
}
