package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DispensaryBlock extends AbstractBlockHut<DispensaryBlock> {
    @Override public String getHutName() { return "dispensary"; }
    @Override public ResourceLocation getRegistryName() { return ResourceLocation.fromNamespaceAndPath("fourtwenty", getHutName()); }
    @Override public BuildingEntry getBuildingEntry() { return DispensaryRegistry.BUILDING.get(); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var tile = new TileEntityColonyBuilding(DispensaryRegistry.ENTITY.get(), pos, state);
        tile.registryName = getRegistryName();
        tile.setPackName(DispensaryBuilding.PACK);
        tile.setBlueprintPath(DispensaryBuilding.PATH + "1.blueprint");
        return tile;
    }
}
