package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lets colony builders build CB Multipart microblocks (e.g. the Dispensary roof trim): instead of a
 * microblock item they cannot request, they pay the blocks the parts are cut from
 * ({@link MicroblockCost}). With the builder's blueprint at hand, the cost is spread over the whole
 * structure so 8 thin covers cost one source block; otherwise each position pays at least one.
 *
 * <p>Reads the blueprint's saved part data directly, so FourTwenty needs no compile dependency on
 * CB Multipart; the handler is inert when CB is not installed.
 */
public final class MicroblockPlacementHandler extends CostOnlyPlacementHandler {
    private static final ResourceLocation MULTIPART = ResourceLocation.parse("cb_multipart:multipart");
    private record Allocation(RotationMirror rotation, Map<BlockPos, Map<String, Integer>> charges) {}
    /** Per blueprint (re-computed when its rotation changes, since rotation moves positions). */
    private final Map<Blueprint, Allocation> allocations = new WeakHashMap<>();

    @Override
    public boolean canHandle(Level world, BlockPos pos, BlockState state) {
        return MULTIPART.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    @Override
    protected List<ItemStack> builderCost(Level world, BlockPos pos, BlockState state, CompoundTag tileEntityData, IPlacementContext context) {
        if (tileEntityData == null || !tileEntityData.contains("parts", Tag.TAG_LIST)) return null;
        Map<String, Integer> blocks = null;
        var blueprint = context.getBluePrint();
        if (blueprint != null && context instanceof IStructureHandler handler) {
            blocks = allocation(blueprint).get(handler.getStructurePosFromWorld(pos));
        }
        if (blocks == null) blocks = MicroblockCost.blocks(parts(tileEntityData));   // no blueprint: per-position cost
        var items = new ArrayList<ItemStack>();
        blocks.forEach((block, count) -> {
            var location = ResourceLocation.tryParse(block);
            if (location == null) return;
            var item = BuiltInRegistries.BLOCK.get(location).asItem();
            if (item != Items.AIR) items.add(new ItemStack(item, count));
        });
        return items;
    }

    private Map<BlockPos, Map<String, Integer>> allocation(Blueprint blueprint) {
        synchronized (allocations) {
            var cached = allocations.get(blueprint);
            if (cached != null && cached.rotation() == blueprint.getRotationMirror()) return cached.charges();
            var positions = new ArrayList<BlockPos>();
            var volumes = new ArrayList<Map<String, Integer>>();
            blueprint.getBlockInfoAsList().stream()
                    .filter(info -> info.hasTileEntityData() && MULTIPART.equals(BuiltInRegistries.BLOCK.getKey(info.getState().getBlock())))
                    .sorted(Comparator.comparingInt((com.ldtteam.structurize.util.BlockInfo info) -> info.getPos().getY())
                            .thenComparingInt(info -> info.getPos().getZ()).thenComparingInt(info -> info.getPos().getX()))
                    .forEach(info -> {
                        positions.add(info.getPos().immutable());
                        volumes.add(MicroblockCost.volumes(parts(info.getTileEntityData())));
                    });
            var charges = MicroblockCost.allocate(volumes);
            var byPos = new HashMap<BlockPos, Map<String, Integer>>();
            for (int i = 0; i < positions.size(); i++) byPos.put(positions.get(i), charges.get(i));
            allocations.put(blueprint, new Allocation(blueprint.getRotationMirror(), byPos));
            return byPos;
        }
    }

    private static List<MicroblockCost.Part> parts(CompoundTag tileEntityData) {
        var parts = new ArrayList<MicroblockCost.Part>();
        if (tileEntityData == null) return parts;
        for (Tag tag : tileEntityData.getList("parts", Tag.TAG_COMPOUND)) {
            var part = (CompoundTag) tag;
            parts.add(new MicroblockCost.Part(part.getString("id"), part.getByte("shape") & 0xFF, part.getString("material")));
        }
        return parts;
    }
}
