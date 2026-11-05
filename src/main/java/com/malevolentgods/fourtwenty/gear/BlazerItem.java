package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.domain.GearRules.Piece;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The Blazer, the arm laser: Blaze Beam always, and Bore Toke, Timber Torch and Paint Puff once their terpenes are in. Right-click fires
 * the chosen mode; sneak and right-click at the air cycles modes. Every block it changes goes through the game's own break and place
 * checks, so claims and spawn protection hold, and drops come out as from mining by hand.
 */
public class BlazerItem extends Item implements Gear.GearPiece {
    public enum Mode {
        BEAM("beam_power"), BORE("bore_toke"), TIMBER("timber_torch"), PAINT("paint_puff");

        /** The ability that unlocks the mode; Blaze Beam's makes it hit harder instead. */
        public final String ability;

        Mode(String ability) { this.ability = ability; }

        public String key() { return "gear.fourtwenty.blazer.mode." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    private static final double RANGE = 32, MINE_RANGE = 8;

    public BlazerItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override public Piece piece() { return Piece.BLAZER; }

    public static Mode mode(ItemStack stack) { return Mode.values()[Math.floorMod(Gear.state(stack).mode(), Mode.values().length)]; }

    /** Whether the mode can be chosen: the beam always, the others with their terpene in. */
    public static boolean available(ItemStack stack, Mode mode) { return mode == Mode.BEAM || Gear.has(stack, mode.ability); }

    @Override public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        BlockHitResult block = (BlockHitResult) player.pick(MINE_RANGE, 0, false);
        boolean onBlock = block.getType() == HitResult.Type.BLOCK;
        if (player.isShiftKeyDown() && !onBlock) {
            cycle(sp, stack);
            return InteractionResultHolder.consume(stack);
        }
        Mode mode = mode(stack);
        if (!available(stack, mode)) mode = Mode.BEAM;
        boolean fired = switch (mode) {
            case BEAM -> beam(sp, stack);
            case BORE -> onBlock && bore(sp, stack, block);
            case TIMBER -> onBlock && timber(sp, stack, block.getBlockPos());
            case PAINT -> onBlock && paint(sp, stack, block, player.isShiftKeyDown());
        };
        if (!fired && !Gear.powered(player, stack, 1)) player.displayClientMessage(Component.translatable("message.fourtwenty.gear.unpowered"), true);
        player.getCooldowns().addCooldown(this, mode == Mode.BEAM ? 10 : 4);
        return InteractionResultHolder.consume(stack);
    }

    private static void cycle(ServerPlayer player, ItemStack stack) {
        Mode now = mode(stack);
        for (int i = 1; i <= Mode.values().length; i++) {
            Mode next = Mode.values()[(now.ordinal() + i) % Mode.values().length];
            if (available(stack, next)) {
                Gear.set(stack, Gear.state(stack).withMode(next.ordinal()));
                player.displayClientMessage(Component.translatable("message.fourtwenty.blazer.mode", Component.translatable(next.key())), true);
                return;
            }
        }
    }

    // ---- Blaze Beam

    public static boolean beam(ServerPlayer player, ItemStack stack) {
        boolean power = Gear.has(stack, Mode.BEAM.ability);
        int cost = GearConfig.beam() + (power ? Gear.cost(stack, Mode.BEAM.ability) : 0);
        if (!Gear.spend(player, stack, cost)) return false;
        ServerLevel level = player.serverLevel();
        Vec3 from = player.getEyePosition(), look = player.getViewVector(1), to = from.add(look.scale(RANGE));
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        var hit = ProjectileUtil.getEntityHitResult(level, player, from, end, new AABB(from, end).inflate(1),
                e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator(), 0.3f);
        if (hit != null) {
            end = hit.getLocation();
            Entity target = hit.getEntity();
            target.hurt(level.damageSources().playerAttack(player), GearConfig.beamDamage() * (power ? 1.5f : 1f));
            target.igniteForSeconds(2);
        }
        double length = end.distanceTo(from);
        for (double d = 1; d < length; d += 0.5) {
            Vec3 p = from.add(look.scale(d));
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7f, 1.4f);
        return true;
    }

    // ---- Bore Toke: a 3x3 face, as if mined by hand

    public static boolean bore(ServerPlayer player, ItemStack stack, BlockHitResult hit) {
        Direction face = hit.getDirection();
        BlockPos centre = hit.getBlockPos();
        int per = Gear.cost(stack, Mode.BORE.ability), mined = 0;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                BlockPos pos = switch (face.getAxis()) {
                    case X -> centre.offset(0, a, b);
                    case Y -> centre.offset(a, 0, b);
                    case Z -> centre.offset(a, b, 0);
                };
                BlockState state = player.level().getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(player.level(), pos) < 0 || state.hasBlockEntity()) continue;
                if (!Gear.powered(player, stack, per)) return mined > 0;
                if (player.gameMode.destroyBlock(pos)) {
                    Gear.spend(player, stack, per);
                    mined++;
                }
            }
        }
        return mined > 0;
    }

    // ---- Timber Torch: a whole tree

    public static boolean timber(ServerPlayer player, ItemStack stack, BlockPos start) {
        Level level = player.level();
        if (!level.getBlockState(start).is(BlockTags.LOGS)) return false;
        int per = Gear.cost(stack, Mode.TIMBER.ability), cap = GearConfig.timberLogs(), felled = 0;
        var seen = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        queue.add(start);
        seen.add(start);
        List<BlockPos> logs = new ArrayList<>();
        while (!queue.isEmpty() && logs.size() < cap) {
            BlockPos pos = queue.poll();
            logs.add(pos);
            for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos next = pos.offset(dx, dy, dz);
                if (seen.add(next) && level.getBlockState(next).is(BlockTags.LOGS)) queue.add(next);
            }
        }
        for (BlockPos pos : logs) {
            if (!Gear.powered(player, stack, per)) break;
            if (player.gameMode.destroyBlock(pos)) {
                Gear.spend(player, stack, per);
                felled++;
            }
        }
        if (felled > 0) level.playSound(null, start, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 0.8f);
        return felled > 0;
    }

    // ---- Paint Puff: a line along the look (or, sneaking, a wall up), from the block in the other hand

    public static boolean paint(ServerPlayer player, ItemStack stack, BlockHitResult hit, boolean wall) {
        ItemStack paint = player.getOffhandItem();
        if (!(paint.getItem() instanceof BlockItem blockItem)) {
            player.displayClientMessage(Component.translatable("message.fourtwenty.blazer.paint_needs"), true);
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        Direction along = wall ? Direction.UP : player.getDirection();
        int per = Gear.cost(stack, Mode.PAINT.ability), placed = 0;
        for (int i = 0; i < GearConfig.paintLength() && !paint.isEmpty(); i++, pos = pos.relative(along)) {
            if (!level.getBlockState(pos).canBeReplaced() || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, paint)) break;
            if (!Gear.powered(player, stack, per)) break;
            BlockState state = blockItem.getBlock().defaultBlockState();
            if (!state.canSurvive(level, pos)) break;
            var snapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos);
            level.setBlock(pos, state, 3);
            if (net.neoforged.neoforge.event.EventHooks.onBlockPlace(player, snapshot, Direction.UP)) {
                snapshot.restore();
                break;
            }
            Gear.spend(player, stack, per);
            if (!player.getAbilities().instabuild) paint.shrink(1);
            placed++;
        }
        if (placed > 0) level.playSound(null, hit.getBlockPos(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
        return placed > 0;
    }

    /** The Blazer mines as a tool would, so Bore Toke and Timber Torch drop what a pickaxe or axe would. */
    @Override public boolean isCorrectToolForDrops(@Nonnull ItemStack stack, @Nonnull BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Override public boolean isBarVisible(@Nonnull ItemStack stack) { return true; }
    @Override public int getBarWidth(@Nonnull ItemStack stack) { return KushArmorItem.GearTooltips.bar(stack); }
    @Override public int getBarColor(@Nonnull ItemStack stack) { return 0xFF8C28; }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.fourtwenty.blazer.mode", Component.translatable(mode(stack).key())).withStyle(ChatFormatting.GOLD));
        KushArmorItem.GearTooltips.add(stack, lines);
    }
}
