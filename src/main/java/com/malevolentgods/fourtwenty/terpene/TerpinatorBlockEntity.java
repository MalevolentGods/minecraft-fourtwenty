package com.malevolentgods.fourtwenty.terpene;

import com.malevolentgods.fourtwenty.domain.Terpenes;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * The Terpinator: three timed processes side by side, no fuel. Press: 2 weed buds -> 1 resin.
 * Refine: 1 resin -> 2 of the base the method dial picks. Blend: two bases in ports 1 and 2 -> an
 * accord, or an accord in port 1 and a base in port 3 -> a final terpene (see
 * {@link Terpenes#result}). The press feeds the refiner's resin slot directly. Each stage runs only
 * while switched on (the screen's Start/Stop buttons); refining only starts with its output slot
 * empty, and its method can't change while it runs, so one run never mixes two bases.
 */
public class TerpinatorBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int BUDS = 0, RESIN = 1, BASE_OUT = 2, PORT1 = 3, PORT2 = 4, PORT3 = 5, BLEND_OUT = 6, SIZE = 7;
    /** Ticks per step: quick but not instant (a tech upgrade could shorten these later). */
    public static final int PRESS_TIME = 80, REFINE_TIME = 60, BLEND_TIME = 40;
    public static final int DATA_PRESS = 0, DATA_REFINE = 1, DATA_BLEND = 2, DATA_METHOD = 3, DATA_RUNNING = 4, DATA_COUNT = 5;
    /** Stages, as bits of {@link #DATA_RUNNING} and as Start/Stop button ids (10 + stage). */
    public static final int STAGE_PRESS = 0, STAGE_REFINE = 1, STAGE_BLEND = 2;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int press, refine, blend, method;
    /** Which stages are switched on (bit per stage). */
    private int running;
    /** What the blend in progress is making, so changing the ports restarts it. */
    private String blending = "";

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_PRESS -> press;
                case DATA_REFINE -> refine;
                case DATA_BLEND -> blend;
                case DATA_METHOD -> method;
                case DATA_RUNNING -> running;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {
            switch (index) {
                case DATA_PRESS -> press = value;
                case DATA_REFINE -> refine = value;
                case DATA_BLEND -> blend = value;
                case DATA_METHOD -> method = Math.floorMod(value, 4);
                case DATA_RUNNING -> running = value;
                default -> {}
            }
        }

        @Override public int getCount() { return DATA_COUNT; }
    };

    public TerpinatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModTerpenes.TERPINATOR_ENTITY.get(), pos, state);
    }

    public ContainerData data() { return data; }

    /** The base the dial is set to. */
    public static Terpenes.Base method(int dial) { return Terpenes.Base.values()[Math.floorMod(dial, 4)]; }

    /** A slot's terpene code, or null. */
    public static Terpenes.Code code(ItemStack stack) {
        return stack.getItem() instanceof TerpeneItem t ? t.code() : null;
    }

    /** What the three ports would make now. */
    public static Optional<Terpenes.Code> result(ItemStack p1, ItemStack p2, ItemStack p3) {
        return Terpenes.result(code(p1), code(p2), code(p3));
    }

    /** Whether a stack may go in a slot (outputs take nothing; ports take terpenes that can be blended). */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case BUDS -> stack.is(ModItems.WEED_BUD.get());
            case RESIN -> stack.is(ModTerpenes.RESIN.get());
            case PORT1 -> code(stack) != null && !code(stack).isFinal();
            case PORT2, PORT3 -> code(stack) != null && code(stack).isBase();
            default -> false;
        };
    }

    private boolean fits(int slot, Item item, int count) {
        ItemStack s = items.get(slot);
        return s.isEmpty() || (s.is(item) && s.getCount() + count <= s.getMaxStackSize());
    }

    private void put(int slot, Item item, int count) {
        ItemStack s = items.get(slot);
        if (s.isEmpty()) items.set(slot, new ItemStack(item, count));
        else s.grow(count);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TerpinatorBlockEntity t) {
        boolean changed = false;
        boolean working = false;
        // Press.
        if (t.isRunning(STAGE_PRESS) && t.items.get(BUDS).getCount() >= 2 && t.fits(RESIN, ModTerpenes.RESIN.get(), 1)) {
            working = true;
            if (++t.press >= PRESS_TIME) {
                t.press = 0;
                t.items.get(BUDS).shrink(2);
                t.put(RESIN, ModTerpenes.RESIN.get(), 1);
                t.discovered(TerpeneKnowledge.RESIN);
                changed = true;
            }
        } else if (t.isRunning(STAGE_PRESS) && t.press != 0) {
            t.press = 0;                       // ran out of inputs (a stopped stage keeps its progress)
        }
        // Refine.
        Item base = ModTerpenes.terpene(Terpenes.Code.of(method(t.method)));
        if (t.isRunning(STAGE_REFINE) && !t.items.get(RESIN).isEmpty() && t.fits(BASE_OUT, base, 2)) {
            working = true;
            if (++t.refine >= REFINE_TIME) {
                t.refine = 0;
                t.items.get(RESIN).shrink(1);
                t.put(BASE_OUT, base, 2);
                t.discovered(method(t.method).code());
                changed = true;
            }
        } else if (t.isRunning(STAGE_REFINE) && t.refine != 0) {
            t.refine = 0;                       // ran out of inputs (a stopped stage keeps its progress)
        }
        // Blend.
        Optional<Terpenes.Code> made = result(t.items.get(PORT1), t.items.get(PORT2), t.items.get(PORT3));
        String target = made.map(Terpenes.Code::id).orElse("");
        if (!target.equals(t.blending)) {
            t.blending = target;
            t.blend = 0;
        }
        if (t.isRunning(STAGE_BLEND) && made.isPresent() && t.fits(BLEND_OUT, ModTerpenes.terpene(made.get()), 1)) {
            working = true;
            if (++t.blend >= BLEND_TIME) {
                t.blend = 0;
                for (int port : new int[] {PORT1, PORT2, PORT3}) if (!t.items.get(port).isEmpty()) t.items.get(port).shrink(1);
                t.put(BLEND_OUT, ModTerpenes.terpene(made.get()), 1);
                t.discovered(made.get().id());
                changed = true;
            }
        } else if (t.isRunning(STAGE_BLEND) && t.blend != 0) {
            t.blend = 0;                       // ran out of inputs (a stopped stage keeps its progress)
        }
        if (state.getValue(TerpinatorBlock.LIT) != working) level.setBlock(pos, state.setValue(TerpinatorBlock.LIT, working), 3);
        if (changed) t.setChanged();
    }

    /** Making something is discovering it, for everyone watching this Terpinator. */
    private void discovered(String key) {
        if (level == null) return;
        for (Player p : level.players()) {
            if (p instanceof net.minecraft.server.level.ServerPlayer sp && sp.containerMenu instanceof TerpinatorMenu m && m.container() == this) {
                TerpeneDiscovery.learn(sp, java.util.List.of(key));
            }
        }
    }

    public boolean isRunning(int stage) { return (running & (1 << stage)) != 0; }

    /** Why a stage can't start now (a translation key), or null if it can. */
    public static String cannotStart(int stage, ItemStack refineOutput) {
        return stage == STAGE_REFINE && !refineOutput.isEmpty() ? "gui.fourtwenty.terpinator.refuse.output" : null;
    }

    /** Switches a stage on or off; returns false if it may not start (see {@link #cannotStart}). Stopping keeps its progress. */
    public boolean toggle(int stage) {
        if (stage < 0 || stage > 2) return false;
        if (!isRunning(stage) && cannotStart(stage, items.get(BASE_OUT)) != null) return false;
        running ^= 1 << stage;
        setChanged();
        return true;
    }

    /** Sets the refining method (0..3, in base order); resets the refine in progress. Not while refining. */
    public void setMethod(int dial) {
        if (isRunning(STAGE_REFINE) || Math.floorMod(dial, 4) == method) return;
        method = Math.floorMod(dial, 4);
        refine = 0;
        setChanged();
    }

    // Container.

    @Override public int getContainerSize() { return SIZE; }

    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override public @Nonnull ItemStack getItem(int slot) { return items.get(slot); }

    @Override public @Nonnull ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }

    @Override public @Nonnull ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override public void setItem(int slot, @Nonnull ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, @Nonnull ItemStack stack) { return accepts(slot, stack); }

    @Override public boolean stillValid(@Nonnull Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override public void clearContent() { items.clear(); }

    // Menu.

    @Override public @Nonnull Component getDisplayName() { return Component.translatable("container.fourtwenty.terpinator"); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new TerpinatorMenu(id, inventory, this, data);
    }

    // Saving.

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("press", press);
        tag.putInt("refine", refine);
        tag.putInt("blend", blend);
        tag.putInt("method", method);
        tag.putInt("running", running);
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        press = tag.getInt("press");
        refine = tag.getInt("refine");
        blend = tag.getInt("blend");
        method = Math.floorMod(tag.getInt("method"), 4);
        running = tag.getInt("running") & 7;
    }
}
