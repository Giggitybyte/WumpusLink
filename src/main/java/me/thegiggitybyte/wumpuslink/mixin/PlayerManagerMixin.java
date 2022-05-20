package me.thegiggitybyte.wumpuslink.mixin;

import me.thegiggitybyte.wumpuslink.MessageProxy;
import me.thegiggitybyte.wumpuslink.WumpusLink;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
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
                    target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"
            )
    )
    public void playerConnectMessageProxy(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        var canRelayJoinMessage = WumpusLink.getConfig().getOrDefault("minecraft-join-leave-messages", true);
        
        if (canRelayJoinMessage) {
            var thumbnail = WumpusLink.getMinecraftPlayerBody(player.getUuid());
            var embed = new EmbedBuilder()
                    .setTitle("Player Joined")
                    .setDescription(player.getEntityName())
                    .setThumbnail(thumbnail)
                    .setColor(Color.GREEN);
            
            MessageProxy.sendMessageToDiscord(embed);
        }
    }
}
