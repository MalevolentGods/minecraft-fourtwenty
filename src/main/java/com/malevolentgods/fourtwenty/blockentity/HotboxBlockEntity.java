package com.malevolentgods.fourtwenty.blockentity;

import com.malevolentgods.fourtwenty.block.HotboxBlock;
import com.malevolentgods.fourtwenty.domain.HotboxRules;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import java.util.List;

/**
 * Keeps the 3x3 chunks round the Hotbox running for an hour after the last player leaves them. Nothing survives a restart, by design:
 * tickets saved by the last run are dropped on load, and it starts again when a player next comes into the area.
 */
public class HotboxBlockEntity extends BlockEntity {
    private static final int STEP = 20;
    private static final int RADIUS = 1;

    public static final TicketController TICKETS = new TicketController(ResourceLocation.fromNamespaceAndPath("fourtwenty", "hotbox"),
            (level, tickets) -> List.copyOf(tickets.getBlockTickets().keySet()).forEach(tickets::removeAllTickets));

    private int left;
    private boolean holding;

    public HotboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOTBOX.get(), pos, state);
    }

    public int ticksLeft() {
        return left;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, HotboxBlockEntity box) {
        if (level.getGameTime() % STEP != 0) return;
        ChunkPos centre = new ChunkPos(pos);
        boolean near = level.players().stream().anyMatch(p -> !p.isSpectator()
                && Math.abs(p.chunkPosition().x - centre.x) <= RADIUS && Math.abs(p.chunkPosition().z - centre.z) <= RADIUS);
        box.left = HotboxRules.next(box.left, near, level.getServer().getPlayerCount() > 0, STEP);
        boolean on = box.left > 0;
        if (on != box.holding) {
            hold(level, pos, on);
            box.holding = on;
        }
        if (state.getValue(HotboxBlock.LIT) != on) level.setBlock(pos, state.setValue(HotboxBlock.LIT, on), 3);
    }

    public static void hold(ServerLevel level, BlockPos pos, boolean on) {
        com.malevolentgods.fourtwenty.util.HotboxNetwork.set(level, pos, on);
        ChunkPos centre = new ChunkPos(pos);
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                TICKETS.forceChunk(level, pos, centre.x + dx, centre.z + dz, on, true);
            }
        }
    }
}
