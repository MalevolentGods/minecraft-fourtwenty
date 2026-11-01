package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.util.GoldenGod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/** A shining golden god: their whole body glows gold, with a halo of gold sparks turning over their head and more falling off them. */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GoldenGodClient {
    private GoldenGodClient() {}

    private static final Set<Integer> SHINING = new HashSet<>();
    private static final DustParticleOptions SPARK = new DustParticleOptions(new Vector3f(1, 0.82f, 0.15f), 1);
    private static final int HALO = 10;
    private static final int[] GLOW = {0xFFC8305A, 0xFFB42838, 0xFF9C2020};
    private static final double GLOW_STEP = 0.1;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        GoldenGod.clientShine = shine -> {
            if (shine.on()) SHINING.add(shine.entity());
            else SHINING.remove(shine.entity());
        };
        NeoForge.EVENT_BUS.addListener(GoldenGodClient::tick);
        NeoForge.EVENT_BUS.addListener(GoldenGodClient::halo);
    }

    /** A lamp's glow, but gold and god-sized: the same additive film, in layers that soften outward. */
    private static void halo(RenderPlayerEvent.Post event) {
        var player = event.getEntity();
        if (!SHINING.contains(player.getId())) return;
        var size = new AABB(-player.getBbWidth() / 2, 0, -player.getBbWidth() / 2, player.getBbWidth() / 2, player.getBbHeight(), player.getBbWidth() / 2);
        for (int i = 0; i < GLOW.length; i++) {
            LampHaloRenderer.halo(event.getPoseStack(), event.getMultiBufferSource(), size.inflate(GLOW_STEP * (i + 1)), GLOW[i]);
        }
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new Shine(renderer));
        }
    }

    private static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            SHINING.clear();
            return;
        }
        if (SHINING.isEmpty()) return;
        for (var player : level.players()) {
            if (!SHINING.contains(player.getId())) continue;
            double turn = level.getGameTime() * 0.12, top = player.getBbHeight() + 0.25;
            for (int i = 0; i < HALO; i++) {
                double angle = turn + i * Mth.TWO_PI / HALO;
                level.addParticle(SPARK, player.getX() + Math.cos(angle) * 0.4, player.getY() + top, player.getZ() + Math.sin(angle) * 0.4, 0, 0, 0);
            }
            level.addParticle(SPARK, player.getRandomX(0.6), player.getRandomY(), player.getRandomZ(0.6), 0, -0.05, 0);
        }
    }

    private static final class Shine extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private Shine(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

        @Override public void render(@Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, @Nonnull AbstractClientPlayer player, float limb, float amount,
                                     float partial, float age, float yaw, float pitch) {
            if (!SHINING.contains(player.getId())) return;
            int alpha = 190 + (int) (40 * Mth.sin(age * 0.15f));
            VertexConsumer gold = buffers.getBuffer(RenderType.entityTranslucentEmissive(player.getSkin().texture()));
            getParentModel().renderToBuffer(pose, gold, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, alpha << 24 | 0xFFD230);
        }
    }
}
