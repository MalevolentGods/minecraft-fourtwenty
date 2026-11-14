package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedPayload;
import com.minecolonies.api.entity.ModEntities;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.client.render.modeltype.CitizenModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lifted Builders on the client (specs/013-lifted-builders FR-013, research R10): a see-through green version of the
 * Golden God glow (a faint green over her body and soft glowing shells in its shape, slowly pulsing), and a little
 * smoke trail. Installed from ModClientEvents only when MineColonies is loaded.
 */
public final class LiftedClient {
    private LiftedClient() {}

    private static final Set<Integer> LIFTED = new HashSet<>();
    /** The glow shells, faintest outermost: ARGB. */
    private static final int[] GLOW = {0x40AAFF60, 0x28AAFF60, 0x149CFF50};
    private static final float GLOW_STEP = 0.1f;

    public static void install() {
        LiftedPayload.clientHandler = payload -> {
            if (payload.on()) LIFTED.add(payload.entity());
            else LIFTED.remove(payload.entity());
        };
        NeoForge.EVENT_BUS.addListener(LiftedClient::tick);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        if (ModEntities.CITIZEN == null) return;
        var renderer = event.getRenderer(ModEntities.CITIZEN);
        if (renderer instanceof LivingEntityRenderer living) {
            living.addLayer(new Glow(living));
            living.addLayer(new BoomboxLayer(living));   // the Midnight Machine (spec 014)
        }
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            LIFTED.clear();
            return;
        }
        if (mc.isPaused() || mc.level.getGameTime() % 6 != 0) return;
        for (int id : LIFTED) {
            var entity = mc.level.getEntity(id);
            if (entity == null) continue;
            mc.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 0.1, entity.getZ(),
                    (mc.level.random.nextDouble() - 0.5) * 0.01, 0.015, (mc.level.random.nextDouble() - 0.5) * 0.01);
        }
    }

    private static java.lang.reflect.Field childrenField;

    /** Hides a part's visible children (hat brim, feather) for one draw; returns them to show again. */
    @SuppressWarnings("unchecked")
    private static List<ModelPart> hideChildren(ModelPart part) {
        try {
            if (childrenField == null) {
                childrenField = ModelPart.class.getDeclaredField("children");
                childrenField.setAccessible(true);
            }
            var hidden = new java.util.ArrayList<ModelPart>();
            for (ModelPart child : ((java.util.Map<String, ModelPart>) childrenField.get(part)).values()) {
                if (child.visible) {
                    child.visible = false;
                    hidden.add(child);
                }
            }
            return hidden;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return List.of();
        }
    }

    private static final class Glow extends RenderLayer<AbstractEntityCitizen, CitizenModel<AbstractEntityCitizen>> {
        Glow(RenderLayerParent<AbstractEntityCitizen, CitizenModel<AbstractEntityCitizen>> parent) { super(parent); }

        @Override
        public void render(@Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, @Nonnull AbstractEntityCitizen citizen,
                           float limb, float amount, float partial, float age, float yaw, float pitch) {
            if (!LIFTED.contains(citizen.getId())) return;
            float pulse = 0.8f + 0.2f * Mth.sin(age * 0.1f);
            var model = getParentModel();
            // A faint green over her body: see-through, unlike the Golden God's solid gold.
            int tint = ((int) (70 * pulse) << 24) | 0x9CFF60;
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(citizen))),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint);
            // Bright additive shells, as the Golden God's. Parts hung off the body (a citizen's hat brim and feather) are
            // flat and mostly see-through, and an untextured shell turns them into solid green slabs (seen in game), so
            // they're hidden while the shells draw.
            var glow = buffers.getBuffer(com.malevolentgods.fourtwenty.client.LampHaloRenderer.HaloRenderType.HALO);
            for (ModelPart part : List.of(model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg)) {
                for (int i = 0; i < GLOW.length; i++) {
                    float size = 1 + GLOW_STEP * (i + 1);
                    int colour = ((int) ((GLOW[i] >>> 24) * pulse) << 24) | (GLOW[i] & 0xFFFFFF);
                    part.xScale *= size;
                    part.yScale *= size;
                    part.zScale *= size;
                    var hidden = hideChildren(part);
                    part.render(pose, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
                    hidden.forEach(child -> child.visible = true);
                    part.xScale /= size;
                    part.yScale /= size;
                    part.zScale /= size;
                }
            }
        }
    }
}
