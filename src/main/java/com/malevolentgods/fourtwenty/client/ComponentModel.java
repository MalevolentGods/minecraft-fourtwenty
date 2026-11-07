package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.electronics.ComponentBlock;
import com.malevolentgods.fourtwenty.electronics.Orientation;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A component's model, turned to its orientation (electronics/Orientation): any of the 24, which a blockstate's x and y rotations
 * can't all reach. Its model faces north; this turns every quad about the block's centre. Its faces aren't culled against neighbours
 * (a component is small and never fills its space), so they all come with no side.
 */
public final class ComponentModel extends BakedModelWrapper<BakedModel> {
    private final Map<Object, List<BakedQuad>> turned = new ConcurrentHashMap<>();

    private ComponentModel(BakedModel model) { super(model); }

    @Override public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                       @NotNull ModelData data, @Nullable RenderType renderType) {
        if (state == null || !state.hasProperty(ComponentBlock.FACING)) return super.getQuads(state, side, rand, data, renderType);
        if (side != null) return List.of();
        return turned.computeIfAbsent(List.of(state, renderType == null ? "all" : renderType), key -> {
            IQuadTransformer turn = QuadTransformers.applying(new Transformation(null,
                    Orientation.rotation(state.getValue(ComponentBlock.FACING), state.getValue(ComponentBlock.SPIN)), null, null).blockCenterToCorner());
            List<BakedQuad> out = new ArrayList<>(turn.process(super.getQuads(state, null, rand, data, renderType)));
            for (Direction face : Direction.values()) out.addAll(turn.process(super.getQuads(state, face, rand, data, renderType)));
            return out;
        });
    }

    /** Wraps every component's models. */
    public static void wrap(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        // states share their models (the blockstate keys only on and off): one wrapper each, keyed by the state inside it
        Map<BakedModel, ComponentModel> wrapped = new java.util.IdentityHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof ComponentBlock)) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(key);
                if (model != null && !(model instanceof ComponentModel)) models.put(key, wrapped.computeIfAbsent(model, ComponentModel::new));
            }
        }
    }
}
