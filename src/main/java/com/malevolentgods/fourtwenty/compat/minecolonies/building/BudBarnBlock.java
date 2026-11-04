package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.malevolentgods.fourtwenty.compat.minecolonies.BudBarnRegistry;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Bud Barn's hut block. Its blueprints are Sensi Heights' {@code agriculture/horticulture/budbarn1-5}. */
public final class BudBarnBlock extends AbstractBlockHut<BudBarnBlock> {
    public static final String NAME = "budbarn";
    public static final String PATH = "agriculture/horticulture/budbarn";

    @Override public String getHutName() { return NAME; }
    @Override public ResourceLocation getRegistryName() { return ResourceLocation.fromNamespaceAndPath("fourtwenty", getHutName()); }
    @Override public BuildingEntry getBuildingEntry() { return BudBarnRegistry.BUILDING.get(); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var tile = new TileEntityColonyBuilding(BudBarnRegistry.ENTITY.get(), pos, state);
        tile.registryName = getRegistryName();
        tile.setPackName(com.malevolentgods.fourtwenty.compat.minecolonies.domain.DispensaryPackRules.PACK);
        tile.setBlueprintPath(PATH + "1.blueprint");
        return tile;
    }
}
