package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.domain.BongVariety;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.util.BongBuffs;
import com.malevolentgods.fourtwenty.util.SmokeEffects;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * A glass bong: a placed, functional decoration. Right-click with a Weed Bud anywhere in your
 * inventory to take a hit: it uses one bud and gives the pipe's effects, with bubbling and smoke.
 * Specialty varieties (see {@link BongVariety}) add a one-day buff. (The larger burning "hookah"
 * is {@link WeedBongBlock}.)
 */
public class GlassBongBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<GlassBongBlock> CODEC = simpleCodec(properties -> new GlassBongBlock(BongVariety.GLASS, properties));
    /** Between hits, per player. */
    public static final int COOLDOWN_TICKS = 100;
    private static final VoxelShape SHAPE = Block.box(4.5, 0, 4.5, 11.5, 15.5, 11.5);

    private final BongVariety variety;

    public GlassBongBlock(BongVariety variety, Properties properties) {
        super(properties);
        this.variety = variety;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public BongVariety variety() { return variety; }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The front (bowl side) faces whoever placed it. */
    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override protected BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override protected VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Override protected InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                         @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        var item = ModItems.BONGS.get(variety).get();
        if (player.getCooldowns().isOnCooldown(item)) return InteractionResult.CONSUME;
        if (!player.getAbilities().instabuild && !takeBud(player)) {
            player.displayClientMessage(Component.translatableWithFallback("block.fourtwenty.glass_bong.needs_bud",
                    "You need a Weed Bud to pack the bong."), true);
            return InteractionResult.CONSUME;
        }
        // Light, bubble, exhale.
        server.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
        server.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.BLOCKS, 1.0f, 1.0f);
        server.sendParticles(ParticleTypes.BUBBLE, pos.getX() + 0.5, pos.getY() + 0.12, pos.getZ() + 0.5, 8, 0.12, 0.05, 0.12, 0.02);
        server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.05, 0.1, 0.05, 0.01);
        SmokeEffects.apply(server, player);
        if (variety.specialty()) {
            BongBuffs.apply(server, player, variety);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 10, 0.4, 0.5, 0.4, 0);
        }
        player.getCooldowns().addCooldown(item, COOLDOWN_TICKS);
        return InteractionResult.CONSUME;
    }

    /** Uses one Weed Bud from anywhere in the player's inventory. */
    private static boolean takeBud(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(ModItems.WEED_BUD.get())) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }
}
