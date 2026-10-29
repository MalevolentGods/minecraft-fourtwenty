package com.malevolentgods.fourtwenty.infuser;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;

/**
 * The Infuser's other half: a Landrace seed infused with a Minecraft-colour terpene becomes a seed of the strain for
 * that colour's hue. Olive (Mossy Philosopher) takes two, green and brown; magenta and pink both make Cotton Candy Kush.
 * Done from the Infuse tab, at an Infuser, with the seed and terpenes in the player's inventory.
 */
public final class Infusion {
    private Infusion() {}

    /** A strain and the Minecraft-colour terpenes (mc_ codes) that breed it. */
    public record Recipe(Strain strain, List<String> terpenes) {
        public List<Item> items() { return terpenes.stream().map(c -> (Item) ModTerpenes.MINECRAFT_TERPENES.get(c).get()).toList(); }
    }

    public static final List<Recipe> RECIPES = List.of(
            new Recipe(Strain.WAKE_AND_BAKE, List.of("mc_white")),
            new Recipe(Strain.SPACEBAR_STATIC, List.of("mc_light_gray")),
            new Recipe(Strain.CLOUD_NINE_DRIFT, List.of("mc_gray")),
            new Recipe(Strain.COUCHLOCK_HUM, List.of("mc_black")),
            new Recipe(Strain.BROWNIE_BATCH, List.of("mc_brown")),
            new Recipe(Strain.SUNSHINE_DAYDREAM, List.of("mc_yellow")),
            new Recipe(Strain.CHERRY_PIE, List.of("mc_red")),
            new Recipe(Strain.TANGIE_TANGO, List.of("mc_orange")),
            new Recipe(Strain.MOSSY_PHILOSOPHER, List.of("mc_green", "mc_brown")),
            new Recipe(Strain.LIME_IN_THE_COCONUT, List.of("mc_lime")),
            new Recipe(Strain.STICKY_ICKY, List.of("mc_green")),
            new Recipe(Strain.MERMAID_HOTBOX, List.of("mc_cyan")),
            new Recipe(Strain.PURPLE_HAZE, List.of("mc_purple")),
            new Recipe(Strain.COTTON_CANDY_KUSH, List.of("mc_pink")),
            new Recipe(Strain.COTTON_CANDY_KUSH, List.of("mc_magenta")),
            new Recipe(Strain.BLUE_DREAM, List.of("mc_blue")),
            new Recipe(Strain.THIRD_EYE_TONIC, List.of("mc_light_blue")));

    public static boolean landraceSeed(ItemStack stack) {
        return stack.is(ModItems.WEED_SEEDS.get()) && ModDataComponents.strain(stack) == Strain.LANDRACE;
    }

    private static int count(Inventory inv, Predicate<ItemStack> what) {
        int n = 0;
        for (ItemStack s : inv.items) if (what.test(s)) n += s.getCount();
        return n;
    }

    /** How many times the player could do this recipe from what they carry. */
    public static int available(Player player, Recipe recipe) {
        Inventory inv = player.getInventory();
        int n = count(inv, Infusion::landraceSeed);
        for (Item terp : recipe.items()) n = Math.min(n, count(inv, s -> s.is(terp)));
        return n;
    }

    private static void take(Inventory inv, Predicate<ItemStack> what) {
        for (ItemStack s : inv.items) {
            if (what.test(s)) {
                s.shrink(1);
                return;
            }
        }
    }

    /** Whether the player stands at an Inspiration Infuser (within 6 blocks of one). */
    public static boolean nearInfuser(Player player) {
        BlockPos at = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-6, -3, -6), at.offset(6, 3, 6))) {
            if (player.level().getBlockState(p).getBlock() instanceof InspirationInfuserBlock) return true;
        }
        return false;
    }

    /** Does one infusion: a Landrace seed and the terpenes in, one strain seed out. */
    public static boolean infuse(ServerPlayer player, Recipe recipe) {
        if (available(player, recipe) <= 0 || !nearInfuser(player)) return false;
        Inventory inv = player.getInventory();
        take(inv, Infusion::landraceSeed);
        for (Item terp : recipe.items()) take(inv, s -> s.is(terp));
        ItemStack seed = ModDataComponents.withStrain(new ItemStack(ModItems.WEED_SEEDS.get()), recipe.strain());
        if (!inv.add(seed)) player.drop(seed, false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8f, 1.3f);
        player.containerMenu.broadcastChanges();
        return true;
    }

    /** Client to server: infuse recipe {@code index} of {@link #RECIPES}. */
    public record Infuse(int index) implements CustomPacketPayload {
        public static final Type<Infuse> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fourtwenty", "infuser_infuse"));
        public static final StreamCodec<ByteBuf, Infuse> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Infuse::index, Infuse::new);
        @Override public @Nonnull Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void onInfuse(Infuse msg, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player && msg.index() >= 0 && msg.index() < RECIPES.size()) infuse(player, RECIPES.get(msg.index()));
    }
}
