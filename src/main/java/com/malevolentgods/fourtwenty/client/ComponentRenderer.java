package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.electronics.Shown;
import com.malevolentgods.fourtwenty.electronics.ComponentBlock;
import com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity;
import com.malevolentgods.fourtwenty.electronics.ComponentType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Set;

/**
 * What a component shows of its circuit (docs/electronics/style-pass.md), drawn over its still model: the plinth's light line while it's
 * active, and each part's own sign of life, a Resinator's rod warming with the power it burns, a Kief Cap filling with its charge, rings
 * of light round a Spliff Coil with its current, a hitter's bowl glowing while it conducts. Glows are fullbright. Coordinates are in
 * voxels for a model facing north, turned with the block.
 */
public class ComponentRenderer implements BlockEntityRenderer<ComponentBlockEntity> {
    /** The parts with the new look; the rest keep their plain models for now. */
    static final Set<ComponentType> LAB = EnumSet.of(ComponentType.RESINATOR, ComponentType.KIEF_CAP, ComponentType.SPLIFF_COIL,
            ComponentType.ONE_HITTER, ComponentType.ZEN_HITTER);
    private static final int LIME = 0xB8F06A, LIME_OFF = 0x34401F;

    public ComponentRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(@Nonnull ComponentBlockEntity be, float partialTick, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, int overlay) {
        ComponentType type = be.type();
        if (type == null || !LAB.contains(type)) return;
        int shown = be.shown();
        float time = be.getLevel() == null ? 0 : (be.getLevel().getGameTime() + partialTick) / 20f;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // as ComponentModel turns the still model: to any of the 24 orientations
        pose.mulPose(com.malevolentgods.fourtwenty.electronics.Orientation.rotation(be.getBlockState().getValue(ComponentBlock.FACING),
                be.getBlockState().getValue(ComponentBlock.SPIN)));
        pose.translate(-0.5, -0.5, -0.5);
        VertexConsumer solid = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        TextureAtlasSprite glow = sprite("lab_glow");
        // a lime ring round each gland while it's active (two-pin parts: front and back)
        if (Shown.active(shown) && type.layout == ComponentType.Layout.TWO_PIN) {
            VertexConsumer add = buffers.getBuffer(RenderType.lightning());
            ring(pose, add, 5.7, 5.7, 0.9, 10.3, 10.3, 1.4, LIME, 0.55f);
            ring(pose, add, 5.7, 5.7, 14.6, 10.3, 10.3, 15.1, LIME, 0.55f);
        }
        int level = Shown.level(shown);
        switch (type) {
            case RESINATOR -> {
                // the rod: dark resin when cold, amber as it warms, white past its rating
                float heat = level / 15f;
                int c = heat <= 0 ? 0x6A4A2A : heat < 2 / 3f ? lerp(0x8A5A26, 0xFFB54A, heat * 1.5f) : lerp(0xFFB54A, 0xFFFFFF, (heat - 2 / 3f) * 3);
                box(pose, solid, glow, 6.5, 6.5, 3.5, 9.5, 9.5, 12.5, c, heat > 0 ? LightTexture.FULL_BRIGHT : light);
            }
            case KIEF_CAP -> {
                if (level > 0) {
                    float f = level / 15f;
                    box(pose, solid, sprite("lab_kief"), 4.8, 4.8, 3.2, 11.2, 4.8 + 6.4 * f, 12.8, lerp(0x8C7A50, 0xFFFFFF, 0.4f + 0.6f * f), LightTexture.FULL_BRIGHT);
                }
            }
            case SPLIFF_COIL -> {
                if (level > 0) {
                    // three rings of light drifting steadily along it, fading in at one end and out at the other, brighter with more current
                    VertexConsumer add = buffers.getBuffer(RenderType.lightning());
                    for (int r = 0; r < 3; r++) {
                        float phase = ((time * 0.5f + r / 3f) % 1f + 1f) % 1f;
                        float z = 3 + phase * 10.4f;
                        float a = (0.12f + 0.03f * level) * (float) Math.sin(Math.PI * phase);
                        ring(pose, add, 5.2, 5.2, z, 10.8, 10.8, z + 0.4, 0x9DE8FF, a);
                    }
                }
            }
            case ONE_HITTER, ZEN_HITTER -> {
                if (level > 0) {
                    float f = level / 15f;
                    int ember = type == ComponentType.ZEN_HITTER ? lerp(0x2E8A52, 0x9DFFC4, f) : lerp(0x8A2A10, 0xFFB060, f);
                    box(pose, solid, glow, 6.5, 10.35, 2.7, 9.5, 10.5, 5.1, ember, LightTexture.FULL_BRIGHT);
                    VertexConsumer add = buffers.getBuffer(RenderType.lightning());
                    ring(pose, add, 7.3, 7.3, 6.7, 8.7, 8.7, 13.2, ember, 0.15f + 0.35f * f);
                }
            }
            default -> {}
        }
        pose.popPose();
    }

    private static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(ResourceLocation.fromNamespaceAndPath("fourtwenty", "block/component/" + name));
    }

    static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) ((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = (int) ((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    /** A textured box from (x0, y0, z0) to (x1, y1, z1) in voxels, in colour {@code rgb}, at light {@code light}. */
    static void box(PoseStack pose, VertexConsumer vc, TextureAtlasSprite sp, double x0, double y0, double z0, double x1, double y1, double z1, int rgb, int light) {
        float a = (float) x0 / 16, b = (float) y0 / 16, c = (float) z0 / 16, d = (float) x1 / 16, e = (float) y1 / 16, f = (float) z1 / 16;
        var p = pose.last();
        int argb = 0xFF000000 | rgb;
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        quad(p, vc, argb, light, u0, v0, u1, v1, 0, -1, 0, a, b, c, d, b, c, d, b, f, a, b, f);
        quad(p, vc, argb, light, u0, v0, u1, v1, 0, 1, 0, a, e, f, d, e, f, d, e, c, a, e, c);
        quad(p, vc, argb, light, u0, v0, u1, v1, 0, 0, -1, d, b, c, a, b, c, a, e, c, d, e, c);
        quad(p, vc, argb, light, u0, v0, u1, v1, 0, 0, 1, a, b, f, d, b, f, d, e, f, a, e, f);
        quad(p, vc, argb, light, u0, v0, u1, v1, -1, 0, 0, a, b, c, a, b, f, a, e, f, a, e, c);
        quad(p, vc, argb, light, u0, v0, u1, v1, 1, 0, 0, d, b, f, d, b, c, d, e, c, d, e, f);
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc, int argb, int light, float u0, float v0, float u1, float v1, float nx, float ny, float nz,
                             float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
        vc.addVertex(p, x1, y1, z1).setColor(argb).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x2, y2, z2).setColor(argb).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x3, y3, z3).setColor(argb).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x4, y4, z4).setColor(argb).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    /** An additive glowing box, untextured (lightning's render type): a film of light. */
    static void ring(PoseStack pose, VertexConsumer vc, double x0, double y0, double z0, double x1, double y1, double z1, int rgb, float alpha) {
        float a = (float) x0 / 16, b = (float) y0 / 16, c = (float) z0 / 16, d = (float) x1 / 16, e = (float) y1 / 16, f = (float) z1 / 16;
        var m = pose.last();
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, bl = (rgb & 255) / 255f;
        float[][] faces = {
                {a, b, c, d, b, c, d, b, f, a, b, f}, {a, e, f, d, e, f, d, e, c, a, e, c},
                {d, b, c, a, b, c, a, e, c, d, e, c}, {a, b, f, d, b, f, d, e, f, a, e, f},
                {a, b, c, a, b, f, a, e, f, a, e, c}, {d, b, f, d, b, c, d, e, c, d, e, f}};
        for (float[] q : faces) {
            for (int k = 0; k < 4; k++) vc.addVertex(m, q[k * 3], q[k * 3 + 1], q[k * 3 + 2]).setColor(r, g, bl, alpha);
        }
    }
}
