package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * What a player sits on: an invisible, unsaved entity they ride. On a couch it sits at the cushion
 * and goes when they stand up or the couch is broken; standing up puts them in front of the couch (or
 * on it, if the front is blocked). From /sit it sits where they stand (the ground, a step, a slab).
 */
public class SeatEntity extends Entity {
    /** A rider's hips sit this far above the seat entity (the player's vehicle attachment, 0.6, less the hip height, 0.75). */
    private static final double HIPS = 0.15;
    private boolean couch;

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Sit the player on the couch at pos, if no one is sitting there. */
    public static boolean sit(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (!CouchBlock.isCouch(state) || player.isPassenger() || occupied(level, pos)) return false;
        Direction front = state.getValue(CouchBlock.FACING).getOpposite();
        return seat(level, player, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5 - HIPS, pos.getZ() + 0.5), front.toYRot(), true);
    }

    /** /sit: on a couch, sit on it; anywhere else on solid ground, sit where the player stands. */
    public static boolean sitHere(Player player) {
        Level level = player.level();
        BlockPos at = player.blockPosition();
        if (CouchBlock.isCouch(level.getBlockState(at))) return sit(level, at, player);
        if (CouchBlock.isCouch(level.getBlockState(at.below())) && player.getY() - at.getY() < 0.01) return sit(level, at.below(), player);
        if (player.isPassenger() || !player.onGround() || player.isInWater() || occupied(level, at)) return false;
        return seat(level, player, new Vec3(player.getX(), player.getY() - HIPS, player.getZ()), player.getYRot(), false);
    }

    private static boolean seat(Level level, Player player, Vec3 at, float yRot, boolean couch) {
        SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), level);
        seat.couch = couch;
        seat.moveTo(at.x, at.y, at.z, yRot, 0);
        level.addFreshEntity(seat);
        if (player.startRiding(seat)) return true;
        seat.discard();
        return false;
    }

    /**
     * Whether someone already sits at pos: any seat with a rider there, ours or another mod's (a
     * MineColonies citizen's sitting entity sits most of a block lower than its seat).
     */
    public static boolean occupied(Level level, BlockPos pos) {
        return !level.getEntities((Entity) null, new AABB(pos).expandTowards(0, -1, 0),
                e -> !e.getPassengers().isEmpty() && !(e instanceof LivingEntity)).isEmpty();
    }

    /** Whether a player sits at pos on one of our seats (for MineColonies' own seat check). */
    public static boolean ours(Level level, BlockPos pos) {
        return !level.getEntitiesOfClass(SeatEntity.class, new AABB(pos), e -> !e.getPassengers().isEmpty()).isEmpty();
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        boolean gone = getPassengers().isEmpty()
                || (couch ? !CouchBlock.isCouch(level().getBlockState(blockPosition()))
                          : level().getBlockState(BlockPos.containing(getX(), getY() + HIPS - 0.01, getZ())).getCollisionShape(level(),
                                BlockPos.containing(getX(), getY() + HIPS - 0.01, getZ())).isEmpty());
        if (gone) {
            ejectPassengers();
            discard();
        }
    }

    @Override public @Nonnull Vec3 getDismountLocationForPassenger(@Nonnull LivingEntity passenger) {
        if (!couch) return new Vec3(getX(), getY() + HIPS, getZ());
        BlockPos pos = blockPosition();
        BlockState state = level().getBlockState(pos);
        if (CouchBlock.isCouch(state)) {
            BlockPos front = pos.relative(state.getValue(CouchBlock.FACING).getOpposite());
            if (level().getBlockState(front).getCollisionShape(level(), front).isEmpty()
                    && level().getBlockState(front.above()).getCollisionShape(level(), front.above()).isEmpty()) {
                return Vec3.atBottomCenterOf(front);
            }
        }
        return Vec3.atBottomCenterOf(pos.above());
    }

    @Override protected boolean canAddPassenger(@Nonnull Entity passenger) { return getPassengers().isEmpty(); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void defineSynchedData(@Nonnull SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(@Nonnull CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(@Nonnull CompoundTag tag) {}
}
