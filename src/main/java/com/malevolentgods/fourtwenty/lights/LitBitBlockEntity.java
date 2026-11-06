package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.Circuit;
import com.malevolentgods.fourtwenty.domain.LedRules;
import com.malevolentgods.fourtwenty.domain.LitBitIdRules;
import com.malevolentgods.fourtwenty.power.BIBlockEntity;
import com.malevolentgods.fourtwenty.power.BINode;
import com.malevolentgods.fourtwenty.power.BurnOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * A Lit Bit's ID, its colour, and how lit it is. In the circuit it is an LED: its driver is set from its colour, and the solve tells it
 * how far up its knee the voltage across it puts it. What players see is the colour times that, sent in batches (LampSync).
 */
public class LitBitBlockEntity extends BIBlockEntity {
    private static final int LEVEL_EVERY = 10, FX_EVERY = 20, OVERLOAD_TICKS = 60;
    private int id, colour, shown;
    private double brightness;
    private int status = Circuit.NO_LOOP;
    private long overloadedUntil, levelChangedAt = Long.MIN_VALUE / 2;
    private boolean fromMemory;

    private final BINode node = new BINode() {
        @Override public BIRules.Tier tier() { return BIRules.Tier.PUFF; }
        @Override public long stored() { return 0; }
        @Override public long capacity() { return 0; }
        @Override public long receive(long amount) { return 0; }
        @Override public long extract(long amount) { return 0; }
        @Override public boolean canReceive() { return false; }
        @Override public boolean canExtract() { return false; }
        @Override public Role role() { return Role.LED; }
        @Override public double ledAmps() { return LightsConfig.led().setAmps(LedRules.red(colour), LedRules.green(colour), LedRules.blue(colour)); }
        @Override public void lit(double b, int s) { onLit(b, s); }
        @Override public void overload(long now) { overloadedUntil = now + OVERLOAD_TICKS; }
        @Override public boolean overloaded(long now) { return now < overloadedUntil; }
    };

    public LitBitBlockEntity(BlockPos pos, BlockState state) {
        super(ModLights.LIT_BIT_ENTITY.get(), pos, state);
    }

    public BINode node() { return node; }

    public int id() { return id; }

    public int colour() { return colour; }

    /** What players see, 0xRRGGBB: the colour as lit as the voltage allows. */
    public int shown() { return shown; }

    public int status() { return status; }

    /** Whether its ID came back from the spot it was placed on (the Sticky Finger then leaves it). */
    public boolean fromMemory() { return fromMemory; }

    void assign(int newId, boolean remembered) {
        fromMemory = remembered;
        setId(newId);
    }

    public void setId(int newId) {
        id = LitBitIdRules.clamp(newId);
        if (level instanceof ServerLevel server) LitBitIds.of(server).live(worldPosition, id);
        setChanged();
        sync();
    }

    /** A show sets it. */
    public void setColour(int rgb) {
        if (rgb == colour) return;
        colour = rgb & 0xFFFFFF;
        setChanged();
        refresh();
    }

    private void onLit(double b, int s) {
        brightness = b;
        status = s;
        refresh();
    }

    private void refresh() {
        int now = scale(colour, level != null && node.overloaded(level.getGameTime()) ? 0 : brightness);
        if (now == shown) return;
        shown = now;
        if (level instanceof ServerLevel server) LampSync.queue(server, worldPosition, shown);
    }

    private static int scale(int rgb, double b) {
        double k = Math.max(0, Math.min(1, b));
        return LedRules.pack((int) Math.round(LedRules.red(rgb) * k), (int) Math.round(LedRules.green(rgb) * k), (int) Math.round(LedRules.blue(rgb) * k));
    }

    /** Client side: the colour the server says it shows. */
    public void showOnClient(int rgb) { shown = rgb; }

    @Override public void serverTick() {
        long now = level.getGameTime();
        if (node.overloaded(now)) {
            if (now % FX_EVERY == 0) BurnOut.overloadFx(level, worldPosition);
            refresh();
        }
        int want = LedRules.lightLevel(shown, 1);
        BlockState state = getBlockState();
        if (state.getValue(LitBitBlock.LEVEL) != want && now - levelChangedAt >= LEVEL_EVERY) {
            levelChangedAt = now;
            level.setBlock(worldPosition, state.setValue(LitBitBlock.LEVEL, want), 2);
        }
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server && id > 0) LitBitIds.of(server).live(worldPosition, id);
    }

    @Override public void setRemoved() {
        if (level instanceof ServerLevel server) LitBitIds.of(server).gone(worldPosition);
        super.setRemoved();
    }

    @Override public void onChunkUnloaded() {
        if (level instanceof ServerLevel server) LitBitIds.of(server).gone(worldPosition);
        super.onChunkUnloaded();
    }

    private void sync() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("id", id);
        tag.putInt("colour", colour);
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        id = tag.getInt("id");
        colour = tag.getInt("colour");
        if (tag.contains("shown")) shown = tag.getInt("shown");
    }

    @Override public @Nonnull CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.putInt("colour", colour);
        tag.putInt("shown", shown);
        return tag;
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
