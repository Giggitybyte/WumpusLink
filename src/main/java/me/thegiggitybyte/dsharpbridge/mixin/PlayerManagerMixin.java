package me.thegiggitybyte.dsharpbridge.mixin;

import me.thegiggitybyte.dsharpbridge.DSharpBridge;
import net.minecraft.network.MessageType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Shadow @Final private Map<UUID, ServerPlayerEntity> playerMap;
    
    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At("TAIL"))
    public void discordChatMessageProxy(Text content, Function<ServerPlayerEntity, Text> messageFactory, MessageType type, UUID senderUuid, CallbackInfo ci) {
        proxyMessageToDiscord(content, senderUuid);
    }
    
    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At("TAIL"))
    public void discordSystemMessageProxy(Text content, MessageType type, UUID sender, CallbackInfo ci) {
        proxyMessageToDiscord(content, sender);
    }
    
    private void proxyMessageToDiscord(Text messageText, UUID senderUuid) {
        try {
            URL iconUrl;
            
            if (senderUuid == null || senderUuid == Util.NIL_UUID)
                iconUrl = new URL("https://i.imgur.com/Yu7An4l.png"); // Command block
            else
                iconUrl = new URL("https://crafatar.com/avatars/" + senderUuid + " +?default=MHF_Steve&overlay");
    
            String senderName = playerMap.containsKey(senderUuid)
                    ? playerMap.get(senderUuid).getEntityName()
                    : "Server Message";
    
            DSharpBridge.proxyMessageToDiscord(messageText, senderName , iconUrl);
            
        } catch (Exception e) {
            DSharpBridge.LOGGER.error("Something went wrong while trying to send a message to Discord", e); // I hate checked exceptions :(
        }
    }
    
}
