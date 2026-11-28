package com.malevolentgods.fourtwenty.compat.minecolonies.tiedye;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Tie Dye Shack's hut block. Its blueprints are Sensi Heights' {@code craftsmanship/luxury/tiedyeshack1-5}. */
public final class ShackBlock extends AbstractBlockHut<ShackBlock> {
    /** The College's Textiles studies wait for a block with this id (CollegeFeatures "tie_dye"). */
    public static final String NAME = "tiedyeshack";
    public static final String PATH = "craftsmanship/luxury/tiedyeshack";

    @Override public String getHutName() { return NAME; }
    @Override public ResourceLocation getRegistryName() { return ResourceLocation.fromNamespaceAndPath("fourtwenty", getHutName()); }
    @Override public BuildingEntry getBuildingEntry() { return TieDyeRegistry.BUILDING.get(); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var tile = new TileEntityColonyBuilding(TieDyeRegistry.ENTITY.get(), pos, state);
        tile.registryName = getRegistryName();
        tile.setPackName(com.malevolentgods.fourtwenty.compat.minecolonies.domain.DispensaryPackRules.PACK);
        tile.setBlueprintPath(PATH + "1.blueprint");
        return tile;
    }
}
