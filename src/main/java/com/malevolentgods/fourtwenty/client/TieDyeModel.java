package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.malevolentgods.fourtwenty.tiedye.TieDyeBlockEntity;
import com.malevolentgods.fourtwenty.tiedye.TieDyeBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A tie-dyed full block that spreads its pattern over a square of blocks (specs/018-tie-dye-shack FR-013c, research R11). Each face
 * reads its plane's fit (square size n, cell i, j) from the block entity's model data and draws the pattern's n-sized sprite, its four
 * slot layers tinted 0-3, over just that cell's slice. u runs with the world's x (or z), v from the top of a wall or the low z of a floor,
 * so slices meet across blocks. Quads are cached per face, pattern and fit.
 */
final class TieDyeModel extends BakedModelWrapper<BakedModel> {
    private static final Map<Long, List<BakedQuad>> CACHE = new ConcurrentHashMap<>();

    TieDyeModel(BakedModel original) { super(original); }

    static void clearCache() { CACHE.clear(); }

    @Override public @Nonnull List<BakedQuad> getQuads(BlockState state, Direction side, @Nonnull RandomSource rand, @Nonnull ModelData data, RenderType type) {
        if (state == null || side == null) return super.getQuads(state, side, rand, data, type);
        int[] fits = data.get(TieDyeBlockEntity.FITS);
        int plane = plane(side);
        int n = 1, i = 0, j = 0;
        if (fits != null && fits.length >= 9) {
            n = Math.max(1, Math.min(5, fits[plane * 3]));
            i = fits[plane * 3 + 1];
            j = fits[plane * 3 + 2];
        }
        int pattern = state.getValue(TieDyeBlocks.PATTERN);
        long key = ((((long) side.ordinal() * 8 + pattern) * 8 + n) * 8 + i) * 8 + j;
        final int fn = n, fi = i, fj = j;
        return CACHE.computeIfAbsent(key, k -> bake(side, pattern, fn, fi, fj));
    }

    /** 0 = a wall facing x, 1 = a wall facing z, 2 = a floor or ceiling (TieDyeFits' planes). */
    private static int plane(Direction d) {
        return switch (d.getAxis()) {
            case X -> 0;
            case Z -> 1;
            case Y -> 2;
        };
    }

    private static List<BakedQuad> bake(Direction side, int pattern, int n, int i, int j) {
        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        float[][] corners = corners(side);
        List<BakedQuad> out = new ArrayList<>(TieDyeRules.SLOTS);
        for (int slot = 0; slot < TieDyeRules.SLOTS; slot++) {
            TextureAtlasSprite sprite = atlas.getSprite(ResourceLocation.fromNamespaceAndPath("fourtwenty",
                    "block/tie_dye/" + TieDyeRules.PATTERNS.get(pattern) + "_" + n + "_" + slot));
            QuadBakingVertexConsumer quad = new QuadBakingVertexConsumer();
            quad.setSprite(sprite);
            quad.setDirection(side);
            quad.setTintIndex(slot);
            quad.setShade(true);
            quad.setHasAmbientOcclusion(true);
            var normal = side.getNormal();
            for (float[] c : corners) {
                float[] uv = uv(side, c, n, i, j);
                quad.addVertex(c[0], c[1], c[2]).setColor(-1).setUv(sprite.getU(uv[0]), sprite.getV(uv[1])).setUv2(0, 0)
                        .setNormal(normal.getX(), normal.getY(), normal.getZ());
            }
            out.add(quad.bakeQuad());
        }
        return out;
    }

    /** Where on the square's sprite (0-1) a corner of this face falls. */
    private static float[] uv(Direction side, float[] c, int n, int i, int j) {
        float u, v;
        switch (side.getAxis()) {
            case X -> { u = (i + c[2]) / n; v = (n - 1 - j + (1 - c[1])) / n; }
            case Z -> { u = (i + c[0]) / n; v = (n - 1 - j + (1 - c[1])) / n; }
            default -> { u = (i + c[0]) / n; v = (j + c[2]) / n; }
        }
        return new float[] {u, v};
    }

    /** A full block's face, corners anticlockwise seen from outside (as vanilla's FaceInfo). */
    private static float[][] corners(Direction side) {
        return switch (side) {
            case DOWN -> new float[][] {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}};
            case UP -> new float[][] {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}};
            case NORTH -> new float[][] {{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}};
            case SOUTH -> new float[][] {{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}};
            case WEST -> new float[][] {{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}};
            case EAST -> new float[][] {{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}};
        };
    }
}
