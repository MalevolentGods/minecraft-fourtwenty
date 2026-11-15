package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.Album;
import com.minecolonies.api.client.render.modeltype.CitizenModel;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.data.ModelData;

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
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("fourtwenty", "block/midnight_machine/white");

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

        // The model: x 3..13 (8 = her middle), y 0 = her body's bottom going up (18 tall, so it rises behind her
        // head), z mirrored. The body part's space has y pointing down from her shoulders (12 px of body) and +z out
        // of her back.
        pose.pushPose();
        pose.translate(0, 12 * PX, 0);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.translate(-0.5f, 0, 0);
        var model = mc.getModelManager().getModel(BODY);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), null, model,
                1, 1, 1, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, Sheets.cutoutBlockSheet());
        pose.popPose();

        // The VU bars, in the body part's pixel space (y points down here), inside the meter window painted on the
        // front (model y 1.8..5.0, x 3.75..12.25; the front face is at body z 8.25): six bars across, rising from
        // the window's floor.
        boolean playing = MidnightMachineClient.playing(id);
        int track = MidnightMachineClient.track(id);
        long ms = MidnightMachineClient.positionMs(id, partial);
        VertexConsumer bars = buffers.getBuffer(Sheets.cutoutBlockSheet());
        TextureAtlasSprite white = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WHITE);
        var last = pose.last();
        for (int i = 0; i < 6; i++) {
            float level = playing ? Album.level(track, i, ms) : 0.12f + 0.08f * Mth.sin(age * 0.15f + i);
            int lit = Math.max(1, Math.round(level * SEGMENTS));
            float x0 = (-3.9f + i * 1.39f) * PX, x1 = x0 + 0.85f * PX;
            for (int s = 0; s < lit; s++) {
                float yBottom = (9.95f - s * 0.36f) * PX, yTop = yBottom - 0.28f * PX;
                int argb = 0xFF000000 | (s == lit - 1 ? brighten(BAR_COLOURS[i]) : BAR_COLOURS[i]);   // a peak light on top
                box(bars, last, white, x0, yTop, 8.3f * PX, x1, yBottom, 8.45f * PX, argb);
            }
        }
        pose.popPose();
    }

    private static int brighten(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 0xFF) + 60), g = Math.min(255, ((rgb >> 8) & 0xFF) + 60), b = Math.min(255, (rgb & 0xFF) + 60);
        return r << 16 | g << 8 | b;
    }

    private static void box(VertexConsumer v, PoseStack.Pose p, TextureAtlasSprite t, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        // the face toward the viewer behind her (+z), plus the sides and top so the bars read at an angle
        quad(v, p, t, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, argb);
        quad(v, p, t, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, argb);
        quad(v, p, t, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, argb);
        quad(v, p, t, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, argb);
    }

    private static void quad(VertexConsumer v, PoseStack.Pose p, TextureAtlasSprite t, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz, int argb) {
        float u0 = t.getU0(), u1 = t.getU1(), v0 = t.getV0(), v1 = t.getV1();
        // both windings, so neither face is culled whichever way the pose turns it
        vertex(v, p, ax, ay, az, u0, v0, nx, ny, nz, argb);
        vertex(v, p, bx, by, bz, u1, v0, nx, ny, nz, argb);
        vertex(v, p, cx, cy, cz, u1, v1, nx, ny, nz, argb);
        vertex(v, p, dx, dy, dz, u0, v1, nx, ny, nz, argb);
        vertex(v, p, dx, dy, dz, u0, v1, nx, ny, nz, argb);
        vertex(v, p, cx, cy, cz, u1, v1, nx, ny, nz, argb);
        vertex(v, p, bx, by, bz, u1, v0, nx, ny, nz, argb);
        vertex(v, p, ax, ay, az, u0, v0, nx, ny, nz, argb);
    }

    private static void vertex(VertexConsumer v, PoseStack.Pose p, float x, float y, float z, float u, float vv, float nx, float ny, float nz, int argb) {
        v.addVertex(p.pose(), x, y, z).setColor(argb).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(p, nx, ny, nz);
    }
}
