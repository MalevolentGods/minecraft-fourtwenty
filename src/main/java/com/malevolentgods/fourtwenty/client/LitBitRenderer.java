package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.LedRules;
import com.malevolentgods.fourtwenty.lights.LitBitBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/** A Lit Bit's glowing core: a cube inside its shell in the colour the server says it shows, full bright. Dark, it's a dim grey bead. */
public class LitBitRenderer implements BlockEntityRenderer<LitBitBlockEntity> {
    private static final ResourceLocation CORE = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/block/lit_bit_core.png");
    private static final float LO = 3 / 16f, HI = 13 / 16f;
    private static final int DARK = 0x2A2A2E;

    @Override public void render(@Nonnull LitBitBlockEntity lamp, float partial, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, int overlay) {
        int rgb = lamp.shown();
        boolean on = rgb != 0;
        if (!on) rgb = DARK;
        float r = LedRules.red(rgb) / 255f, g = LedRules.green(rgb) / 255f, b = LedRules.blue(rgb) / 255f;
        VertexConsumer v = buffers.getBuffer(RenderType.entityCutoutNoCull(CORE));
        Matrix4f m = pose.last().pose();
        int glow = on ? LightTexture.FULL_BRIGHT : light;
        // six faces of the core
        face(v, pose, m, r, g, b, glow, LO, LO, LO, HI, LO, LO, HI, HI, LO, LO, HI, LO, 0, 0, -1);
        face(v, pose, m, r, g, b, glow, HI, LO, HI, LO, LO, HI, LO, HI, HI, HI, HI, HI, 0, 0, 1);
        face(v, pose, m, r, g, b, glow, LO, LO, HI, LO, LO, LO, LO, HI, LO, LO, HI, HI, -1, 0, 0);
        face(v, pose, m, r, g, b, glow, HI, LO, LO, HI, LO, HI, HI, HI, HI, HI, HI, LO, 1, 0, 0);
        face(v, pose, m, r, g, b, glow, LO, HI, LO, HI, HI, LO, HI, HI, HI, LO, HI, HI, 0, 1, 0);
        face(v, pose, m, r, g, b, glow, LO, LO, HI, HI, LO, HI, HI, LO, LO, LO, LO, LO, 0, -1, 0);
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
