package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.block.BedGroups;
import com.malevolentgods.fourtwenty.block.BedShape;
import com.malevolentgods.fourtwenty.block.FancyBedBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * While the player holds a FourTwenty bed aimed where it would complete a king, shows the whole king
 * as a translucent ghost: white if it fits, red if something is in the way past the foot (the bed
 * would then go down as a single). Mirrors BedGroups' joining rules.
 */
@EventBusSubscriber(modid = "fourtwenty", value = Dist.CLIENT)
public final class BedPlacementPreview {
    private static final float ALPHA = 0.45f;
    private static final BedShape[] KING = {BedShape.KING_LEFT, BedShape.KING_MIDDLE, BedShape.KING_RIGHT};

    private BedPlacementPreview() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof BlockItem item && item.getBlock() instanceof FancyBedBlock bed)) {
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!(stack.getItem() instanceof BlockItem item && item.getBlock() instanceof FancyBedBlock)) return;
        }
        FancyBedBlock block = (FancyBedBlock) ((BlockItem) stack.getItem()).getBlock();
        // Where the bed would go, as BlockItem/BedBlock place it: the foot in the clicked block if that
        // can be replaced, otherwise against the clicked face; the head one block the way the player faces.
        BlockPos foot = level.getBlockState(hit.getBlockPos()).canBeReplaced() ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
        Direction facing = player.getDirection();
        if (!level.getBlockState(foot).canBeReplaced() || !level.getBlockState(foot.relative(facing)).canBeReplaced()) return;
        List<BlockPos> king = BedGroups.kingFor(level, foot, block, facing);
        if (king == null) return;
        boolean fits = BedGroups.extensionFree(level, king, facing);
        DyeColor blanket = BedGroups.kingBlanket(level, king, foot);

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer ghost = new Translucent(buffers.getBuffer(Sheets.translucentCullBlockSheet()));
        var renderer = mc.getBlockRenderer();
        float r = 1f, g = fits ? 1f : 0.3f, b = fits ? 1f : 0.3f;
        for (int i = 0; i < 3; i++) {
            BlockState state = block.defaultBlockState().setValue(FancyBedBlock.FACING, facing)
                    .setValue(FancyBedBlock.SHAPE, KING[i]).setValue(FancyBedBlock.BLANKET, blanket);
            BlockPos column = king.get(i);
            pose.pushPose();
            pose.translate(column.getX() - camera.x, column.getY() - camera.y, column.getZ() - camera.z);
            // A hair larger than the real bed, so the ghost over the existing columns doesn't flicker.
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(1.004f, 1.004f, 1.004f);
            pose.translate(-0.5, -0.5, -0.5);
            renderer.getModelRenderer().renderModel(pose.last(), ghost, state, renderer.getBlockModel(state), r, g, b,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffers.endBatch(Sheets.translucentCullBlockSheet());
    }

    /** Passes vertices through with their alpha scaled down. */
    private record Translucent(VertexConsumer inner) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { inner.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            inner.setColor(red, green, blue, Math.round(alpha * ALPHA)); return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { inner.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { inner.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { inner.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { inner.setNormal(x, y, z); return this; }
    }
}
