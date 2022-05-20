package me.thegiggitybyte.wumpuslink.mixin;

import com.mojang.authlib.GameProfile;
import me.thegiggitybyte.wumpuslink.MessageProxy;
import me.thegiggitybyte.wumpuslink.WumpusLink;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }
    
    @Inject(
            method = "onDeath",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"
            )
    )
    public void playerDeathMessageProxy(DamageSource source, CallbackInfo ci) {
        var canRelayDeathMessage = WumpusLink.getConfig().getOrDefault("minecraft-player-death-messages", true);
    
        if (canRelayDeathMessage) {
            var thumbnail = WumpusLink.getMinecraftPlayerBody(this.uuid);
            var embed = new EmbedBuilder()
                    .setTitle("Player Died")
                    .setDescription(this.getDamageTracker().getDeathMessage().getString())
                    .setThumbnail(thumbnail)
                    .setColor(Color.RED);
        
            MessageProxy.sendMessageToDiscord(embed);
        }
    }
}
