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
 * What a player sits on when they sit on a couch: an invisible, unsaved entity at the cushion that
 * they ride. It goes when they stand up or the couch is broken. Standing up puts them in front of
 * the couch (or on it, if the front is blocked).
 */
public class SeatEntity extends Entity {
    /** Above the couch block's base: a rider's hips land on the cushion top (y 0.5). */
    private static final double HEIGHT = 0.35;

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Sit the player on the couch at pos, if no one is sitting there. */
    public static boolean sit(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (!CouchBlock.isCouch(state) || player.isPassenger() || occupied(level, pos)) return false;
        SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), level);
        Direction front = state.getValue(CouchBlock.FACING).getOpposite();
        seat.moveTo(pos.getX() + 0.5, pos.getY() + HEIGHT, pos.getZ() + 0.5, front.toYRot(), 0);
        level.addFreshEntity(seat);
        return player.startRiding(seat);
    }

    public static boolean occupied(Level level, BlockPos pos) {
        return !level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty();
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty() || !CouchBlock.isCouch(level().getBlockState(blockPosition())))) {
            ejectPassengers();
            discard();
        }
    }

    @Override public @Nonnull Vec3 getDismountLocationForPassenger(@Nonnull LivingEntity passenger) {
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
