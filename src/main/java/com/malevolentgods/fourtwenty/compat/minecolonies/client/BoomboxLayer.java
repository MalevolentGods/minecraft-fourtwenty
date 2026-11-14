package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.malevolentgods.fourtwenty.client.LampHaloRenderer;
import com.malevolentgods.fourtwenty.compat.minecolonies.builder.Album;
import com.minecolonies.api.client.render.modeltype.CitizenModel;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/**
 * The Midnight Machine on a Lifted Builder's back (specs/014-midnight-machine FR-008): concept B, the Synth Machine,
 * as a baked model (album art up top, glowing rails, knobs, antenna), plus six VU bars drawn here as stacked glowing
 * segments, each following its own frequency band at the moment of the song this player hears.
 */
public final class BoomboxLayer extends RenderLayer<AbstractEntityCitizen, CitizenModel<AbstractEntityCitizen>> {
    public static final ModelResourceLocation BODY =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("fourtwenty", "midnight_machine/body"));

    /** Bass to treble, left to right as you look at her back: the album's sunset. */
    private static final int[] BAR_COLOURS = {0xF0CB73, 0xE16B57, 0xF34659, 0xED2E65, 0xC30B59, 0x8B2365};
    private static final int SEGMENTS = 8;
    private static final float PX = 1 / 16f;

    public BoomboxLayer(RenderLayerParent<AbstractEntityCitizen, CitizenModel<AbstractEntityCitizen>> parent) {
        super(parent);
    }

    @Override
    public void render(@Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, @Nonnull AbstractEntityCitizen citizen,
                       float limb, float amount, float partial, float age, float yaw, float pitch) {
        int id = citizen.getId();
        if (!MidnightMachineClient.lifted(id)) return;
        var mc = Minecraft.getInstance();
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);

        // The model: x 3..13 (8 = her middle), y 0 = her body's bottom going up, z mirrored. The body part's space has
        // y pointing down from her shoulders (12 px of body) and +z out of her back.
        pose.pushPose();
        pose.translate(0, 12 * PX, 0);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.translate(-0.5f, 0, 0);
        var model = mc.getModelManager().getModel(BODY);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), null, model,
                1, 1, 1, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, Sheets.cutoutBlockSheet());
        pose.popPose();

        // The VU bars, in the body part's pixel space: across x -4.5..4.3, rising from y 11.3 up (y points down here),
        // just out from the meter window at z 6.2.
        boolean playing = MidnightMachineClient.playing(id);
        int track = MidnightMachineClient.track(id);
        long ms = MidnightMachineClient.positionMs(id, partial);
        VertexConsumer glow = buffers.getBuffer(LampHaloRenderer.HaloRenderType.HALO);
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < 6; i++) {
            float level = playing ? Album.level(track, i, ms) : 0.12f + 0.08f * Mth.sin(age * 0.15f + i);
            int lit = Math.max(1, Math.round(level * SEGMENTS));
            float x0 = (-4.5f + i * 1.5f) * PX, x1 = x0 + 0.8f * PX;
            for (int s = 0; s < lit; s++) {
                float yBottom = (11.3f - s * 0.5f) * PX, yTop = yBottom - 0.4f * PX;
                int alpha = s == lit - 1 ? 0xFF : 0xD0;   // the top segment brightest, like a peak light
                box(glow, m, x0, yTop, 6.25f * PX, x1, yBottom, 6.45f * PX, alpha << 24 | BAR_COLOURS[i]);
            }
        }
        pose.popPose();
    }

    private static void box(VertexConsumer v, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        // the face toward the viewer behind her (+z), plus thin sides so the bars read at an angle
        quad(v, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, argb);
        quad(v, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, argb);
        quad(v, m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, argb);
        quad(v, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, argb);
    }

    private static void quad(VertexConsumer v, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int argb) {
        v.addVertex(m, ax, ay, az).setColor(argb);
        v.addVertex(m, bx, by, bz).setColor(argb);
        v.addVertex(m, cx, cy, cz).setColor(argb);
        v.addVertex(m, dx, dy, dz).setColor(argb);
    }
}
