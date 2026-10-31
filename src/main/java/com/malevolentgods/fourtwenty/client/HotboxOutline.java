package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.util.HotboxNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** A holding Hotbox's area in the world, on its chunk edges: shown as MineColonies shows a colony's, with the Build Tool in hand (or a Hotbox). */
@EventBusSubscriber(modid = "fourtwenty", value = Dist.CLIENT)
public final class HotboxOutline {
    private static final ResourceLocation BUILD_TOOL = ResourceLocation.fromNamespaceAndPath("structurize", "sceptergold");
    private static final float R = 0.35f, G = 1f, B = 0.2f, A = 0.9f;
    private static final int SPAN = 32, BAND = 8, CHUNK = 16;
    private static final double RANGE = 256;

    private HotboxOutline() {}

    private static boolean shows(ItemStack stack) {
        return stack.is(ModItems.HOTBOX.get()) || BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(BUILD_TOOL);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !(shows(player.getMainHandItem()) || shows(player.getOffhandItem()))) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        int y0 = Math.max(mc.level.getMinBuildHeight(), player.getBlockY() - SPAN);
        int y1 = Math.min(mc.level.getMaxBuildHeight(), player.getBlockY() + SPAN);
        for (HotboxNetwork.Box box : HotboxNetwork.boxes()) {
            if (!box.level().equals(mc.level.dimension())) continue;
            int[] a = box.area();
            int minX = a[0], minZ = a[1], maxX = a[2], maxZ = a[3];
            if (player.getX() < minX - RANGE || player.getX() > maxX + RANGE || player.getZ() < minZ - RANGE || player.getZ() > maxZ + RANGE) continue;
            for (int y = y0 - Math.floorMod(y0, BAND); y <= y1; y += BAND) {
                LevelRenderer.renderLineBox(pose, lines, minX, y, minZ, maxX, y, maxZ, R, G, B, A);
            }
            for (int x = minX; x <= maxX; x += CHUNK) {
                LevelRenderer.renderLineBox(pose, lines, x, y0, minZ, x, y1, minZ, R, G, B, A);
                LevelRenderer.renderLineBox(pose, lines, x, y0, maxZ, x, y1, maxZ, R, G, B, A);
            }
            for (int z = minZ; z <= maxZ; z += CHUNK) {
                LevelRenderer.renderLineBox(pose, lines, minX, y0, z, minX, y1, z, R, G, B, A);
                LevelRenderer.renderLineBox(pose, lines, maxX, y0, z, maxX, y1, z, R, G, B, A);
            }
        }
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }
}
