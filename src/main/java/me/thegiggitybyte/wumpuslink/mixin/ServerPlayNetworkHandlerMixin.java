package me.thegiggitybyte.wumpuslink.mixin;

import me.thegiggitybyte.wumpuslink.MessageProxy;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    
    @Shadow public ServerPlayerEntity player;
    
    @Inject(method = "handleMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/filter/TextStream$Message;getFiltered()Ljava/lang/String;"))
    public void discordChatMessageProxy(TextStream.Message message, CallbackInfo ci) {
        MessageProxy.proxyMessageToDiscord(message.getRaw(), this.player.getEntityName(), this.player.getUuid());
    }
}
