package me.thegiggitybyte.wumpuslink.mixin;

import me.thegiggitybyte.wumpuslink.MessageProxy;
import me.thegiggitybyte.wumpuslink.config.JsonConfiguration;
import net.minecraft.network.message.SignedMessage;
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
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "handleDecoratedMessage", at = @At(value = "HEAD"))
    public void playerChatMessageProxy(SignedMessage message, CallbackInfo ci) {
        boolean canRelayChatMessages = JsonConfiguration.getUserInstance().getValue("minecraft-chat-messages").getAsBoolean();
        if (!canRelayChatMessages) return;

        MessageProxy.sendPlayerMessageToDiscord(this.player, message.getContent());
    }

    @Inject(method = "onDisconnected", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"))
    public void playerDisconnectMessageProxy(Text reason, CallbackInfo ci) {
        boolean canRelayDisconnectMessages = JsonConfiguration.getUserInstance().getValue("minecraft-join-leave-messages").getAsBoolean();
        if (!canRelayDisconnectMessages) return;

        var embed = new EmbedBuilder()
                .setTitle("Left the game")
                .setDescription("```java\n" + reason.getString() + "\n```")
                .setColor(Color.RED);

        MessageProxy.sendPlayerMessageToDiscord(this.player, embed);
    }
}
