package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.block.HempPathStairBlock;
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
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
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
 * The model of every road, road slab and road stair state: four quarter boxes, each one of the
 * road's case parts (tools/generate_paths.py) picked by {@link RoadShape} from the blocks round it as
 * the chunk is drawn, plus a stair's risers. Quarters: 0 nw, 1 ne, 2 sw, 3 se (bit 0 east, bit 1 south).
 */
public final class RoadModel implements IDynamicBakedModel {
    private static final ModelProperty<Integer> CASES = new ModelProperty<>();
    private static final String[] QUARTERS = {"nw", "ne", "sw", "se"};
    private static final String[] CASE_NAMES = {"outer", "ns", "ew", "inner", "full"};
    /** A quarter's boxes: 0..15 (a road's), 0..7 (a bottom slab's or tread's), 8..15 (a top slab's, an upside-down tread's). */
    private static final String[] BOXES = {"%s_%s_%s", "%s_slab_bottom_%s_%s", "%s_slab_top_%s_%s"};
    private static final RandomSource RANDOM = RandomSource.create(42);

    /** parts[box][quarter][case] */
    private final BakedModel[][][] parts;
    private final int[] box = new int[4];
    private final List<BakedModel> risers = new ArrayList<>();
    private final Map<Integer, List<BakedQuad>[]> quads = new ConcurrentHashMap<>();

    private RoadModel(BlockState state, BakedModel[][][] parts, BakedModel[][][] risers) {
        this.parts = parts;
        boolean stair = state.getBlock() instanceof HempPathStairBlock;
        for (int q = 0; q < 4; q++) {
            int sx = sx(q), sz = sz(q);
            if (state.getBlock() instanceof SlabBlock) {
                SlabType type = state.getValue(SlabBlock.TYPE);
                box[q] = type == SlabType.BOTTOM ? 1 : type == SlabType.TOP ? 2 : 0;
            } else if (stair && !RoadShape.high(state, sx, sz)) {
                box[q] = state.getValue(StairBlock.HALF) == Half.BOTTOM ? 1 : 2;
            }
        }
        if (stair) {                                // a high quarter's face toward a lower one beside it
            int half = state.getValue(StairBlock.HALF) == Half.BOTTOM ? 0 : 1;
            for (int q = 0; q < 4; q++) {
                if (box[q] != 0) continue;
                if (box[q ^ 1] != 0) this.risers.add(risers[half][q][0]);
                if (box[q ^ 2] != 0) this.risers.add(risers[half][q][1]);
            }
        }
    }

    private static int sx(int q) { return (q & 1) == 0 ? -1 : 1; }

    private static int sz(int q) { return (q & 2) == 0 ? -1 : 1; }

    @Override public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData data) {
        int packed = 0;
        for (int q = 0; q < 4; q++) packed |= RoadShape.caseOf(level, pos, state, sx(q), sz(q)) << (3 * q);
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
            for (int q = 0; q < 4; q++) list.addAll(parts[box[q]][q][(cases >> (3 * q)) & 7].getQuads(state, side, RANDOM));
            for (BakedModel riser : risers) list.addAll(riser.getQuads(state, side, RANDOM));
            out[i] = List.copyOf(list);
        }
        return out;
    }

    @Override public boolean useAmbientOcclusion() { return true; }

    @Override public boolean isGui3d() { return true; }

    @Override public boolean usesBlockLight() { return true; }

    @Override public boolean isCustomRenderer() { return false; }

    @Override public @NotNull TextureAtlasSprite getParticleIcon() { return parts[0][0][0].getParticleIcon(); }

    @Override public @NotNull ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }

    @Override public @NotNull ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }

    // ---- registration ------------------------------------------------------------------------

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("fourtwenty", "block/" + name));
    }

    private static List<String> partNames(String road) {
        List<String> names = new ArrayList<>();
        for (String b : BOXES) for (String q : QUARTERS) for (String c : CASE_NAMES) names.add(String.format(b, road, q, c));
        names.addAll(riserNames(road));
        return names;
    }

    /** riser parts, in order [half][quarter][toward the other x quarter, toward the other z quarter]. */
    private static List<String> riserNames(String road) {
        List<String> names = new ArrayList<>();
        for (String half : new String[]{"bottom", "top"}) {
            for (int q = 0; q < 4; q++) {
                names.add(road + "_riser_" + half + "_" + QUARTERS[q] + "_" + (sx(q) < 0 ? "east" : "west"));
                names.add(road + "_riser_" + half + "_" + QUARTERS[q] + "_" + (sz(q) < 0 ? "south" : "north"));
            }
        }
        return names;
    }

    private static String road(int tier) { return BuiltInRegistries.BLOCK.getKey(ModTerpenes.PATHS.get(tier).get()).getPath(); }

    /** The parts are standalone models: nothing's blockstate names them. */
    public static void registerParts(ModelEvent.RegisterAdditional event) {
        for (int t = 0; t < ModTerpenes.PATHS.size(); t++) partNames(road(t)).forEach(n -> event.register(part(n)));
    }

    /** Every state of the road blocks gets this model; a tier whose parts are missing keeps its plain model. */
    public static void replace(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (int t = 0; t < ModTerpenes.PATHS.size(); t++) {
            String road = road(t);
            BakedModel[][][] parts = new BakedModel[3][4][5];
            BakedModel[][][] risers = new BakedModel[2][4][2];
            List<String> riserNames = riserNames(road);
            boolean ok = true;
            for (int b = 0; b < 3; b++) for (int q = 0; q < 4; q++) for (int c = 0; c < 5; c++) {
                ok &= (parts[b][q][c] = models.get(part(String.format(BOXES[b], road, QUARTERS[q], CASE_NAMES[c])))) != null;
            }
            for (int i = 0; i < riserNames.size(); i++) ok &= (risers[i / 8][(i / 2) % 4][i % 2] = models.get(part(riserNames.get(i)))) != null;
            if (!ok) continue;
            for (Block block : List.<Block>of(ModTerpenes.PATHS.get(t).get(), ModTerpenes.PATH_SLABS.get(t).get(), ModTerpenes.PATH_STAIRS.get(t).get())) {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    models.put(BlockModelShaper.stateToModelLocation(state), new RoadModel(state, parts, risers));
                }
            }
        }
    }
}
