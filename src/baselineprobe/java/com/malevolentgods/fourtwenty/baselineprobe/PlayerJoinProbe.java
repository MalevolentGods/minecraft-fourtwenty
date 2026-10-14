package com.malevolentgods.fourtwenty.baselineprobe;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;
@net.neoforged.fml.common.Mod("fourtwenty_baselineprobe")
@GameTestHolder("fourtwenty_baselineprobe")
@PrefixGameTestTemplate(false)
public final class PlayerJoinProbe {
    public PlayerJoinProbe() {}
    public static net.minecraft.server.level.ServerPlayer player(net.minecraft.gametest.framework.GameTestHelper h) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "effect-fixture"), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        return player;
    }
    public static void remove(ServerPlayer player) {
        player.getServer().getPlayerList().remove(player);
        player.discard();
    }

    @GameTest(template="empty")
    public static void playerJoinWithoutFourTwenty(GameTestHelper h) {
        var player = player(h);
        remove(player);
        h.succeed();
    }
}
