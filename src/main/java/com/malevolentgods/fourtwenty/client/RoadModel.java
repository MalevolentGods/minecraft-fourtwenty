package com.malevolentgods.fourtwenty.client;

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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The model of every road, road slab and road stair state, built as the chunk is drawn from what
 * {@link RoadShape} says of each quarter: its top bordered where the surface ends, steps or is
 * covered (one of the quarter-top textures tools/generate_paths.py makes for every arrangement of
 * borders and notches), its underside the same, and each side split by the quarter across it: a
 * bordered step face above a lower step of the road, the cut through the road elsewhere: a wall read as
 * one face, its top border running along its top and corner bands up its ends where it turns a corner,
 * meets another block or rises above the wall beside it (wall tiles for every arrangement of those).
 * Standing on a road block, its faces reach down the pixel to that block's top, so the two read as one
 * face; under any other solid block, the road below fills that pixel itself. Quarters: 0 nw, 1 ne, 2 sw, 3 se (bit 0 east, bit 1 south).
 */
public final class RoadModel implements IDynamicBakedModel {
    private static final ModelProperty<Key> KEY = new ModelProperty<>();
    private static final String[] QUARTERS = {"nw", "ne", "sw", "se"};
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final FaceBakery BAKERY = new FaceBakery();
    /** Per-quarter flags in the key. */
    private static final int COVERED = 1, CAPPED = 2, ON_ROAD = 4, SUPPORTED = 8;

    /** What a block's drawing depends on: per quarter its top, underside and flags; per quarter and side what's across it. */
    private static final class Key {
        final int[] v;

        Key(int[] v) { this.v = v; }

        @Override public boolean equals(Object o) { return o instanceof Key k && Arrays.equals(v, k.v); }

        @Override public int hashCode() { return Arrays.hashCode(v); }
    }

    private final Textures textures;
    private final BlockState state;
    private final int[] y0 = new int[4], y1 = new int[4];
    private final Map<Key, List<BakedQuad>[]> quads = new ConcurrentHashMap<>();
    private volatile Key alone;

    private RoadModel(BlockState state, Textures textures) {
        this.state = state;
        this.textures = textures;
        for (int q = 0; q < 4; q++) {
            y0[q] = RoadShape.bottom(state, sx(q), sz(q));
            y1[q] = RoadShape.top(state, sx(q), sz(q));
        }
    }

    private static int sx(int q) { return (q & 1) == 0 ? -1 : 1; }

    private static int sz(int q) { return (q & 2) == 0 ? -1 : 1; }

    /** A side's packed facts: road across (1), its underside (5 bits) and top (5), covered (1), the step face (10), its open ends (2), the side open to view (1), the wall's end bands (2 + 2). */
    private static Key key(BlockGetter level, BlockPos pos, BlockState state) {
        int[] v = new int[4 * 3 + 16];
        for (int q = 0; q < 4; q++) {
            int sx = sx(q), sz = sz(q);
            v[3 * q] = RoadShape.border(level, pos, state, sx, sz);
            v[3 * q + 1] = RoadShape.underBorder(level, pos, state, sx, sz);
            v[3 * q + 2] = (RoadShape.covered(level, pos, state, sx, sz) ? COVERED : 0) | (RoadShape.capped(level, pos, state, sx, sz) ? CAPPED : 0)
                    | (RoadShape.standsOnRoad(level, pos, state, sx, sz) ? ON_ROAD : 0) | (RoadShape.supported(level, pos, state, sx, sz) ? SUPPORTED : 0);
            for (int i = 0; i < SIDES.length; i++) {
                Direction d = SIDES[i];
                RoadShape.Across a = RoadShape.across(level, pos, state, sx, sz, d);
                int face = RoadShape.stepFace(level, pos, state, sx, sz, d);
                int ends = face == 0 ? 0 : (RoadShape.stepRunsOn(level, pos, state, sx, sz, d, uLow(d)) ? 1 : 0)
                        | (RoadShape.stepRunsOn(level, pos, state, sx, sz, d, uLow(d).getOpposite()) ? 2 : 0);
                v[12 + 4 * q + i] = (a.road() ? 1 : 0) | a.bottom() << 1 | a.top() << 6 | (a.covered() ? 1 << 11 : 0)
                        | face << 12 | ends << 22 | (RoadShape.open(level, pos, state, sx, sz, d) ? 1 << 24 : 0)
                        | band(RoadShape.wallEnd(level, pos, state, sx, sz, d, uLow(d))) << 25
                        | band(RoadShape.wallEnd(level, pos, state, sx, sz, d, uLow(d).getOpposite())) << 27;
            }
        }
        return new Key(v);
    }

    /** A wall end's band, packed: none, the whole height, from 7, from 15. */
    private static int band(int from) {
        return from < 0 ? 0 : from == 0 ? 1 : from == 7 ? 2 : 3;
    }

    private static final String BANDS = "nf7h";

    @Override public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData data) {
        return ModelData.of(KEY, key(level, pos, state));
    }

    @Override public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                       @NotNull ModelData data, @Nullable RenderType renderType) {
        Key key = data.get(KEY);
        if (key == null) {                                      // drawn with no level round it (moving, breaking): a lone block
            if (alone == null) alone = key(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, this.state);
            key = alone;
        }
        return quads.computeIfAbsent(key, this::build)[side == null ? 6 : side.ordinal()];
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
            int top = key.v[3 * q], under = key.v[3 * q + 1], flags = key.v[3 * q + 2];
            float x0 = sx < 0 ? 0 : 8, z0 = sz < 0 ? 0 : 8, x1 = x0 + 8, z1 = z0 + 8;
            int lo = y0[q], hi = y1[q];
            Direction ns = sz < 0 ? Direction.NORTH : Direction.SOUTH, ew = sx < 0 ? Direction.WEST : Direction.EAST;
            // Top, unless something stands on it: the yellow brick road's rosette where its border turns a block corner.
            if ((flags & COVERED) == 0) {
                boolean rosette = textures.rosettes && (top & bit(ns)) != 0 && (top & bit(ew)) != 0;
                String name = String.format("top_%s_%x%x%s", QUARTERS[q], top & 15, top >> 4, rosette ? "r" : "");
                add(out, null, Direction.UP, x0, hi, z0, x1, hi, z1, new float[]{0, 0, 16, 16}, textures.sprite(name));
            }
            // Underside, unless it rests on something.
            if ((flags & SUPPORTED) == 0) {
                String name = String.format("under_%s_%x%x", QUARTERS[q], under & 15, under >> 4);
                add(out, lo == 0 ? Direction.DOWN : null, Direction.DOWN, x0, lo, z0, x1, lo, z1, new float[]{0, 0, 16, 16}, textures.sprite(name));
            }
            // Sides.
            // A wall's tile: its top edging at its own top (a low wall's at 7), dropped in a stack; its footing dropped standing on a road block.
            String stack = (hi == 7 ? "l" : (flags & CAPPED) != 0 ? "u" : "") + ((flags & ON_ROAD) != 0 ? "d" : "");
            boolean reach = (flags & ON_ROAD) != 0;                 // down the pixel to the road block below
            for (int i = 0; i < SIDES.length; i++) {
                Direction d = SIDES[i];
                int f = key.v[12 + 4 * q + i];
                boolean road = (f & 1) != 0, outer = d == ns || d == ew;
                int aBottom = (f >> 1) & 31, aTop = (f >> 6) & 31, face = (f >> 12) & 1023, ends = (f >> 22) & 3;
                float[] u = span(d, x0, z0);
                TextureAtlasSprite cut = textures.sprite("wall_" + (u[0] < 8 ? 0 : 1) + "_" + (stack.isEmpty() ? "x" : stack) + "_"
                        + BANDS.charAt((f >> 25) & 3) + BANDS.charAt((f >> 27) & 3));
                if (road) {
                    if (aTop < hi) {                            // above the step across: its face, or the cut where it's a wall
                        int from = Math.max(aTop, lo);
                        if (face != 0) side(out, null, d, x0, from, z0, x1, hi, z1, new float[]{0, 0, 16, 16}, front(u, ends));
                        else wall(out, null, d, x0, from, z0, x1, hi, z1, u, cut, reach && from == 0);
                    }
                    if (aBottom > lo) {                         // under the quarter across, where it overhangs
                        wall(out, null, d, x0, lo, z0, x1, Math.min(aBottom, hi), z1, u, cut, reach && lo == 0);
                    }
                    // Standing on a road block with this side hidden down to the bottom by the road beside it:
                    // still reach down the pixel to the block below, which is open under the one beside.
                    boolean reachedDown = (aTop < hi && Math.max(aTop, lo) == 0 && face == 0) || aBottom > lo;
                    if (outer && reach && lo == 0 && !reachedDown) {
                        side(out, null, d, x0, -1, z0, x1, 0, z1, new float[]{0, 15, 16, 16}, cut);
                    }
                } else if (outer) {
                    if (face != 0) side(out, d, d, x0, reach ? -1 : lo, z0, x1, hi, z1, new float[]{0, 0, 16, 16}, front(u, ends));
                    else wall(out, d, d, x0, lo, z0, x1, hi, z1, u, cut, reach && lo == 0);
                }
                // The pixel up to a full block, under a solid block that isn't a road block (a road block reaches down itself).
                boolean coveredAcross = road && aTop == 15 && (f & (1 << 11)) != 0;
                if (outer && (flags & COVERED) != 0 && (flags & CAPPED) == 0 && hi == 15 && !coveredAcross) {
                    side(out, null, d, x0, 15, z0, x1, 16, z1, new float[]{0, 0, 16, 1}, cut);
                }
            }
        }
        List<BakedQuad>[] lists = new List[7];
        for (int i = 0; i < 7; i++) lists[i] = List.copyOf(out[i]);
        return lists;
    }

    private static int bit(Direction d) {
        return switch (d) {
            case NORTH -> RoadShape.N;
            case EAST -> RoadShape.E;
            case SOUTH -> RoadShape.S;
            default -> RoadShape.W;
        };
    }

    /** A step face's tile: the half of the face this quarter's in, bordered at the ends that don't run on. */
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

    /** A wall face from ya to yb (its tile's rows pinned to the block's height), reaching one pixel further down where `reach`. */
    private static void wall(List<BakedQuad>[] out, @Nullable Direction cull, Direction d, float x0, float ya, float z0, float x1, float yb, float z1,
                             float[] u, TextureAtlasSprite tile, boolean reach) {
        side(out, cull, d, x0, ya, z0, x1, yb, z1, new float[]{0, 16 - yb, 16, 16 - ya}, tile);
        if (reach) side(out, null, d, x0, -1, z0, x1, 0, z1, new float[]{0, 15, 16, 16}, tile);
    }

    private static void side(List<BakedQuad>[] out, @Nullable Direction cull, Direction d, float x0, float ya, float z0, float x1, float yb, float z1,
                             float[] uv, TextureAtlasSprite sprite) {
        add(out, cull, d, x0, ya, z0, x1, yb, z1, uv, sprite);
    }

    /** The face on side `side` of the box (x0, y0, z0)-(x1, y1, z1). */
    private static void add(List<BakedQuad>[] out, @Nullable Direction cull, Direction side, float x0, float y0, float z0, float x1, float y1, float z1,
                            float[] uv, TextureAtlasSprite sprite) {
        BlockElementFace face = new BlockElementFace(cull, -1, "", new BlockFaceUV(uv, 0));
        // Smooth lighting on every face: roads give off their own light, so a lone road's top isn't shaded dark.
        out[cull == null ? 6 : cull.ordinal()].add(BAKERY.bakeQuad(new Vector3f(x0, y0, z0), new Vector3f(x1, y1, z1), face, sprite, side, BlockModelRotation.X0_Y0, null, true));
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
