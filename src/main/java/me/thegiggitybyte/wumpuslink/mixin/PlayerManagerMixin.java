package me.thegiggitybyte.wumpuslink.mixin;

import me.thegiggitybyte.wumpuslink.MessageProxy;
import me.thegiggitybyte.wumpuslink.WumpusLink;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(
            method = "onPlayerConnect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"
            )
    )
    public void playerConnectMessageProxy(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        var canRelayJoinMessage = WumpusLink.getConfig().getOrDefault("minecraft-join-leave-messages", true);
        if (!canRelayJoinMessage) return;

        var playTimeTicks = player.getStatHandler().getStat(Stats.CUSTOM, Stats.PLAY_TIME);
        var leaveCount = player.getStatHandler().getStat(Stats.CUSTOM, Stats.LEAVE_GAME);
        var embed = new EmbedBuilder();

        if ((leaveCount == 0) & (playTimeTicks == 0)) {
            embed.setTitle("New player");
            embed.setDescription("Joined the server for the first time");
            embed.setThumbnail(WumpusLink.getMinecraftPlayerRender(player.getUuid()));
            embed.setColor(Color.YELLOW);

        } else {
            embed.setTitle("Joined the server");
            embed.setColor(Color.GREEN);

            var playTimeHours = playTimeTicks / 3600.0;
            var playTimeMinutes = playTimeTicks / 60.0;

            if (playTimeHours > 0)
                embed.setDescription(playTimeHours + " hours of playtime");
            else if (playTimeMinutes > 1)
                embed.setDescription(playTimeMinutes + " minutes of playtime");
        }

        MessageProxy.sendPlayerMessageToDiscord(player, embed);
    }
}
