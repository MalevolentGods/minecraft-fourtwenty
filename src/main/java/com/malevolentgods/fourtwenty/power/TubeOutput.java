package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.power.WickBlock.Link;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A machine's end of its Transport Tubes. A network has no direction: everything on it can send to everything else, at the same time, so each machine just walks
 * the network and shares its outputs out evenly among the blocks that want them, straight away. A network that runs into an unloaded chunk can't be trusted to
 * deliver, so it counts as blocked and the machine stops until the chunk is back.
 */
public final class TubeOutput {
    public record Line(List<BlockPos> ends, boolean blocked) {}

    /** The longest line it will follow: far past any sane build, short of a tube loop's worst case. */
    public static final int MAX_TUBES = 1024;
    private static final int EVERY = 10;

    private boolean blocked;
    private int wait;
    private int turn;

    public boolean blocked() { return blocked; }

    /** Follows every tube from {@code start}. */
    public static Line walk(Level level, BlockPos start, Predicate<BlockPos> loaded) {
        List<BlockPos> ends = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(Set.of(start));
        var queue = new ArrayDeque<BlockPos>();
        for (Direction d : Direction.values()) {
            BlockPos next = start.relative(d);
            if (loaded.test(next) && level.getBlockState(next).getBlock() instanceof TubeBlock) {
                seen.add(next);
                queue.add(next);
            }
        }
        boolean blocked = false;
        int tubes = 0;
        while (!queue.isEmpty() && tubes++ < MAX_TUBES) {
            BlockPos tube = queue.poll();
            BlockState state = level.getBlockState(tube);
            for (Direction d : Direction.values()) {
                BlockPos next = tube.relative(d);
                if (!loaded.test(next)) {
                    blocked |= state.getValue(WickBlock.SIDES.get(d)) != Link.NONE;     // all a tube can know about a chunk that isn't there
                } else if (seen.add(next)) {
                    if (level.getBlockState(next).getBlock() instanceof TubeBlock) queue.add(next);
                    else if (level.getBlockState(next).is(ModPower.TUBE_BLOCKS)) ends.add(next);
                }
            }
        }
        return new Line(ends, blocked);
    }

    /** Every so often: checks the line and sends off what's in the slots from {@code first} to the end of {@code items}. */
    public void tick(Level level, BlockPos pos, Container items, int first) {
        if (wait-- > 0) return;
        wait = EVERY;
        Line line = walk(level, pos, level::hasChunkAt);
        blocked = line.blocked();
        if (blocked) return;
        for (int slot = first; slot < items.getContainerSize(); slot++) {
            ItemStack stack = items.getItem(slot);
            if (stack.isEmpty()) continue;
            ItemStack left = share(level, line.ends(), stack.copy());
            if (left.getCount() != stack.getCount()) items.setItem(slot, left);
        }
    }

    /** Deals {@code stack} round the ends a bit at a time, carrying on from wherever the last one left off; what nobody wanted comes back. */
    private ItemStack share(Level level, List<BlockPos> ends, ItemStack stack) {
        boolean moved = !ends.isEmpty();
        while (moved && !stack.isEmpty()) {
            moved = false;
            int deal = Math.max(1, stack.getCount() / ends.size());
            for (int i = 0; i < ends.size() && !stack.isEmpty(); i++) {
                BlockPos end = ends.get(Math.floorMod(turn++, ends.size()));
                var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, end, Direction.UP);
                if (handler == null) continue;
                ItemStack rest = ItemHandlerHelper.insertItem(handler, stack.copyWithCount(Math.min(deal, stack.getCount())), false);
                int sent = Math.min(deal, stack.getCount()) - rest.getCount();
                if (sent > 0) {
                    stack.shrink(sent);
                    moved = true;
                }
            }
        }
        return stack;
    }
}
