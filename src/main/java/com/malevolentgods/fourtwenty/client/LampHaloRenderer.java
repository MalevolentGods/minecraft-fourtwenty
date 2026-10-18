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
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;

/**
 * The glow around a lit lamp, as ProjectRed draws it: a box 0.05 blocks bigger than the lamp (or
 * trim plate) on every side, in the lamp's halo colour, blended additively like lightning, so a
 * soft coloured film shows over the neighbouring blocks' faces.
 */
public class LampHaloRenderer implements BlockEntityRenderer<LampBlockEntity> {
    public LampHaloRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(@Nonnull LampBlockEntity lamp, float partialTick, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers,
                       int light, int overlay) {
        var state = lamp.getBlockState();
        AABB box;
        int colour;
        if (state.getBlock() instanceof LampBlock block) {
            if (!state.getValue(LampBlock.LIT)) return;
            box = new AABB(0, 0, 0, 1, 1, 1);
            colour = block.color().getId();
        } else if (state.getBlock() instanceof LampTrimBlock trim) {
            box = state.getShape(lamp.getLevel(), lamp.getBlockPos()).bounds();
            colour = trim.color().getId();
        } else return;
        int rgba = LampGlow.rgba(colour);
        box = box.inflate(LampGlow.EXPAND);
        cube(pose, buffers.getBuffer(HaloRenderType.HALO), box,
                (rgba >>> 24) & 0xFF, (rgba >> 16) & 0xFF, (rgba >> 8) & 0xFF, rgba & 0xFF);
    }

    private static void cube(PoseStack pose, VertexConsumer out, AABB b, int r, int g, int bl, int a) {
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
        for (var q : quads) {
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
