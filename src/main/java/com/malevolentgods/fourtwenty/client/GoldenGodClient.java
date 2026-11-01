package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.util.GoldenGod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A shining golden god: their whole body glows gold, with a soft glow in the shape of their body around it. */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GoldenGodClient {
    private GoldenGodClient() {}

    private static final Set<Integer> SHINING = new HashSet<>();
    /** A lamp's glow, shaped to the body: its colour for each layer, by how far out the layer sits. */
    private static final int[] GLOW = {0x58FFC830, 0x38FFB428, 0x20FF9C20};
    private static final float GLOW_STEP = 0.1f;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        GoldenGod.clientShine = shine -> {
            if (shine.on()) SHINING.add(shine.entity());
            else SHINING.remove(shine.entity());
        };
        NeoForge.EVENT_BUS.addListener(GoldenGodClient::tick);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new Shine(renderer));
        }
    }

    private static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) SHINING.clear();
    }

    private static final class Shine extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private Shine(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

        @Override public void render(@Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, @Nonnull AbstractClientPlayer player, float limb, float amount,
                                     float partial, float age, float yaw, float pitch) {
            if (!SHINING.contains(player.getId())) return;
            int alpha = 190 + (int) (40 * Mth.sin(age * 0.15f));
            VertexConsumer gold = buffers.getBuffer(RenderType.entityTranslucentEmissive(player.getSkin().texture()));
            getParentModel().renderToBuffer(pose, gold, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, alpha << 24 | 0xFFD230);
            VertexConsumer glow = buffers.getBuffer(LampHaloRenderer.HaloRenderType.HALO);
            var model = getParentModel();
            for (ModelPart part : List.of(model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg)) {
                for (int i = 0; i < GLOW.length; i++) {
                    float size = 1 + GLOW_STEP * (i + 1);
                    part.xScale *= size;
                    part.yScale *= size;
                    part.zScale *= size;
                    part.render(pose, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, GLOW[i]);
                    part.xScale /= size;
                    part.yScale /= size;
                    part.zScale /= size;
                }
            }
        }
    }
}
