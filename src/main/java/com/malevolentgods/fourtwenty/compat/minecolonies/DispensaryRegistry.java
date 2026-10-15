package com.malevolentgods.fourtwenty.compat.minecolonies;

import com.malevolentgods.fourtwenty.compat.minecolonies.building.*;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.items.ItemBlockHut;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.CraftingWorkerBuildingModule;
import com.minecolonies.core.colony.buildings.moduleviews.CraftingModuleView;
import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import com.minecolonies.core.colony.buildings.views.EmptyView;
import com.minecolonies.core.colony.jobs.views.DefaultJobView;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Referenced only inside the optional MineColonies boundary. */
public final class DispensaryRegistry {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("fourtwenty");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("fourtwenty");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "fourtwenty");
    private static final DeferredRegister<JobEntry> JOBS = DeferredRegister.create(CommonMinecoloniesAPIImpl.JOBS, "fourtwenty");
    private static final DeferredRegister<BuildingEntry> BUILDINGS = DeferredRegister.create(CommonMinecoloniesAPIImpl.BUILDINGS, "fourtwenty");
    private static final DeferredRegister<com.minecolonies.api.entity.citizen.happiness.HappinessRegistry.HappinessFactorTypeEntry> HAPPINESS =
            DeferredRegister.create(CommonMinecoloniesAPIImpl.HAPPINESS_FACTOR_TYPES, "fourtwenty");
    static { HAPPINESS.register("cannabis_satisfaction", () -> new com.minecolonies.api.entity.citizen.happiness.HappinessRegistry.HappinessFactorTypeEntry(
            com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CannabisHappinessModifier::new)); }
    public static final DeferredHolder<Block, DispensaryBlock> HUT = BLOCKS.register("dispensary", DispensaryBlock::new);
    public static final DeferredHolder<Item, ItemBlockHut> ITEM = ITEMS.register("dispensary", () -> new ItemBlockHut(HUT.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityColonyBuilding>> ENTITY = ENTITIES.register("dispensary",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileEntityColonyBuilding(DispensaryRegistry.ENTITY.get(), pos, state), HUT.get()).build(null));
    public static final DeferredHolder<JobEntry, JobEntry> JOB = JOBS.register("budtender", () -> new JobEntry.Builder()
            .setRegistryName(ResourceLocation.parse(DispensaryIds.BUDTENDER)).setJobProducer(BudtenderJob::new)
            .setJobViewProducer(() -> DefaultJobView::new).createJobEntry());
    public static final BuildingEntry.ModuleProducer<CraftingWorkerBuildingModule, WorkerBuildingModuleView> WORK = new BuildingEntry.ModuleProducer<>(
            "fourtwenty:budtender_work", () -> new CraftingWorkerBuildingModule(JOB.get(), Skill.Dexterity, Skill.Knowledge, false,
                    building -> building.getBuildingLevel() > 0 ? 1 : 0), () -> WorkerBuildingModuleView::new);
    public static final BuildingEntry.ModuleProducer<JointCraftingModule, CraftingModuleView> CRAFT = new BuildingEntry.ModuleProducer<>(
            "fourtwenty:joint_crafting", JointCraftingModule::new, () -> CraftingModuleView::new);
    public static final DeferredHolder<BuildingEntry, BuildingEntry> BUILDING = BUILDINGS.register("dispensary", () -> new BuildingEntry.Builder()
            .setRegistryName(ResourceLocation.parse(DispensaryIds.DISPENSARY)).setBuildingBlock(HUT.get())
            .setBuildingProducer(DispensaryBuilding::new).setBuildingViewProducer(() -> EmptyView::new)
            .addBuildingModuleProducer(WORK).addBuildingModuleProducer(CRAFT)
            .addBuildingModuleProducer(BuildingModules.STATS_MODULE).addBuildingModuleProducer(BuildingModules.MIN_STOCK).createBuildingEntry());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); JOBS.register(bus); BUILDINGS.register(bus); HAPPINESS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
                event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                        ENTITY.get(), (tile, side) -> tile.getItemHandlerCap(side)));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(ITEM.get());
        });
    }
    private DispensaryRegistry() {}
}
