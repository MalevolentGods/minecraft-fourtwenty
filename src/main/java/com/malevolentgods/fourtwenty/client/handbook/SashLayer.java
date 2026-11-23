package com.malevolentgods.fourtwenty.client.handbook;

import com.malevolentgods.fourtwenty.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Draws the Stoner Badge Sash on a player who wears it (specs/016-weedcraft-handbook FR-036, FR-038): a green cloth sash from one shoulder
 * to the opposite hip, front and back, with the wearer's most recent badges as patches along its front. It's worn in the chest slot, or
 * in Curios' body slot when Curios is installed (found by reflection, so Curios is never required). The badges are the wearer's own, as the
 * server last told us (HandbookNetwork.SashPayload), so a lent sash shows its wearer's.
 */
public final class SashLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation CLOTH = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/entity/stoner_sash.png");
    private static final int MAX_PATCHES = 8;
    /** How far in front of the body the sash sits, in pixels: clear of a chestplate. */
    private static final float FRONT = 2.9f, BACK = 2.9f;

    public SashLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(@Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, @Nonnull AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partial, float age, float headYaw, float headPitch) {
        if (player.isInvisible() || !wearing(player)) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        Matrix4f m = pose.last().pose();
        // the cloth: a slanted band, left shoulder to right hip (x from -4 to 4 across the body, y 0 to 12 down it)
        VertexConsumer cloth = buffers.getBuffer(RenderType.entityCutoutNoCull(CLOTH));
        band(cloth, pose, m, light, -FRONT, true);
        band(cloth, pose, m, light, BACK, false);
        // the patches, newest last, along the front of the sash
        List<String> badges = ProgressClient.sash(player.getId());
        int n = Math.min(MAX_PATCHES, badges.size());
        for (int i = 0; i < n; i++) {
            String id = badges.get(badges.size() - n + i);
            float t = (i + 0.5f) / MAX_PATCHES;                 // along the sash: shoulder to hip
            float cx = -3.0f + 6.0f * t, cy = 0.8f + 10.2f * t, half = 1.05f;
            VertexConsumer v = buffers.getBuffer(RenderType.entityCutoutNoCull(patchTexture(id)));
            quad(v, pose, m, light, cx - half, cy - half, cx + half, cy + half, -FRONT - 0.04f, true);
        }
        pose.popPose();
    }

    private static ResourceLocation patchTexture(String id) {
        return ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/handbook/patches/" + id + ".png");
    }

    /** The sash cloth as a parallelogram: 2.6 pixels wide across, from the top left to the bottom right. */
    private static void band(VertexConsumer v, PoseStack pose, Matrix4f m, int light, float z, boolean front) {
        float x0 = -4.1f, x1 = 4.1f, w = 2.6f;
        corner(v, pose, m, light, x0, 0, z, 0, 0, front);
        corner(v, pose, m, light, x0 + w, 0, z, 1, 0, front);
        corner(v, pose, m, light, x1, 11.8f, z, 1, 1, front);
        corner(v, pose, m, light, x1 - w, 11.8f, z, 0, 1, front);
    }

    private static void quad(VertexConsumer v, PoseStack pose, Matrix4f m, int light, float x0, float y0, float x1, float y1, float z, boolean front) {
        corner(v, pose, m, light, x0, y0, z, 0, 0, front);
        corner(v, pose, m, light, x1, y0, z, 1, 0, front);
        corner(v, pose, m, light, x1, y1, z, 1, 1, front);
        corner(v, pose, m, light, x0, y1, z, 0, 1, front);
    }

    private static void corner(VertexConsumer v, PoseStack pose, Matrix4f m, int light, float x, float y, float z, float u, float vv, boolean front) {
        float nz = front ? -1 : 1;
        v.addVertex(m, x / 16f, y / 16f, z / 16f).setColor(255, 255, 255, 255).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose.last(), 0, 0, nz);
    }

    // ---- is it worn?

    private static Item SASH;
    private static Method curiosInventory, equipped;
    private static boolean curiosTried;

    private static boolean wearing(LivingEntity e) {
        if (SASH == null) SASH = ModItems.STONER_BADGE_SASH.get();
        if (e.getItemBySlot(EquipmentSlot.CHEST).is(SASH)) return true;
        if (!ModList.get().isLoaded("curios")) return false;
        try {
            if (!curiosTried) {
                curiosTried = true;
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                curiosInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
                equipped = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler").getMethod("isEquipped", Item.class);
            }
            if (curiosInventory == null) return false;
            Object opt = curiosInventory.invoke(null, e);
            if (opt instanceof Optional<?> o && o.isPresent()) return (boolean) equipped.invoke(o.get(), SASH);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            curiosInventory = null;      // an API we don't recognise: the chest slot still works
        }
        return false;
    }
}
