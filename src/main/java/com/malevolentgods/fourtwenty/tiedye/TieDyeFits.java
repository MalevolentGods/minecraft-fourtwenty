package com.malevolentgods.fourtwenty.tiedye;

import com.malevolentgods.fourtwenty.domain.tiedye.SquareFit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps tie-dyed blocks' squares up to date (research R11). When one is placed, removed or recoloured, every tie-dyed block within
 * {@link SquareFit#MAX} of it is refitted in each of its three planes: 0 = a wall facing x (u = z, v = y), 1 = a wall facing z
 * (u = x, v = y), 2 = a floor (u = x, v = z). Server side; the fits travel to clients with the block entity.
 */
public final class TieDyeFits {
    private TieDyeFits() {}

    public static final int PLANES = 3;

    public static void refresh(Level level, BlockPos changed) {
        if (level == null || level.isClientSide()) return;
        Map<Long, String> looks = new HashMap<>();
        int r = SquareFit.MAX;
        for (BlockPos p : BlockPos.betweenClosed(changed.offset(-r, -r, -r), changed.offset(r, r, r))) {
            if (!(level.getBlockEntity(p) instanceof TieDyeBlockEntity be)) continue;
            String look = look(level, p, looks);
            if (look == null) continue;
            int[] fits = new int[PLANES * 3];
            for (int plane = 0; plane < PLANES; plane++) {
                final int pl = plane;
                BlockPos at = p.immutable();
                SquareFit.Fit f = SquareFit.fit((du, dv) -> look.equals(look(level, offset(at, pl, du, dv), looks)));
                fits[plane * 3] = f.n();
                fits[plane * 3 + 1] = f.i();
                fits[plane * 3 + 2] = f.j();
            }
            be.setFits(fits);
        }
    }

    /** The block {@code du, dv} along plane {@code plane} from {@code pos}. */
    public static BlockPos offset(BlockPos pos, int plane, int du, int dv) {
        return switch (plane) {
            case 0 -> pos.offset(0, dv, du);
            case 1 -> pos.offset(du, dv, 0);
            default -> pos.offset(du, 0, dv);
        };
    }

    /** A placed tie-dyed block's look as one string (block, pattern, colourway), or null if there's none there. Memoised per refresh. */
    private static String look(Level level, BlockPos pos, Map<Long, String> memo) {
        long key = pos.asLong();
        if (memo.containsKey(key)) return memo.get(key);
        String out = null;
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof TieDyeBlockEntity be) {
            var state = level.getBlockState(pos);
            out = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()) + "|" + state.getValue(TieDyeBlocks.PATTERN) + "|" + be.colourway();
        }
        memo.put(key, out);
        return out;
    }
}
