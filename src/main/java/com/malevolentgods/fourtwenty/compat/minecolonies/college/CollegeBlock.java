package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Weed College's hut block. Its blueprints are Sensi Heights' {@code education/weedcollege1-5}. */
public final class CollegeBlock extends AbstractBlockHut<CollegeBlock> {
    public static final String NAME = "weedcollege";
    public static final String PATH = "education/weedcollege";

    @Override public String getHutName() { return NAME; }
    @Override public ResourceLocation getRegistryName() { return ResourceLocation.fromNamespaceAndPath("fourtwenty", getHutName()); }
    @Override public BuildingEntry getBuildingEntry() { return CollegeRegistry.BUILDING.get(); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var tile = new TileEntityColonyBuilding(CollegeRegistry.ENTITY.get(), pos, state);
        tile.registryName = getRegistryName();
        tile.setPackName(com.malevolentgods.fourtwenty.compat.minecolonies.domain.DispensaryPackRules.PACK);
        tile.setBlueprintPath(PATH + "1.blueprint");
        return tile;
    }
}
