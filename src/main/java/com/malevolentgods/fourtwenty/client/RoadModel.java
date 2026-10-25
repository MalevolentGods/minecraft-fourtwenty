package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.block.RoadShape;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The model of every road and road slab state: four quarter boxes, each one of the road's case parts
 * (tools/generate_paths.py) picked by {@link RoadShape} from the blocks round it as the chunk is
 * drawn, and a cap up to a full block where another road block stands on it. In a stack a full-height
 * quarter's sides drop the surface's edge (a block on it) and the footing (standing on one), so the
 * stack's sides read as one wall. (Stairs are separate
 * steps with plain models.) Quarters: 0 nw, 1 ne, 2 sw, 3 se (bit 0 east, bit 1 south).
 */
public final class RoadModel implements IDynamicBakedModel {
    private static final ModelProperty<Integer> CASES = new ModelProperty<>();
    private static final String[] QUARTERS = {"nw", "ne", "sw", "se"};
    private static final String[] CASE_NAMES = {"outer", "ns", "ew", "inner", "full"};
    /** A quarter's boxes: 0..15 (a road's, a double slab's), 0..7 (a bottom slab's), 8..15 (a top slab's). */
    private static final String[] BOXES = {"%s_%s_%s", "%s_slab_bottom_%s_%s", "%s_slab_top_%s_%s"};
    /** A full-height quarter's sides in a stack: alone, a block on it, standing on one, both. */
    private static final String[] STACKS = {"", "_u", "_d", "_ud"};
    private static final RandomSource RANDOM = RandomSource.create(42);

    /** parts[box][quarter][case][stack] (only the full-height box stacks) */
    private final BakedModel[][][][] parts;
    /** caps[quarter][plain, at an outer corner] */
    private final BakedModel[][] caps;
    private final int[] box = new int[4];
    private final Map<Integer, List<BakedQuad>[]> quads = new ConcurrentHashMap<>();

    private RoadModel(BlockState state, BakedModel[][][][] parts, BakedModel[][] caps) {
        this.parts = parts;
        this.caps = caps;
        if (state.getBlock() instanceof SlabBlock) {
            SlabType type = state.getValue(SlabBlock.TYPE);
            java.util.Arrays.fill(box, type == SlabType.BOTTOM ? 1 : type == SlabType.TOP ? 2 : 0);
        }
    }

    private static int sx(int q) { return (q & 1) == 0 ? -1 : 1; }

    private static int sz(int q) { return (q & 2) == 0 ? -1 : 1; }

    @Override public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData data) {
        int packed = 0;
        for (int q = 0; q < 4; q++) {
            packed |= RoadShape.caseOf(level, pos, state, sx(q), sz(q)) << (3 * q);
            if (RoadShape.capped(level, pos, state, sx(q), sz(q))) packed |= 1 << (12 + q);
            if (RoadShape.standsOnRoad(level, pos, state, sx(q), sz(q))) packed |= 1 << (16 + q);
        }
        return ModelData.of(CASES, packed);
    }

    @Override public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                       @NotNull ModelData data, @Nullable RenderType renderType) {
        Integer cases = data.get(CASES);
        return quads.computeIfAbsent(cases == null ? 0 : cases, c -> build(state, c))[side == null ? 6 : side.ordinal()];
    }

    @SuppressWarnings("unchecked")
    private List<BakedQuad>[] build(BlockState state, int cases) {
        List<BakedQuad>[] out = new List[7];
        for (int i = 0; i < 7; i++) {
            Direction side = i == 6 ? null : Direction.values()[i];
            List<BakedQuad> list = new ArrayList<>();
            for (int q = 0; q < 4; q++) {
                int c = (cases >> (3 * q)) & 7;
                int stack = box[q] != 0 ? 0 : ((cases >> (12 + q)) & 1) | ((cases >> (16 + q)) & 1) << 1;
                list.addAll(parts[box[q]][q][c][stack].getQuads(state, side, RANDOM));
                if ((cases & (1 << (12 + q))) != 0) list.addAll(caps[q][c == RoadShape.OUTER ? 1 : 0].getQuads(state, side, RANDOM));
            }
            out[i] = List.copyOf(list);
        }
        return out;
    }

    @Override public boolean useAmbientOcclusion() { return true; }

    @Override public boolean isGui3d() { return true; }

    @Override public boolean usesBlockLight() { return true; }

    @Override public boolean isCustomRenderer() { return false; }

    @Override public @NotNull TextureAtlasSprite getParticleIcon() { return parts[0][0][0][0].getParticleIcon(); }

    @Override public @NotNull ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }

    @Override public @NotNull ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }

    // ---- registration ------------------------------------------------------------------------

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("fourtwenty", "block/" + name));
    }

    private static List<String> partNames(String road) {
        List<String> names = new ArrayList<>();
        for (String b : BOXES) for (String q : QUARTERS) for (String c : CASE_NAMES) names.add(String.format(b, road, q, c));
        for (String q : QUARTERS) for (String c : CASE_NAMES) for (int v = 1; v < 4; v++) names.add(String.format(BOXES[0], road, q, c) + STACKS[v]);
        for (String q : QUARTERS) {
            names.add(road + "_cap_" + q);
            names.add(road + "_cap_" + q + "_corner");
        }
        return names;
    }

    private static String road(int tier) { return BuiltInRegistries.BLOCK.getKey(ModTerpenes.PATHS.get(tier).get()).getPath(); }

    /** The parts are standalone models: nothing's blockstate names them. */
    public static void registerParts(ModelEvent.RegisterAdditional event) {
        for (int t = 0; t < ModTerpenes.PATHS.size(); t++) partNames(road(t)).forEach(n -> event.register(part(n)));
    }

    /** Every road and slab state gets this model; a tier whose parts are missing keeps its plain model. */
    public static void replace(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (int t = 0; t < ModTerpenes.PATHS.size(); t++) {
            String road = road(t);
            BakedModel[][][][] parts = new BakedModel[3][4][5][4];
            BakedModel[][] caps = new BakedModel[4][2];
            boolean ok = true;
            for (int b = 0; b < 3; b++) for (int q = 0; q < 4; q++) for (int c = 0; c < 5; c++) for (int v = 0; v < (b == 0 ? 4 : 1); v++) {
                ok &= (parts[b][q][c][v] = models.get(part(String.format(BOXES[b], road, QUARTERS[q], CASE_NAMES[c]) + STACKS[v]))) != null;
            }
            for (int q = 0; q < 4; q++) {
                ok &= (caps[q][0] = models.get(part(road + "_cap_" + QUARTERS[q]))) != null;
                ok &= (caps[q][1] = models.get(part(road + "_cap_" + QUARTERS[q] + "_corner"))) != null;
            }
            if (!ok) continue;
            for (Block block : List.<Block>of(ModTerpenes.PATHS.get(t).get(), ModTerpenes.PATH_SLABS.get(t).get())) {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    models.put(BlockModelShaper.stateToModelLocation(state), new RoadModel(state, parts, caps));
                }
            }
        }
    }
}
