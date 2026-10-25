package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.block.HempPathStairBlock;
import com.malevolentgods.fourtwenty.block.RoadShape;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The model of every road, road slab and road stair state, built as the chunk is drawn: four quarter
 * boxes, each with its top bordered where {@link RoadShape} says the surface ends or steps (one of the
 * quarter-top textures tools/generate_paths.py makes for every arrangement of borders and notches),
 * its underside joined like a road's, its sides the cut through the road (running on as one wall in a
 * stack), and a stair's step fronts bordered strips that run on into the fronts beside them.
 * Quarters: 0 nw, 1 ne, 2 sw, 3 se (bit 0 east, bit 1 south).
 */
public final class RoadModel implements IDynamicBakedModel {
    private static final ModelProperty<Key> KEY = new ModelProperty<>();
    private static final String[] QUARTERS = {"nw", "ne", "sw", "se"};
    private static final String[] CASES = {"outer", "ns", "ew", "inner", "full"};
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final FaceBakery BAKERY = new FaceBakery();

    /** What a block's drawing depends on: per quarter, its top's borders and notches and whether it's stacked (tops); its fronts' open ends (fronts). */
    private record Key(long tops, int fronts) {}

    private final Textures textures;
    private final BlockState state;
    private final boolean rosettes;
    private final int[] y0 = new int[4], y1 = new int[4];
    private final Map<Key, List<BakedQuad>[]> quads = new ConcurrentHashMap<>();

    private RoadModel(BlockState state, Textures textures) {
        this.state = state;
        this.textures = textures;
        this.rosettes = textures.rosettes && !(state.getBlock() instanceof HempPathStairBlock);   // the yellow brick road's corners; not on steps
        for (int q = 0; q < 4; q++) {
            y0[q] = RoadShape.bottom(state, sx(q), sz(q));
            y1[q] = RoadShape.top(state, sx(q), sz(q)) == 7 ? 7 : 15;
        }
    }

    private static int sx(int q) { return (q & 1) == 0 ? -1 : 1; }

    private static int sz(int q) { return (q & 2) == 0 ? -1 : 1; }

    @Override public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData data) {
        long tops = 0;
        int fronts = 0;
        for (int q = 0; q < 4; q++) {
            int sx = sx(q), sz = sz(q);
            long bits = RoadShape.border(level, pos, state, sx, sz);
            if (RoadShape.capped(level, pos, state, sx, sz)) bits |= 1 << 8;
            if (RoadShape.standsOnRoad(level, pos, state, sx, sz)) bits |= 1 << 9;
            tops |= bits << (10 * q);
            for (int i = 0; i < SIDES.length; i++) {
                Direction d = SIDES[i];
                if (RoadShape.front(state, sx, sz, d) == 0) continue;
                int open = (RoadShape.frontRunsOn(level, pos, state, sx, sz, d, uLow(d)) ? 1 : 0)
                        | (RoadShape.frontRunsOn(level, pos, state, sx, sz, d, uLow(d).getOpposite()) ? 2 : 0);
                fronts |= open << (2 * (4 * q + i));
            }
        }
        return ModelData.of(KEY, new Key(tops, fronts));
    }

    @Override public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                       @NotNull ModelData data, @Nullable RenderType renderType) {
        Key key = data.get(KEY);
        return quads.computeIfAbsent(key == null ? new Key(alone(), 0) : key, this::build)[side == null ? 6 : side.ordinal()];
    }

    /** A lone block's tops (bordered all round, a stair's steps each bordered): for drawing it with no level round it. */
    private long alone() {
        long tops = 0;
        for (int q = 0; q < 4; q++) {
            int f = RoadShape.N | RoadShape.E | RoadShape.S | RoadShape.W;
            tops |= (long) f << (10 * q);
        }
        return tops;
    }

    /** The world side at a face's u = 0 (looking at it from outside). */
    private static Direction uLow(Direction d) {
        return switch (d) {
            case SOUTH -> Direction.WEST;
            case NORTH -> Direction.EAST;
            case WEST -> Direction.NORTH;
            default -> Direction.SOUTH;
        };
    }

    @SuppressWarnings("unchecked")
    private List<BakedQuad>[] build(Key key) {
        List<BakedQuad>[] out = new List[7];
        for (int i = 0; i < 7; i++) out[i] = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            int sx = sx(q), sz = sz(q);
            int bits = (int) (key.tops() >> (10 * q)) & 0x3ff, border = bits & 0xff;
            boolean up = (bits & 256) != 0, down = (bits & 512) != 0;
            float x0 = sx < 0 ? 0 : 8, z0 = sz < 0 ? 0 : 8, x1 = x0 + 8, z1 = z0 + 8;
            int lo = y0[q], hi = y1[q];
            // Top: this quarter's piece of the bordered surface.
            boolean rosette = rosettes && (border & (sz < 0 ? RoadShape.N : RoadShape.S)) != 0 && (border & (sx < 0 ? RoadShape.W : RoadShape.E)) != 0;
            String top = String.format("top_%s_%x%x%s", QUARTERS[q], border & 15, border >> 4, rosette ? "r" : "");
            add(out, null, x0, hi, z0, x1, hi, z1, Direction.UP, new float[]{0, 0, 16, 16}, textures.sprite(top));
            // Underside: joined like a road's, at the block's edges.
            String under = "bottom_" + CASES[RoadShape.caseOf(border, sx, sz)];
            add(out, lo == 0 ? Direction.DOWN : null, x0, lo, z0, x1, lo, z1, Direction.DOWN, new float[]{x0, 16 - z1, x1, 16 - z0}, textures.sprite(under));
            // Outer sides: the cut through the road (quoins at a convex corner), or a stair's step front.
            boolean corner = (border & (sz < 0 ? RoadShape.N : RoadShape.S)) != 0 && (border & (sx < 0 ? RoadShape.W : RoadShape.E)) != 0;
            String stack = (up ? "u" : "") + (down ? "d" : "");
            String side = "side" + (corner ? "_corner" : "") + (stack.isEmpty() ? "" : "_" + stack);
            for (int i = 0; i < SIDES.length; i++) {
                Direction d = SIDES[i];
                boolean outer = d.getStepX() != 0 ? d.getStepX() == sx : d.getStepZ() == sz;
                float[] u = span(d, x0, z0);
                int kind = RoadShape.front(state, sx, sz, d);
                int open = (key.fronts() >> (2 * (4 * q + i))) & 3;
                if (outer) {
                    if (kind != 0) {
                        face(out, d, d, x0, lo, z0, x1, hi, z1, new float[]{0, 0, 16, 16}, front(u, open));
                    } else {
                        face(out, d, d, x0, lo, z0, x1, hi, z1, new float[]{u[0], 16 - hi, u[1], 16 - lo}, textures.sprite(side));
                        if (up && hi == 15) {                   // the pixel up to a full block (no cullface: a stair's back hides a 15/16 face, not this)
                            face(out, null, d, x0, 15, z0, x1, 16, z1, new float[]{u[0], 0, u[1], 1}, textures.sprite(side));
                        }
                    }
                    continue;
                }
                // Inside the block: where this quarter rises above the one beside it (a stair).
                int other = q ^ (d.getStepX() != 0 ? 1 : 2);
                if (y1[other] < hi) {                           // a riser, above the lower step
                    float b = y1[other];
                    if (kind != 0) face(out, null, d, x0, b, z0, x1, hi, z1, new float[]{0, 0, 16, 16}, front(u, open));
                    else face(out, null, d, x0, b, z0, x1, hi, z1, new float[]{u[0], 16 - hi, u[1], 16 - b}, textures.sprite("side"));
                }
                if (y0[other] > lo) {                           // below an upside-down stair's higher underside
                    float t = y0[other];
                    face(out, null, d, x0, lo, z0, x1, t, z1, new float[]{u[0], 16 - t, u[1], 16 - lo}, textures.sprite(down ? "side_d" : "side"));
                }
            }
        }
        List<BakedQuad>[] lists = new List[7];
        for (int i = 0; i < 7; i++) lists[i] = List.copyOf(out[i]);
        return lists;
    }

    /** A step front's tile: the half of the face this quarter's in, bordered at the ends that don't run on. */
    private TextureAtlasSprite front(float[] u, int open) {
        int half = u[0] < 8 ? 0 : 1, ends = ((open & 1) == 0 ? 1 : 0) | ((open & 2) == 0 ? 2 : 0);
        return textures.sprite("front_" + half + ends);
    }

    /** A quarter's u range on side d. */
    private static float[] span(Direction d, float x0, float z0) {
        return switch (d) {
            case NORTH -> new float[]{8 - x0, 16 - x0};
            case SOUTH -> new float[]{x0, x0 + 8};
            case WEST -> new float[]{z0, z0 + 8};
            default -> new float[]{8 - z0, 16 - z0};
        };
    }

    /** The face on side `side` of the box (x0, y0, z0)-(x1, y1, z1). */
    private static void face(List<BakedQuad>[] out, @Nullable Direction cull, Direction side, float x0, float y0, float z0, float x1, float y1, float z1,
                             float[] uv, TextureAtlasSprite sprite) {
        add(out, cull, x0, y0, z0, x1, y1, z1, side, uv, sprite);
    }

    private static void add(List<BakedQuad>[] out, @Nullable Direction cull, float x0, float y0, float z0, float x1, float y1, float z1,
                            Direction side, float[] uv, TextureAtlasSprite sprite) {
        BlockElementFace face = new BlockElementFace(cull, -1, "", new BlockFaceUV(uv, 0));
        BakedQuad quad = BAKERY.bakeQuad(new Vector3f(x0, y0, z0), new Vector3f(x1, y1, z1), face, sprite, side, BlockModelRotation.X0_Y0, null, true);
        out[cull == null ? 6 : cull.ordinal()].add(quad);
    }

    @Override public boolean useAmbientOcclusion() { return true; }

    @Override public boolean isGui3d() { return true; }

    @Override public boolean usesBlockLight() { return true; }

    @Override public boolean isCustomRenderer() { return false; }

    @Override public @NotNull TextureAtlasSprite getParticleIcon() { return textures.sprite("side"); }

    @Override public @NotNull ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }

    @Override public @NotNull ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }

    // ---- textures and registration -------------------------------------------------------------

    /** One road's textures (block/path/<road>_<name>), looked up in the block atlas as they're first wanted. */
    private static final class Textures {
        private final String road;
        private final boolean rosettes;
        private final Map<String, TextureAtlasSprite> sprites = new ConcurrentHashMap<>();

        Textures(String road, boolean rosettes) {
            this.road = road;
            this.rosettes = rosettes;
        }

        TextureAtlasSprite sprite(String name) {
            return sprites.computeIfAbsent(name, n -> Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
                    .getSprite(ResourceLocation.fromNamespaceAndPath("fourtwenty", "block/path/" + road + "_" + n)));
        }
    }

    /** Every road, slab and stair state gets this model (the blockstates name a plain stand-in). */
    public static void replace(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (int t = 0; t < ModTerpenes.PATHS.size(); t++) {
            Textures textures = new Textures(BuiltInRegistries.BLOCK.getKey(ModTerpenes.PATHS.get(t).get()).getPath(), t == 2);
            for (Block block : List.<Block>of(ModTerpenes.PATHS.get(t).get(), ModTerpenes.PATH_SLABS.get(t).get(), ModTerpenes.PATH_STAIRS.get(t).get())) {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    models.put(BlockModelShaper.stateToModelLocation(state), new RoadModel(state, textures));
                }
            }
        }
    }
}
