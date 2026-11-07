package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.LampGlow;
import com.malevolentgods.fourtwenty.domain.LedRules;
import com.malevolentgods.fourtwenty.lights.LitBitBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/**
 * A lit Lit Bit looks like a Four Twenty lamp in its colour: the lamp's lit face, tinted, full bright, and the lamp's halo round it. The
 * block's own model is the lamp's unlit face, so a dark Lit Bit is a lamp switched off. Drawn every frame, so a show changing colour never
 * rebuilds chunks.
 */
public class LitBitRenderer implements BlockEntityRenderer<LitBitBlockEntity> {
    private static final ResourceLocation LIT = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/block/white_lamp_on.png");
    private static final float LO = -0.002f, HI = 1.002f;

    @Override public void render(@Nonnull LitBitBlockEntity lamp, float partial, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, int overlay) {
        int rgb = lamp.shown();
        if (rgb == 0) return;
        float r = LedRules.red(rgb) / 255f, g = LedRules.green(rgb) / 255f, b = LedRules.blue(rgb) / 255f;
        VertexConsumer v = buffers.getBuffer(RenderType.entityCutoutNoCull(LIT));
        Matrix4f m = pose.last().pose();
        int glow = LightTexture.FULL_BRIGHT;
        face(v, pose, m, r, g, b, glow, LO, LO, LO, HI, LO, LO, HI, HI, LO, LO, HI, LO, 0, 0, -1);
        face(v, pose, m, r, g, b, glow, HI, LO, HI, LO, LO, HI, LO, HI, HI, HI, HI, HI, 0, 0, 1);
        face(v, pose, m, r, g, b, glow, LO, LO, HI, LO, LO, LO, LO, HI, LO, LO, HI, HI, -1, 0, 0);
        face(v, pose, m, r, g, b, glow, HI, LO, LO, HI, LO, HI, HI, HI, HI, HI, HI, LO, 1, 0, 0);
        face(v, pose, m, r, g, b, glow, LO, HI, LO, HI, HI, LO, HI, HI, HI, LO, HI, HI, 0, 1, 0);
        face(v, pose, m, r, g, b, glow, LO, LO, HI, HI, LO, HI, HI, LO, LO, LO, LO, LO, 0, -1, 0);
        // the lamps' halo, in this lamp's colour, fainter as it dims; touching lit Lit Bits share one glow
        var level = lamp.getLevel();
        boolean[] joined = new boolean[6];
        if (level != null) {
            for (Direction side : Direction.values()) {
                joined[side.ordinal()] = level.getBlockEntity(lamp.getBlockPos().relative(side)) instanceof LitBitBlockEntity other && other.shown() != 0;
            }
        }
        int halo = LampGlow.forRgb(rgb);
        int alpha = Math.round((halo & 0xFF) * Math.max(r, Math.max(g, b)));
        LampHaloRenderer.halo(pose, buffers, new AABB(0, 0, 0, 1, 1, 1), halo & 0xFFFFFF00 | alpha, joined);
    }

    @Override public int getViewDistance() { return 256; }

    @Override public @Nonnull AABB getRenderBoundingBox(@Nonnull LitBitBlockEntity lamp) {
        return new AABB(lamp.getBlockPos()).inflate(LampGlow.EXPAND);
    }

    private static void face(VertexConsumer v, PoseStack pose, Matrix4f m, float r, float g, float b, int light,
                             float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
                             float nx, float ny, float nz) {
        vertex(v, pose, m, r, g, b, light, x1, y1, z1, 0, 1, nx, ny, nz);
        vertex(v, pose, m, r, g, b, light, x2, y2, z2, 1, 1, nx, ny, nz);
        vertex(v, pose, m, r, g, b, light, x3, y3, z3, 1, 0, nx, ny, nz);
        vertex(v, pose, m, r, g, b, light, x4, y4, z4, 0, 0, nx, ny, nz);
    }

    private static void vertex(VertexConsumer v, PoseStack pose, Matrix4f m, float r, float g, float b, int light, float x, float y, float z, float u, float uv,
                               float nx, float ny, float nz) {
        v.addVertex(m, x, y, z).setColor(r, g, b, 1f).setUv(u, uv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), nx, ny, nz);
    }
}
