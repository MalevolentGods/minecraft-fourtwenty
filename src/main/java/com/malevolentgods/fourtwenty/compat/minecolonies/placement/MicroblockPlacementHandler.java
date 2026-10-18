package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets colony builders build CB Multipart microblocks (e.g. the Dispensary roof trim): instead of a
 * microblock item they cannot request, they pay the blocks the parts are cut from
 * ({@link MicroblockCost}). Reads the blueprint's saved part data directly, so FourTwenty needs no
 * compile dependency on CB Multipart; the handler is inert when CB is not installed.
 */
public final class MicroblockPlacementHandler extends CostOnlyPlacementHandler {
    private static final ResourceLocation MULTIPART = ResourceLocation.parse("cb_multipart:multipart");

    @Override
    public boolean canHandle(Level world, BlockPos pos, BlockState state) {
        return MULTIPART.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    @Override
    protected List<ItemStack> builderCost(BlockState state, CompoundTag tileEntityData) {
        if (tileEntityData == null || !tileEntityData.contains("parts", Tag.TAG_LIST)) return null;
        var parts = new ArrayList<MicroblockCost.Part>();
        for (Tag tag : tileEntityData.getList("parts", Tag.TAG_COMPOUND)) {
            var part = (CompoundTag) tag;
            parts.add(new MicroblockCost.Part(part.getString("id"), part.getByte("shape") & 0xFF, part.getString("material")));
        }
        var items = new ArrayList<ItemStack>();
        MicroblockCost.blocks(parts).forEach((block, count) -> {
            var location = ResourceLocation.tryParse(block);
            if (location == null) return;
            var item = BuiltInRegistries.BLOCK.get(location).asItem();
            if (item != Items.AIR) items.add(new ItemStack(item, count));
        });
        // Nothing payable (unknown materials): keep the normal behaviour rather than building for free.
        return items.isEmpty() ? null : items;
    }
}
