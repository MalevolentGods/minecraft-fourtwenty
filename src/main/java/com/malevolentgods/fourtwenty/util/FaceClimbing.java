package com.malevolentgods.fourtwenty.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Blocks you climb from the outside, like a wall of ladders: they stay solid (nobody walks into them), and anyone
 * pressing into one climbs it. MineColonies racks are the first ({@code #fourtwenty:climbable_faces}).
 *
 * Players and mobs climb while they push into the face, or while they're off the ground and were climbing the
 * tick before (so letting go of forward mid-climb holds on, as on a ladder). Walking along a wall of them on the
 * ground without pushing in is ordinary walking.
 */
public final class FaceClimbing {
    private FaceClimbing() {}

    public static final TagKey<Block> FACES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("fourtwenty", "climbable_faces"));

    /** How far past the body a face still counts as touched. */
    private static final double REACH = 0.02;

    private static final Map<LivingEntity, Long> LAST_CLIMB = new WeakHashMap<>();

    public static boolean isFace(BlockState state) {
        return state.is(FACES);
    }

    /** Whether the entity is climbing a face this tick. */
    public static boolean climbing(LivingEntity entity) {
        if (entity.isSpectator() || entity.noPhysics) return false;
        long now = entity.level().getGameTime();
        Long last = LAST_CLIMB.get(entity);
        boolean holding = !entity.onGround() && last != null && last >= now - 1;
        if (!entity.horizontalCollision && !holding) return false;
        if (!touching(entity)) return false;
        LAST_CLIMB.put(entity, now);
        return true;
    }

    /** Whether a face block sits against the entity's side, anywhere along its body. */
    public static boolean touching(LivingEntity entity) {
        AABB body = entity.getBoundingBox();
        AABB reach = new AABB(body.minX - REACH, body.minY + 0.01, body.minZ - REACH, body.maxX + REACH, body.maxY - 0.01, body.maxZ + REACH);
        var level = entity.level();
        for (BlockPos pos : BlockPos.betweenClosed(
                (int) Math.floor(reach.minX), (int) Math.floor(reach.minY), (int) Math.floor(reach.minZ),
                (int) Math.floor(reach.maxX), (int) Math.floor(reach.maxY), (int) Math.floor(reach.maxZ))) {
            if (!isFace(level.getBlockState(pos))) continue;
            if (new AABB(pos).intersects(reach)) return true;
        }
        return false;
    }

    /** A block lookup by coordinates (MineColonies' path cache, or a level). */
    @FunctionalInterface
    public interface Lookup {
        BlockState at(int x, int y, int z);
    }

    /**
     * For route-finding: the direction from an open cell to a face it can climb, or null. The cell must be open
     * (nothing to collide with) with a face beside it, and either be off the ground or have a face beside the cell
     * above too, so a floor cell next to a single rack, or beside a rack wall you only walk along, isn't a climb.
     */
    @Nullable
    public static Direction pathFace(BlockState state, int x, int y, int z, Lookup lookup) {
        if (!open(state)) return null;
        boolean airborne = open(lookup.at(x, y - 1, z));
        Direction found = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (!isFace(lookup.at(x + d.getStepX(), y, z + d.getStepZ()))) continue;
            if (isFace(lookup.at(x + d.getStepX(), y + 1, z + d.getStepZ()))) return d;
            if (airborne && found == null) found = d;
        }
        return found;
    }

    private static boolean open(BlockState state) {
        return state.getFluidState().isEmpty() && state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
    }
}
