package com.malevolentgods.fourtwenty.compat.minecolonies.tiedye;

import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.items.ItemBlockHut;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
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

/** The Tie Dye Shack and its Tie Dyer (specs/018-tie-dye-shack FR-001). Referenced only inside the optional MineColonies boundary. */
public final class TieDyeRegistry {
    public static final String BUILDING_ID = "fourtwenty:" + ShackBlock.NAME, JOB_ID = "fourtwenty:tie_dyer";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("fourtwenty");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("fourtwenty");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "fourtwenty");
    private static final DeferredRegister<JobEntry> JOBS = DeferredRegister.create(CommonMinecoloniesAPIImpl.JOBS, "fourtwenty");
    private static final DeferredRegister<BuildingEntry> BUILDINGS = DeferredRegister.create(CommonMinecoloniesAPIImpl.BUILDINGS, "fourtwenty");

    public static final DeferredHolder<Block, ShackBlock> HUT = BLOCKS.register(ShackBlock.NAME, ShackBlock::new);
    public static final DeferredHolder<Item, ItemBlockHut> ITEM = ITEMS.register(ShackBlock.NAME, () -> new ItemBlockHut(HUT.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityColonyBuilding>> ENTITY = ENTITIES.register(ShackBlock.NAME,
            () -> BlockEntityType.Builder.of((pos, state) -> new TileEntityColonyBuilding(TieDyeRegistry.ENTITY.get(), pos, state), HUT.get()).build(null));
    public static final DeferredHolder<JobEntry, JobEntry> JOB = JOBS.register("tie_dyer", () -> new JobEntry.Builder()
            .setRegistryName(ResourceLocation.parse(JOB_ID)).setJobProducer(TieDyerJob::new)
            .setJobViewProducer(() -> DefaultJobView::new).createJobEntry());
    public static BuildingEntry.ModuleProducer<WorkerBuildingModule, WorkerBuildingModuleView> WORK;
    public static BuildingEntry.ModuleProducer<ShackModule, com.malevolentgods.fourtwenty.compat.minecolonies.client.ShackView> ORDERS;
    public static final DeferredHolder<BuildingEntry, BuildingEntry> BUILDING = BUILDINGS.register(ShackBlock.NAME, TieDyeRegistry::createBuildingEntry);

    private static BuildingEntry createBuildingEntry() {
        WORK = new BuildingEntry.ModuleProducer<>("fourtwenty:tie_dyer_work",
                () -> new WorkerBuildingModule(JOB.get(), Skill.Creativity, Skill.Dexterity, false, building -> TieDyeRules.workers(building.getBuildingLevel())),
                () -> WorkerBuildingModuleView::new);
        ORDERS = new BuildingEntry.ModuleProducer<>("fourtwenty:tiedyeshack_orders", ShackModule::new,
                () -> com.malevolentgods.fourtwenty.compat.minecolonies.client.ShackView::new);
        return new BuildingEntry.Builder()
                .setRegistryName(ResourceLocation.parse(BUILDING_ID)).setBuildingBlock(HUT.get())
                .setBuildingProducer(ShackBuilding::new).setBuildingViewProducer(() -> EmptyView::new)
                .addBuildingModuleProducer(WORK).addBuildingModuleProducer(ORDERS)
                .addBuildingModuleProducer(BuildingModules.STATS_MODULE).createBuildingEntry();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); JOBS.register(bus); BUILDINGS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
                event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                        ENTITY.get(), (tile, side) -> tile.getItemHandlerCap(side)));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS
                    || event.getTabKey() == com.malevolentgods.fourtwenty.registry.ModCreativeTabs.FOUR_TWENTY_TAB.getKey()) event.accept(ITEM.get());
        });
        TieDyeNetwork.register(bus);
    }

    private TieDyeRegistry() {}
}
