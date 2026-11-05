package com.malevolentgods.fourtwenty.paving;

import com.malevolentgods.fourtwenty.block.HempPathSlabBlock;
import com.malevolentgods.fourtwenty.block.HempPathStairBlock;
import com.malevolentgods.fourtwenty.block.RoadSurface;
import com.malevolentgods.fourtwenty.domain.PaveRules;
import com.malevolentgods.fourtwenty.domain.PaveRules.Action;
import com.malevolentgods.fourtwenty.domain.PaveRules.Shape;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModDataComponents.PenState;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import com.malevolentgods.fourtwenty.util.StrainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a Pave Pen does in the world. Held on a block, it works on it like mining: when the work is done, the block becomes road (or a
 * better road, or a road of another shape), paid for from the pen's charge and any smooth stone or gold the job needs.
 */
public final class Paving {
    private Paving() {}

    /** Buds the pen burns, cheapest first, so cured buds are kept for smoking. */
    private static final List<Fuel> FUELS = List.of(new Fuel(ModItems.ROUGH_BUD::get, PaveRules.ROUGH),
            new Fuel(ModItems.TRIMMED_BUD::get, PaveRules.TRIMMED), new Fuel(ModItems.WEED_BUD::get, PaveRules.CURED));

    private record Fuel(java.util.function.Supplier<Item> item, int bi) {}

    private record Work(ResourceKeyPos at, BlockState state, int done) {}

    private record ResourceKeyPos(net.minecraft.resources.ResourceKey<Level> level, BlockPos pos) {}

    /** What the pen will do to one block: the job, the block it makes, its time and cost, and the materials it needs. */
    public record Plan(Action action, BlockState result, int ticks, int cost, boolean stone, boolean gold) {}

    private static final Map<UUID, Work> WORK = new ConcurrentHashMap<>();

    public static PaveRules.Numbers numbers() { return ProcessingConfig.pave(); }

    public static int roadTier(BlockState state) {
        if (state.getBlock() instanceof RoadSurface road) return road.tier();
        return state.getBlock() instanceof HempPathStairBlock stairs ? stairs.tier() : 0;
    }

    public static Shape roadShape(BlockState state) {
        if (state.getBlock() instanceof HempPathSlabBlock) return Shape.SLAB;
        return state.getBlock() instanceof HempPathStairBlock ? Shape.STAIRS : Shape.FULL;
    }

    public static Block road(int tier, Shape shape) {
        var list = switch (shape) {
            case FULL -> ModTerpenes.PATHS;
            case SLAB -> ModTerpenes.PATH_SLABS;
            case STAIRS -> ModTerpenes.PATH_STAIRS;
        };
        return list.get(Math.max(1, Math.min(PaveRules.TIERS, tier)) - 1).get();
    }

    /** What the pen would do at {@code pos}; null, with the reason in {@code why}, when it does nothing there. */
    public static @Nullable Plan plan(Level level, BlockPos pos, Player player, ItemStack pen, @Nullable java.util.function.Consumer<Component> why) {
        java.util.function.Consumer<Component> say = why == null ? c -> {} : why;
        PenState s = PavePenItem.state(pen);
        BlockState state = level.getBlockState(pos);
        int roadTier = roadTier(state);
        Shape roadShape = roadShape(state);
        Shape want = PaveRules.wanted(PavePenItem.isPro(pen), Shape.of(s.shape()), roadTier, roadShape);
        Action action = PaveRules.action(roadTier, roadShape, s.target(), want);
        PaveRules.Numbers n = numbers();
        double hardness = state.getDestroySpeed(level, pos);
        if (action == Action.NONE) {
            say.accept(Component.translatable("message.fourtwenty.pave_pen.done", state.getBlock().getName()));
            return null;
        }
        if (action == Action.LAY) {
            if (state.isAir() || state.hasBlockEntity() || !state.isCollisionShapeFullBlock(level, pos)) {
                say.accept(Component.translatable("message.fourtwenty.pave_pen.solid"));
                return null;
            }
            if (!PaveRules.layable(hardness, n)) {
                say.accept(Component.translatable("message.fourtwenty.pave_pen.hard", state.getBlock().getName()));
                return null;
            }
        }
        int tier = PaveRules.resultTier(roadTier, s.target()), from = PaveRules.startTier(roadTier);
        BlockState result = shaped(road(tier, want), roadTier > 0 && roadShape == want ? state : null, player, level, pos);
        return new Plan(action, result, PaveRules.ticks(action, hardness, n), PaveRules.cost(action, hardness, n),
                PaveRules.needsStone(from, tier), PaveRules.needsGold(from, tier));
    }

    private static BlockState shaped(Block road, @Nullable BlockState keep, Player player, Level level, BlockPos pos) {
        BlockState s = keep != null ? road.withPropertiesOf(keep) : road.defaultBlockState();
        if (keep == null && s.hasProperty(StairBlock.FACING)) s = s.setValue(StairBlock.FACING, player.getDirection());
        return Block.updateFromNeighbourShapes(s, level, pos);
    }

    /** BI the player could put in the pen from the buds they carry. */
    public static long fuelCarried(Player player) {
        long bi = 0;
        for (Fuel f : FUELS) bi += (long) player.getInventory().countItem(f.item().get()) * f.bi();
        return bi;
    }

    public static boolean free(Player player) { return player.getAbilities().instabuild || StrainEffects.isHigh(player); }

    /** Why the player can't pay for the plan now, or null when they can. Takes nothing. */
    public static @Nullable Component shortfall(Player player, ItemStack pen, Plan plan) {
        if (player.getAbilities().instabuild) return null;
        PenState s = PavePenItem.state(pen);
        if (plan.stone() && s.stone() <= 0 && player.getInventory().countItem(Items.SMOOTH_STONE) == 0)
            return Component.translatable("message.fourtwenty.pave_pen.needs", Items.SMOOTH_STONE.getDescription());
        if (plan.gold() && s.gold() <= 0 && player.getInventory().countItem(Items.GOLD_INGOT) == 0)
            return Component.translatable("message.fourtwenty.pave_pen.needs", Items.GOLD_INGOT.getDescription());
        if (!free(player) && s.charge() + fuelCarried(player) < plan.cost()) return Component.translatable("message.fourtwenty.pave_pen.empty");
        return null;
    }

    /** Takes the plan's cost: materials always, BI unless the player is high. Call only after {@link #shortfall} said yes. */
    static void pay(Player player, ItemStack pen, Plan plan) {
        if (player.getAbilities().instabuild) return;
        PenState s = PavePenItem.state(pen);
        int stone = s.stone(), gold = s.gold(), charge = s.charge();
        if (plan.stone()) {
            if (stone <= 0 && take(player, Items.SMOOTH_STONE)) stone += PaveRules.ROADS_PER_MATERIAL;
            stone--;
        }
        if (plan.gold()) {
            if (gold <= 0 && take(player, Items.GOLD_INGOT)) gold += PaveRules.ROADS_PER_MATERIAL;
            gold--;
        }
        if (!free(player)) {
            charge = refuel(player, charge, plan.cost());
            charge = Math.max(0, charge - plan.cost());
        }
        PavePenItem.set(pen, s.withCredit(Math.max(0, stone), Math.max(0, gold)).withCharge(charge));
    }

    /** Burns buds from the inventory into {@code charge} until it covers {@code need}, never past the pen's capacity by more than a bud. */
    private static int refuel(Player player, int charge, int need) {
        int capacity = numbers().capacity();
        for (Fuel f : FUELS) {
            while (charge < need && charge + f.bi() <= Math.max(capacity, need + f.bi()) && take(player, f.item().get())) charge += f.bi();
        }
        return charge;
    }

    private static boolean take(Player player, Item item) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                inv.setChanged();
                return true;
            }
        }
        return false;
    }

    /** Whether the world lets the player change this block: spawn protection, claims and other mods' break checks all apply. */
    static boolean allowed(ServerPlayer player, BlockPos pos, BlockState state) {
        Level level = player.level();
        return player.mayBuild() && level.mayInteract(player, pos)
                && !net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state).isCanceled();
    }

    /** A tick of a held pen: works on the block the player looks at. */
    public static void tick(ServerPlayer player, ItemStack pen) {
        HitResult hit = player.pick(player.blockInteractionRange(), 0, false);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) work(player, pen, block.getBlockPos());
        else stop(player);
    }

    /** A tick of work on {@code pos}. Returns true on the tick the block is paved. */
    public static boolean work(ServerPlayer player, ItemStack pen, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        Plan plan = plan(level, pos, player, pen, msg -> player.displayClientMessage(msg, true));
        Component short_ = plan == null ? null : shortfall(player, pen, plan);
        if (plan == null || short_ != null) {
            if (short_ != null) player.displayClientMessage(short_, true);
            stop(player);
            return false;
        }
        BlockState now = level.getBlockState(pos);
        var at = new ResourceKeyPos(level.dimension(), pos.immutable());
        Work w = WORK.get(player.getUUID());
        if (w == null || !w.at().equals(at) || w.state() != now) {
            stop(player);
            w = new Work(at, now, 0);
        }
        int done = w.done() + 1;
        if (done < plan.ticks()) {
            WORK.put(player.getUUID(), new Work(at, now, done));
            level.destroyBlockProgress(player.getId(), pos, done * 10 / plan.ticks());
            if (done % 4 == 1) level.playSound(null, pos, now.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.5f, 0.8f);
            return false;
        }
        stop(player);
        if (!allowed(player, pos, now)) {
            player.displayClientMessage(Component.translatable("message.fourtwenty.pave_pen.protected"), true);
            return false;
        }
        pay(player, pen, plan);
        if (plan.action() == Action.LAY) level.levelEvent(2001, pos, Block.getId(now));
        level.setBlock(pos, plan.result(), Block.UPDATE_ALL);
        level.playSound(null, pos, plan.result().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, plan.action() == Action.LAY ? 1f : 1.2f);
        if (plan.action() != Action.LAY) level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.02);
        return true;
    }

    /** Ends the player's work, clearing its cracks. */
    public static void stop(Player player) {
        Work w = WORK.remove(player.getUUID());
        if (w != null && player.level() instanceof ServerLevel level && level.dimension().equals(w.at().level()))
            level.destroyBlockProgress(player.getId(), w.at().pos(), -1);
    }
}
