package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.block.LampBlock;
import com.malevolentgods.fourtwenty.block.LampTrimBlock;
import com.malevolentgods.fourtwenty.domain.LightFalloff;
import com.malevolentgods.fourtwenty.util.ClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The colour a lit lamp casts. Minecraft's light has no colour, so a lamp lays a film of its colour over the surfaces it can reach, added the way its halo is. The
 * film spreads out through open space from the lamp, so walls stop it as they would light. It's worked out once and kept, since a lamp doesn't move, and only
 * looked at again now and then in case the blocks around it changed.
 */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LampFilm {
    private LampFilm() {}

    /** Four corners of each face: x, y, z from the lamp, and alpha. */
    private record Film(int rgb, float[] corners) {}

    private static final int REACH = 7, MOST_FACES = 500, MOST_LAMPS = 32, WITHIN = 48, NEW_A_TICK = 4, REFRESH_EVERY = 4, NEAREST_EVERY = 5;
    private static final float STRENGTH = 0.24f;
    private static final double SKIN = 0.004;
    /** How far each film quad overlaps its neighbours. Film adds up, so anything above 0 (try 0.004) lights a glowing grid along the block edges: a good look for something else. */
    private static final double SEAM = 0;
    private static final Direction[] SIDES = Direction.values();
    /** A block's faces from outside, by Direction ordinal: as LampHaloRenderer.cube. */
    private static final int[][] CORNERS = {
            {0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1}, {0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0}, {0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0},
            {0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1}, {0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0}, {1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1}};

    private static final Map<BlockPos, Film> FILMS = new HashMap<>();
    private static final Map<BlockPos, Integer> COLOURS = new HashMap<>();
    private static final ArrayDeque<BlockPos> NEW = new ArrayDeque<>(), REFRESH = new ArrayDeque<>();
    private static List<BlockPos> nearest = new ArrayList<>();

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(LampFilm::tick);
        NeoForge.EVENT_BUS.addListener(LampFilm::render);
    }

    /** A lit lamp was drawn: if it's near enough, it casts its colour from now on. */
    public static void placed(BlockPos pos, int rgb) {
        if (!ClientConfig.COLOURED_LIGHT.get() || COLOURS.containsKey(pos)) return;
        if (!pos.getCenter().closerThan(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), WITHIN)) return;
        BlockPos at = pos.immutable();
        COLOURS.put(at, rgb);
        NEW.add(at);
    }

    private static boolean lit(ClientLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof LampBlock ? state.getValue(LampBlock.LIT) : state.getBlock() instanceof LampTrimBlock;
    }

    private static void forget(BlockPos pos) {
        COLOURS.remove(pos);
        FILMS.remove(pos);
    }

    private static void clear() {
        FILMS.clear();
        COLOURS.clear();
        NEW.clear();
        REFRESH.clear();
        nearest = new ArrayList<>();
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !ClientConfig.COLOURED_LIGHT.get()) {
            clear();
            return;
        }
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        for (int i = 0; i < NEW_A_TICK && !NEW.isEmpty(); i++) bake(mc.level, NEW.poll(), camera);
        long now = mc.level.getGameTime();
        if (now % REFRESH_EVERY == 0 && !REFRESH.isEmpty()) bake(mc.level, REFRESH.poll(), camera);
        if (now % NEAREST_EVERY == 0) {
            nearest = new ArrayList<>(FILMS.keySet().stream().sorted(Comparator.comparingDouble(p -> p.distToCenterSqr(camera))).limit(MOST_LAMPS).toList());
        }
    }

    private static void bake(ClientLevel level, BlockPos pos, Vec3 camera) {
        Integer rgb = COLOURS.get(pos);
        if (rgb == null) return;
        if (!lit(level, pos) || !pos.getCenter().closerThan(camera, WITHIN)) {
            forget(pos);
            return;
        }
        FILMS.put(pos, new Film(rgb, corners(level, pos)));
        REFRESH.add(pos);
    }

    /** Spreads out through open space from the lamp; every solid face it meets is lit. */
    private static float[] corners(ClientLevel level, BlockPos lamp) {
        Vec3 at = lamp.getCenter();
        List<float[]> faces = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(Set.of(lamp));
        var open = new ArrayDeque<BlockPos>(List.of(lamp));
        while (!open.isEmpty() && faces.size() < MOST_FACES) {
            BlockPos cell = open.poll();
            for (Direction side : SIDES) {
                BlockPos next = cell.relative(side);
                if (!next.closerThan(lamp, REACH)) continue;
                boolean solid = level.getBlockState(next).isSolidRender(level, next);
                if (solid) faces.add(face(next, side.getOpposite(), lamp, at));
                else if (seen.add(next)) open.add(next);
            }
        }
        float[] all = new float[faces.size() * 16];
        for (int i = 0; i < faces.size(); i++) System.arraycopy(faces.get(i), 0, all, i * 16, 16);
        return all;
    }

    private static float[] face(BlockPos pos, Direction side, BlockPos lamp, Vec3 at) {
        int[] corners = CORNERS[side.ordinal()];
        float[] out = new float[16];
        for (int i = 0, c = 0; i < corners.length; i += 3, c += 4) {
            double x = pos.getX() + corners[i] + side.getStepX() * SKIN + (corners[i] == 0 ? -SEAM : SEAM);
            double y = pos.getY() + corners[i + 1] + side.getStepY() * SKIN + (corners[i + 1] == 0 ? -SEAM : SEAM);
            double z = pos.getZ() + corners[i + 2] + side.getStepZ() * SKIN + (corners[i + 2] == 0 ? -SEAM : SEAM);
            out[c] = (float) (x - lamp.getX());
            out[c + 1] = (float) (y - lamp.getY());
            out[c + 2] = (float) (z - lamp.getZ());
            out[c + 3] = STRENGTH * LightFalloff.film(Math.sqrt(at.distanceToSqr(x, y, z)), REACH);
        }
        return out;
    }

    private static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || nearest.isEmpty()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        // Checked every frame, so a lamp that's broken or switched off loses its colour at once.
        for (BlockPos pos : nearest) if (!lit(level, pos)) forget(pos);
        nearest.removeIf(pos -> !FILMS.containsKey(pos));
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        VertexConsumer out = buffers.getBuffer(LampHaloRenderer.HaloRenderType.HALO);
        for (BlockPos pos : nearest) {
            Film film = FILMS.get(pos);
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            var matrix = pose.last().pose();
            int r = film.rgb >> 16 & 0xFF, g = film.rgb >> 8 & 0xFF, b = film.rgb & 0xFF;
            float[] c = film.corners;
            for (int i = 0; i < c.length; i += 4) out.addVertex(matrix, c[i], c[i + 1], c[i + 2]).setColor(r, g, b, (int) (255 * c[i + 3]));
            pose.popPose();
        }
        buffers.endBatch(LampHaloRenderer.HaloRenderType.HALO);
    }
}
