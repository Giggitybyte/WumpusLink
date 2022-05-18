package me.thegiggitybyte.dsharpbridge.mixin;

import me.thegiggitybyte.dsharpbridge.MessageProxy;
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

import java.util.Map;
import java.util.UUID;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Shadow @Final private Map<UUID, ServerPlayerEntity> playerMap;
    
    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At("TAIL"))
    public void discordSystemMessageProxy(Text message, MessageType type, UUID senderUuid, CallbackInfo ci) {
        String name = "";
        
        if (senderUuid != null && senderUuid != Util.NIL_UUID && playerMap.containsKey(senderUuid))
            name = playerMap.get(senderUuid).getEntityName();
        
        MessageProxy.proxyMessageToDiscord(message, name.isEmpty() ? "Server" : name, senderUuid);
    }
}
