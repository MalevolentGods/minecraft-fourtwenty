package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.entity.BuzzBot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;

/** A Buzz Bot: a riveted little body with a stinger, and glass wings that blur. */
public class BuzzBotRenderer extends MobRenderer<BuzzBot, BuzzBotRenderer.Model> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("fourtwenty", "buzz_bot"), "main");
    private static final int SNORES = 3;
    private static final float SNORE_TICKS = 60;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/entity/buzz_bot.png");

    public BuzzBotRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(LAYER)), 0.15f);
    }

    @Override public @Nonnull ResourceLocation getTextureLocation(@Nonnull BuzzBot bot) { return TEXTURE; }

    @Override public void render(@Nonnull BuzzBot bot, float yaw, float partial, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light) {
        super.render(bot, yaw, partial, pose, buffers, light);
        if (bot.dormant()) snore(bot, partial, pose, buffers, light);
    }

    /** Zzzs rising off it, growing and fading as they go. */
    private void snore(BuzzBot bot, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Font font = getFont();
        for (int i = 0; i < SNORES; i++) {
            float rise = ((bot.tickCount + partial) / SNORE_TICKS + (float) i / SNORES) % 1;
            int alpha = (int) (255 * (1 - rise));
            if (alpha < 8) continue;
            pose.pushPose();
            pose.translate(0.15 + rise * 0.25, 0.5 + rise * 0.7, 0);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            float scale = 0.012f + rise * 0.016f;
            pose.scale(scale, -scale, scale);
            font.drawInBatch("Z", 0, 0, alpha << 24 | 0xFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
            pose.popPose();
        }
    }

    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -3.5F, 5, 5, 7)
                .texOffs(24, 0).addBox(-0.5F, -0.5F, 3.5F, 1, 1, 2, new CubeDeformation(0)), PartPose.offset(0, 20, 0));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 12).addBox(0, 0, -2, 6, 0, 5), PartPose.offset(2.5F, -2.5F, -1));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 18).addBox(-6, 0, -2, 6, 0, 5), PartPose.offset(-2.5F, -2.5F, -1));
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static class Model extends HierarchicalModel<BuzzBot> {
        private final ModelPart root, body, left, right;

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            left = body.getChild("left_wing");
            right = body.getChild("right_wing");
        }

        @Override public @Nonnull ModelPart root() { return root; }

        @Override public void setupAnim(@Nonnull BuzzBot bot, float limb, float limbAmount, float age, float yaw, float pitch) {
            float flap = bot.dormant() ? 0.9F : Mth.cos(age * 2.6F) * 0.5F + 0.2F;
            left.zRot = flap;
            right.zRot = -flap;
            body.y = bot.dormant() ? 21.5F : 20 + Mth.sin(age * 0.3F) * 0.4F;
        }

        @Override public void renderToBuffer(@Nonnull PoseStack pose, @Nonnull VertexConsumer buffer, int light, int overlay, int colour) {
            super.renderToBuffer(pose, buffer, light, overlay, colour);
        }
    }
}
