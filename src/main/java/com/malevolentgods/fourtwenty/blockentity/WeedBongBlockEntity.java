package com.malevolentgods.fourtwenty.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.block.WeedBongBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class WeedBongBlockEntity extends BlockEntity {
    private static final int EFFECT_RADIUS = 4; // 4 block radius for area effects
    private static final int BURN_DURATION = 300; // 15 seconds (300 ticks)
    private static final int USE_COOLDOWN = 100; // 5 seconds between uses
    
    private int burnTime = 0;
    private int lastUseTick = 0;
    
    public WeedBongBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.WEED_BONG.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WeedBongBlockEntity entity) {
        if (level.isClientSide()) {
            entity.clientTick(level, pos, state);
        } else {
            entity.serverTick(level, pos, state);
        }
    }

    private void clientTick(Level level, BlockPos pos, BlockState state) {
        // Client-side particle effects when lit
        if (state.getValue(WeedBongBlock.LIT) && level.random.nextFloat() < 0.3f) {
            // Add smoke particles
            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
            
            level.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                x, y, z, 0.0, 0.1, 0.0);
        }
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(WeedBongBlock.LIT)) {
            burnTime++;
            
            // Apply area effects every 20 ticks (1 second)
            if (burnTime % 20 == 0) {
                applyAreaEffects(level, pos);
            }
            
            // Consume weed and extinguish after burn duration
            if (burnTime >= BURN_DURATION) {
                consumeWeed(level, pos, state);
                burnTime = 0;
            }
        }
    }

    private void applyAreaEffects(Level level, BlockPos pos) {
        AABB effectArea = new AABB(pos).inflate(EFFECT_RADIUS);
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, effectArea);
        
        for (Player player : nearbyPlayers) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Apply enhanced effects for group session
                applyBongEffects(serverPlayer);
            }
        }
    }

    private void applyBongEffects(ServerPlayer player) {
        // Enhanced effects compared to individual consumption
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1)); // 20s, level 2
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0)); // 1 minute
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 0)); // 30s
        
        // Bonus effects for group sessions
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0)); // 15s
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0)); // 15s
        
        // Slight hunger effect (munchies)
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0)); // 10s
    }

    public void lightBong(ServerPlayer player) {
        burnTime = 0;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // Play lighting sound
            level.playSound(null, worldPosition, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.2f);
        }
    }

    public void useBong(ServerPlayer player) {
        if (level == null) return;
        
        long currentTick = level.getGameTime();
        
        // Check cooldown
        if (currentTick - lastUseTick < USE_COOLDOWN) {
            return;
        }
        
        lastUseTick = (int) currentTick;
        
        // Apply immediate strong effects to the user
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2)); // 30s, level 3
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0)); // 1.5 minutes
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 900, 1)); // 45s, level 2
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 2)); // 5s food restoration
        
        // Play use sound
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.8f, 0.8f);
        }
        
        setChanged();
    }

    private void consumeWeed(Level level, BlockPos pos, BlockState state) {
        int currentLevel = state.getValue(WeedBongBlock.WEED_LEVEL);
        if (currentLevel > 0) {
            level.setBlock(pos, state
                .setValue(WeedBongBlock.WEED_LEVEL, currentLevel - 1)
                .setValue(WeedBongBlock.LIT, false), 3);
        }
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("LastUseTick", lastUseTick);
    }

    @Override
    protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt("BurnTime");
        lastUseTick = tag.getInt("LastUseTick");
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    @Nonnull
    public CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
