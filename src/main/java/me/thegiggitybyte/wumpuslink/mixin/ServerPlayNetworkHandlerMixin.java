package me.thegiggitybyte.wumpuslink.mixin;

import me.thegiggitybyte.wumpuslink.MessageProxy;
import me.thegiggitybyte.wumpuslink.WumpusLink;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;
    
    @Inject(method = "handleMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/filter/TextStream$Message;getFiltered()Ljava/lang/String;"))
    public void playerChatMessageProxy(TextStream.Message message, CallbackInfo ci) {
        boolean canRelayChatMessages = WumpusLink.getConfig().getOrDefault("minecraft-chat-messages", true);
        if (canRelayChatMessages) {
            var avatarUrl = WumpusLink.getMinecraftPlayerHeadUrl(this.player.getUuid());
            MessageProxy.sendMessageToDiscord(this.player.getEntityName(), avatarUrl, message.getRaw());
        }
    }
    
    @Inject(method = "onDisconnected", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"))
    public void playerDisconnectMessageProxy(Text reason, CallbackInfo ci) {
        var canRelayDisconnectMessages = WumpusLink.getConfig().getOrDefault("minecraft-join-leave-messages", true);
        if (canRelayDisconnectMessages) {
    
            var thumbnail = "https://crafatar.com/renders/body/" + player.getUuid();
            var embed = new EmbedBuilder()
                    .setTitle("Player Left")
                    .setDescription(player.getEntityName())
                    .setThumbnail(thumbnail)
                    .setFooter(reason.getString())
                    .setColor(Color.RED);
    
            MessageProxy.sendMessageToDiscord(embed);
        }
    }
}
