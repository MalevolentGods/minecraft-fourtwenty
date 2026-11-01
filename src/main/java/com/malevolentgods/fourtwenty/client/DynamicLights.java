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
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Light that moves: a player holding a torch or lamp, a dropped one, a Golden God. Nothing is placed in the world. The renderer is simply told the block light is
 * higher near a source (see mixin/LevelRendererLightMixin), and the chunks around it are rebuilt as it moves.
 *
 * <p>Minecraft's light has no colour, so a coloured source also lays a film of its colour over the surfaces it can reach, added the way a lamp's halo is. The film
 * spreads out through open space from the source, so walls stop it as they would light.
 */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DynamicLights {
    private DynamicLights() {}

    /** {@code rgb} is 0 for plain white light, which needs no film. */
    private record Source(int id, Vec3 at, int level, int rgb) {}

    private record Face(BlockPos pos, Direction side) {}

    private record Lit(BlockPos pos, int level) {}

    /** A placed lamp's film. It doesn't move, so it's only looked at again now and then. */
    private record Placed(int rgb, List<Face> faces) {}

    private static final int TICK_EVERY = 2, FILM_EVERY = 4, FILM_REACH = 9, MAX_FACES = 1200, GOD_RGB = 0xFFC830, ITEMS_WITHIN = 32;
    private static final float FILM_STRENGTH = 0.42f;
    /** Placed lamps come in rows, and film adds up, so each is softer and shorter than a moving light, and only the nearest few show. */
    private static final float PLACED_STRENGTH = 0.24f;
    private static final int PLACED_REACH = 7, PLACED_FACES = 500, PLACED_MOST = 32, PLACED_WITHIN = 48, PLACED_CHECKS = 4;
    private static final double HOLD_HEIGHT = 1.2, ITEM_HEIGHT = 0.3, SKIN = 0.004;
    /** How far each film quad overlaps its neighbours. Film adds up, so anything above 0 (try 0.004) lights a glowing grid along the block edges: a good look for something else. */
    private static final double SEAM = 0;
    private static final Direction[] SIDES = Direction.values();
    /** A block's faces from outside, by Direction ordinal: as LampHaloRenderer.cube. */
    private static final int[][] CORNERS = {
            {0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1}, {0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0}, {0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0},
            {0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1}, {0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0}, {1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1}};

    /** Read from chunk-building threads, so it's only ever swapped whole. */
    private static volatile List<Source> sources = List.of();
    private static final Map<Integer, Lit> LIT = new HashMap<>();
    private static final Map<Integer, List<Face>> FILMS = new HashMap<>();
    private static final Map<BlockPos, Placed> PLACED = new HashMap<>();
    private static final ArrayDeque<BlockPos> TO_CHECK = new ArrayDeque<>();

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(DynamicLights::tick);
        NeoForge.EVENT_BUS.addListener(DynamicLights::render);
    }

    /** A lit lamp was drawn: it casts its colour from now on. */
    public static void placed(BlockPos pos, int rgb) {
        if (!ClientConfig.COLOURED_LIGHT.get() || PLACED.containsKey(pos)) return;
        BlockPos at = pos.immutable();
        PLACED.put(at, new Placed(rgb, List.of()));
        TO_CHECK.addFirst(at);
    }

    /** A few lamps a tick: drops any that are gone or far away, and redoes the film of the rest. */
    private static void checkPlaced(ClientLevel level, Vec3 camera) {
        if (!ClientConfig.COLOURED_LIGHT.get()) {
            PLACED.clear();
            TO_CHECK.clear();
            return;
        }
        for (int i = 0; i < PLACED_CHECKS && !TO_CHECK.isEmpty(); i++) {
            BlockPos pos = TO_CHECK.poll();
            Placed lamp = PLACED.get(pos);
            if (lamp == null) continue;
            var state = level.getBlockState(pos);
            boolean lit = state.getBlock() instanceof LampBlock ? state.getValue(LampBlock.LIT) : state.getBlock() instanceof LampTrimBlock;
            if (!lit || !pos.getCenter().closerThan(camera, PLACED_WITHIN)) {
                PLACED.remove(pos);
                continue;
            }
            PLACED.put(pos, new Placed(lamp.rgb, spread(level, pos, PLACED_REACH, PLACED_FACES)));
            TO_CHECK.addLast(pos);
        }
    }

    /** The block light moving sources give at {@code pos}. */
    public static int at(BlockPos pos) {
        var now = sources;
        int best = 0;
        for (Source s : now) best = Math.max(best, LightFalloff.level(s.level, Math.sqrt(pos.distToCenterSqr(s.at))));
        return best;
    }

    private static int emission(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item ? item.getBlock().defaultBlockState().getLightEmission() : 0;
    }

    /** A lamp's own colour, in 0xRRGGBB; 0 for anything else. */
    private static int colour(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return 0;
        Block block = item.getBlock();
        if (block instanceof LampBlock lamp) return lamp.halo() >>> 8;
        return block instanceof LampTrimBlock trim ? trim.halo() >>> 8 : 0;
    }

    private static Source held(Entity entity, ItemStack main, ItemStack off) {
        ItemStack bright = emission(off) > emission(main) ? off : main;
        return new Source(entity.getId(), entity.position().add(0, HOLD_HEIGHT, 0), emission(bright), colour(bright));
    }

    private static List<Source> gather(ClientLevel level, Vec3 camera) {
        List<Source> found = new ArrayList<>();
        for (var player : level.players()) {
            Source held = held(player, player.getMainHandItem(), player.getOffhandItem());
            if (GoldenGodClient.shining(player.getId())) found.add(new Source(player.getId(), held.at, 15, GOD_RGB));
            else if (held.level > 0) found.add(held);
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item && entity.position().closerThan(camera, ITEMS_WITHIN) && emission(item.getItem()) > 0) {
                found.add(new Source(entity.getId(), entity.position().add(0, ITEM_HEIGHT, 0), emission(item.getItem()), colour(item.getItem())));
            }
        }
        return found;
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            sources = List.of();
            LIT.clear();
            FILMS.clear();
            PLACED.clear();
            TO_CHECK.clear();
            return;
        }
        if (mc.level.getGameTime() % TICK_EVERY != 0) return;
        List<Source> now = ClientConfig.DYNAMIC_LIGHTS.get() ? gather(mc.level, mc.gameRenderer.getMainCamera().getPosition()) : List.of();
        sources = now;
        Map<Integer, Lit> before = new HashMap<>(LIT);
        LIT.clear();
        for (Source s : now) {
            Lit lit = new Lit(BlockPos.containing(s.at), s.level);
            LIT.put(s.id, lit);
            Lit old = before.remove(s.id);
            if (lit.equals(old)) continue;
            if (old != null) rebuild(old);
            rebuild(lit);
        }
        before.values().forEach(DynamicLights::rebuild);
        if (mc.level.getGameTime() % FILM_EVERY == 0) films(mc.level, now);
        checkPlaced(mc.level, mc.gameRenderer.getMainCamera().getPosition());
    }

    /** Has every chunk section the light reaches built again. */
    private static void rebuild(Lit lit) {
        var renderer = Minecraft.getInstance().levelRenderer;
        BlockPos p = lit.pos;
        int r = lit.level;
        for (int x = SectionPos.blockToSectionCoord(p.getX() - r); x <= SectionPos.blockToSectionCoord(p.getX() + r); x++) {
            for (int y = SectionPos.blockToSectionCoord(p.getY() - r); y <= SectionPos.blockToSectionCoord(p.getY() + r); y++) {
                for (int z = SectionPos.blockToSectionCoord(p.getZ() - r); z <= SectionPos.blockToSectionCoord(p.getZ() + r); z++) {
                    renderer.setSectionDirty(x, y, z);
                }
            }
        }
    }

    private static void films(ClientLevel level, List<Source> now) {
        FILMS.clear();
        if (!ClientConfig.COLOURED_LIGHT.get()) return;
        for (Source s : now) if (s.rgb != 0) FILMS.put(s.id, spread(level, BlockPos.containing(s.at), FILM_REACH, MAX_FACES));
    }

    /** Spreads out through open space from the source; every solid face it meets is lit. */
    private static List<Face> spread(ClientLevel level, BlockPos start, int reach, int most) {
        List<Face> faces = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(Set.of(start));
        var open = new ArrayDeque<BlockPos>(List.of(start));
        while (!open.isEmpty() && faces.size() < most) {
            BlockPos cell = open.poll();
            for (Direction side : SIDES) {
                BlockPos next = cell.relative(side);
                if (!next.closerThan(start, reach) || !seen.add(next) && !level.getBlockState(next).isSolidRender(level, next)) continue;
                if (level.getBlockState(next).isSolidRender(level, next)) faces.add(new Face(next, side.getOpposite()));
                else open.add(next);
            }
        }
        return faces;
    }

    private static void film(VertexConsumer out, Matrix4f matrix, List<Face> faces, Vec3 at, int rgb, double reach, float strength) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        for (Face face : faces) {
            int[] corners = CORNERS[face.side.ordinal()];
            for (int i = 0; i < corners.length; i += 3) {
                double x = face.pos.getX() + corners[i] + face.side.getStepX() * SKIN + (corners[i] == 0 ? -SEAM : SEAM);
                double y = face.pos.getY() + corners[i + 1] + face.side.getStepY() * SKIN + (corners[i + 1] == 0 ? -SEAM : SEAM);
                double z = face.pos.getZ() + corners[i + 2] + face.side.getStepZ() * SKIN + (corners[i + 2] == 0 ? -SEAM : SEAM);
                int alpha = (int) (255 * strength * LightFalloff.film(Math.sqrt(at.distanceToSqr(x, y, z)), reach));
                out.addVertex(matrix, (float) x, (float) y, (float) z).setColor(r, g, b, alpha);
            }
        }
    }

    private static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || FILMS.isEmpty() && PLACED.isEmpty()) return;
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer out = buffers.getBuffer(LampHaloRenderer.HaloRenderType.HALO);
        var matrix = pose.last().pose();
        for (Source source : sources) {
            List<Face> faces = FILMS.get(source.id);
            if (faces == null) continue;
            film(out, matrix, faces, source.at, source.rgb, FILM_REACH, FILM_STRENGTH);
        }
        PLACED.entrySet().stream().filter(e -> !e.getValue().faces.isEmpty())
                .sorted(Comparator.comparingDouble(e -> e.getKey().distToCenterSqr(camera)))
                .limit(PLACED_MOST)
                .forEach(e -> film(out, matrix, e.getValue().faces, e.getKey().getCenter(), e.getValue().rgb, PLACED_REACH, PLACED_STRENGTH));
        pose.popPose();
        buffers.endBatch(LampHaloRenderer.HaloRenderType.HALO);
    }
}
