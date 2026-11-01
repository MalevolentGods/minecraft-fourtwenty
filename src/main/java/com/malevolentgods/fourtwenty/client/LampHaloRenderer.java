package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.block.LampBlock;
import com.malevolentgods.fourtwenty.block.LampTrimBlock;
import com.malevolentgods.fourtwenty.blockentity.LampBlockEntity;
import com.malevolentgods.fourtwenty.domain.LampGlow;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;

/**
 * The glow around a lit lamp, as ProjectRed draws it: a box 0.05 blocks bigger than the lamp (or
 * trim plate) on every side, in the lamp's halo colour, blended additively like lightning, so a
 * soft coloured film shows over the neighbouring blocks' faces.
 *
 * <p>The glow spilling onto neighbouring blocks is the effect, with two exceptions where the halo
 * neither extends nor draws a face: another lit lamp (or a trim on the same face), so touching lamps
 * share one glow instead of overlapping into bright seams; and a ladder, which the film would
 * otherwise cover.
 */
public class LampHaloRenderer implements BlockEntityRenderer<LampBlockEntity> {
    public LampHaloRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(@Nonnull LampBlockEntity lamp, float partialTick, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers,
                       int light, int overlay) {
        var state = lamp.getBlockState();
        var level = lamp.getLevel();
        var pos = lamp.getBlockPos();
        if (level == null) return;
        AABB box;
        int rgba;
        boolean[] joined = new boolean[6];   // by Direction ordinal: down, up, north, south, west, east
        if (state.getBlock() instanceof LampBlock block) {
            if (!state.getValue(LampBlock.LIT)) return;
            box = new AABB(0, 0, 0, 1, 1, 1);
            rgba = block.halo();
            for (var side : Direction.values()) {
                var next = level.getBlockState(pos.relative(side));
                joined[side.ordinal()] = next.getBlock() instanceof LampBlock && next.getValue(LampBlock.LIT) || isLadder(next);
            }
        } else if (state.getBlock() instanceof LampTrimBlock trim) {
            box = state.getShape(level, pos).bounds();
            rgba = trim.halo();
            var facing = state.getValue(LampTrimBlock.FACING);
            // The plate's front and back faces lie inside its own block space; only its edges meet neighbours.
            for (var side : Direction.values()) {
                if (side.getAxis() == facing.getAxis()) continue;
                var next = level.getBlockState(pos.relative(side));
                joined[side.ordinal()] = next.getBlock() instanceof LampTrimBlock && next.getValue(LampTrimBlock.FACING) == facing || isLadder(next);
            }
        } else return;
        halo(pose, buffers, box, rgba, joined);
        LampFilm.placed(pos, rgba >>> 8);
    }

    private static boolean isLadder(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof net.minecraft.world.level.block.LadderBlock;
    }

    /** Draw the halo for a lamp occupying {@code box} (block-local), in colour {@code rgba} (0xRRGGBBAA). */
    static void halo(PoseStack pose, MultiBufferSource buffers, AABB box, int rgba) {
        halo(pose, buffers, box, rgba, new boolean[6]);
    }

    /** As above, but sides marked {@code joined} (Direction ordinal) neither extend nor draw a face. */
    static void halo(PoseStack pose, MultiBufferSource buffers, AABB box, int rgba, boolean[] joined) {
        double e = LampGlow.EXPAND;
        var grown = new AABB(
                box.minX - (joined[Direction.WEST.ordinal()] ? 0 : e), box.minY - (joined[Direction.DOWN.ordinal()] ? 0 : e),
                box.minZ - (joined[Direction.NORTH.ordinal()] ? 0 : e), box.maxX + (joined[Direction.EAST.ordinal()] ? 0 : e),
                box.maxY + (joined[Direction.UP.ordinal()] ? 0 : e), box.maxZ + (joined[Direction.SOUTH.ordinal()] ? 0 : e));
        cube(pose, buffers.getBuffer(HaloRenderType.HALO), grown, joined,
                (rgba >>> 24) & 0xFF, (rgba >> 16) & 0xFF, (rgba >> 8) & 0xFF, rgba & 0xFF);
    }

    private static void cube(PoseStack pose, VertexConsumer out, AABB b, boolean[] skip, int r, int g, int bl, int a) {
        var m = pose.last().pose();
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ, x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        float[][] quads = {
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1},   // down
                {x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0},   // up
                {x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0},   // north
                {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},   // south
                {x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0},   // west
                {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1},   // east
        };
        for (int face = 0; face < 6; face++) {
            if (skip[face]) continue;
            var q = quads[face];
            for (int v = 0; v < 12; v += 3) out.addVertex(m, q[v], q[v + 1], q[v + 2]).setColor(r, g, bl, a);
        }
    }

    @Override public int getViewDistance() { return 256; }

    @Override public AABB getRenderBoundingBox(@Nonnull LampBlockEntity lamp) {
        return new AABB(lamp.getBlockPos()).inflate(LampGlow.EXPAND);
    }

    /** Position-colour quads, additive (lightning) blending, no depth write: ProjectRed's halo pass. */
    static final class HaloRenderType extends RenderType {
        static final RenderType HALO = create("fourtwenty:lamp_halo", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
                2048, false, true, CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setCullState(CULL)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));

        private HaloRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
                               Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sort, setup, clear);
        }
    }
}
