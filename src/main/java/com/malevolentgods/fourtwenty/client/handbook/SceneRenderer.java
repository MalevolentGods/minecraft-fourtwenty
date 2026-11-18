package com.malevolentgods.fourtwenty.client.handbook;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders the Handbook's scenes with the game's own renderer (specs/016-weedcraft-handbook, research R8; the user, 2026-11-18:
 * "real in-game renders"). A client command for the book's authors: {@code /handbook_render} reads
 * {@code config/fourtwenty-handbook-scenes.json} (name, from, to for each scene), draws each box of the loaded world from an
 * isometric camera into a large off-screen picture, and saves it to {@code handbook-renders/<name>.png} in the game folder.
 * tools/generate_handbook_scenes.py crops and scales those for the book and the PDF.
 */
public final class SceneRenderer {
    private SceneRenderer() {}

    private static final int SIZE = 2400;

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("handbook_render").executes(ctx -> {
            renderAll(ctx.getSource());
            return 1;
        }));
    }

    private static void renderAll(CommandSourceStack source) {
        Path config = FMLPaths.CONFIGDIR.get().resolve("fourtwenty-handbook-scenes.json");
        Path out = FMLPaths.GAMEDIR.get().resolve("handbook-renders");
        try {
            Files.createDirectories(out);
            JsonObject root = JsonParser.parseString(Files.readString(config, StandardCharsets.UTF_8)).getAsJsonObject();
            int n = 0;
            for (JsonElement e : root.getAsJsonArray("scenes")) {
                JsonObject s = e.getAsJsonObject();
                var a = s.getAsJsonArray("from");
                var b = s.getAsJsonArray("to");
                BlockPos from = new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
                BlockPos to = new BlockPos(b.get(0).getAsInt(), b.get(1).getAsInt(), b.get(2).getAsInt());
                float yaw = s.has("yaw") ? s.get("yaw").getAsFloat() : 45;     // 45: from the south-east, so south-facing fronts show
                render(from, to, yaw, out.resolve(s.get("name").getAsString() + ".png"));
                n++;
            }
            source.sendSystemMessage(Component.literal("Rendered " + n + " scenes to " + out));
        } catch (Exception ex) {
            source.sendSystemMessage(Component.literal("Scene render failed: " + ex));
        }
    }

    private static void render(BlockPos from, BlockPos to, float yaw, Path file) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) throw new IllegalStateException("no world");
        int x0 = Math.min(from.getX(), to.getX()), y0 = Math.min(from.getY(), to.getY()), z0 = Math.min(from.getZ(), to.getZ());
        int x1 = Math.max(from.getX(), to.getX()), y1 = Math.max(from.getY(), to.getY()), z1 = Math.max(from.getZ(), to.getZ());
        float cx = (x0 + x1 + 1) / 2f, cy = (y0 + y1 + 1) / 2f, cz = (z0 + z1 + 1) / 2f;
        float span = (x1 - x0 + 1) + (z1 - z0 + 1);
        float k = SIZE / (span * 0.78f);          // pixels per block, with room to spare (cropped afterwards)

        TextureTarget target = new TextureTarget(SIZE, SIZE, true, Minecraft.ON_OSX);
        target.setClearColor(0, 0, 0, 0);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, SIZE, SIZE, 0, -100000, 100000), VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        RenderSystem.applyModelViewMatrix();
        FogRenderer.setupNoFog();
        RenderSystem.enableDepthTest();
        Lighting.setupForEntityInInventory();

        PoseStack pose = new PoseStack();
        pose.translate(SIZE / 2f, SIZE / 2f, 0);
        pose.scale(k, -k, k);
        pose.mulPose(Axis.XP.rotationDegrees(30));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-cx, -cy, -cz);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        var blocks = mc.getBlockRenderer();
        var bes = mc.getBlockEntityRenderDispatcher();
        int light = LightTexture.FULL_BRIGHT;
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    FluidState fluid = state.getFluidState();
                    if (!fluid.isEmpty()) {
                        // the liquid renderer writes section-relative positions: place them through the pose ourselves
                        Matrix4f m = new Matrix4f(pose.last().pose()).translate(x & ~15, y & ~15, z & ~15);
                        VertexConsumer inner = buffers.getBuffer(ItemBlockRenderTypes.getRenderLayer(fluid));
                        blocks.renderLiquid(pos, level, new Transformed(inner, m), state, fluid);
                    }
                    if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) continue;
                    pose.pushPose();
                    pose.translate(x, y, z);
                    if (state.getRenderShape() == RenderShape.MODEL) {
                        ModelData data = level.getModelData(pos);
                        blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY, data == null ? ModelData.EMPTY : data, null);
                    }
                    var be = level.getBlockEntity(pos);
                    if (be != null) {
                        var r = bes.getRenderer(be);
                        if (r != null) r.render(be, 0, pose, buffers, light, OverlayTexture.NO_OVERLAY);
                    }
                    pose.popPose();
                }
            }
        }
        buffers.endBatch();

        NativeImage img = new NativeImage(SIZE, SIZE, false);
        RenderSystem.bindTexture(target.getColorTextureId());
        img.downloadTexture(0, false);
        img.flipY();
        img.writeToFile(file);
        img.close();

        mv.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        target.destroyBuffers();
        mc.getMainRenderTarget().bindWrite(true);
    }

    /** Passes vertices on with their positions moved by a matrix (for the liquid renderer, which ignores the pose). */
    private record Transformed(VertexConsumer inner, Matrix4f m) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            Vector4f v = m.transform(new Vector4f(x, y, z, 1));
            inner.addVertex(v.x, v.y, v.z);
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { inner.setColor(r, g, b, a); return this; }
        @Override public VertexConsumer setUv(float u, float v) { inner.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { inner.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { inner.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { inner.setNormal(x, y, z); return this; }
    }
}
