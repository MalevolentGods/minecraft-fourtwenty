package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.block.RoadShape;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The model of every one of our blocks that isn't a road or a full cube (rugs, couches, tables, Pot Pots):
 * standing on a road block's full-height top, it's drawn a pixel lower, resting on the road (whose top is
 * at 15) rather than hovering over it. Only the drawing moves; the block's shape stays where it is.
 */
public final class SunkModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Boolean> SUNK = new ModelProperty<>();
    private static final float PIXEL = 1 / 16f;
    /** Every quad, a pixel lower (quads are shared between models, so one map serves them all). */
    private static final Map<BakedQuad, BakedQuad> LOWERED = new ConcurrentHashMap<>();

    private SunkModel(BakedModel model) {
        super(model);
    }

    /** Whether a road's top lies under the whole of pos: every quarter of the block below is a road at full height. */
    static boolean onRoad(BlockAndTintGetter level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (RoadShape.tier(below) < 0) return false;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                if (RoadShape.top(below, sx, sz) != 15) return false;
            }
        }
        return true;
    }

    @Override public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData data) {
        ModelData d = super.getModelData(level, pos, state, data);
        return onRoad(level, pos) ? d.derive().with(SUNK, true).build() : d;
    }

    @Override public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                       @NotNull ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        if (data.get(SUNK) != Boolean.TRUE) return quads;
        return quads.stream().map(q -> LOWERED.computeIfAbsent(q, SunkModel::lower)).toList();
    }

    private static BakedQuad lower(BakedQuad q) {
        int[] v = q.getVertices().clone();
        int stride = v.length / 4;
        for (int i = 0; i < 4; i++) v[i * stride + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(v[i * stride + 1]) - PIXEL);
        return new BakedQuad(v, q.getTintIndex(), q.getDirection(), q.getSprite(), q.isShade(), q.hasAmbientOcclusion());
    }

    /** Wraps our blocks' models, all but the roads' and full cubes' (a full block on a road covers it: RoadModel). */
    public static void wrap(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("fourtwenty")) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (RoadShape.tier(state) >= 0 || state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) continue;
                ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(key);
                if (model != null) models.put(key, new SunkModel(model));
            }
        }
    }
}
