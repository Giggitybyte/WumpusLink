package me.thegiggitybyte.wumpuslink.mixin;

import com.mojang.authlib.GameProfile;
import me.thegiggitybyte.wumpuslink.MessageProxy;
import me.thegiggitybyte.wumpuslink.WumpusLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Shadow public abstract ServerStatHandler getStatHandler();
    
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile, @Nullable PlayerPublicKey publicKey) {
        super(world, pos, yaw, gameProfile, publicKey);
    }
    
    @Inject(
            method = "onDeath",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/util/registry/RegistryKey;)V"
            )
    )
    public void playerDeathMessageProxy(DamageSource source, CallbackInfo ci) {
        var canRelayDeathMessage = WumpusLink.getConfig().getOrDefault("minecraft-player-death-messages", true);
        if (canRelayDeathMessage == false) return;
        
        var recentDamage = this.getDamageTracker().getMostRecentDamage();
        var embed = new EmbedBuilder();
        Entity responsibleEntity = null;
        
        if ((recentDamage == null) || recentDamage.getDamageSource() == DamageSource.GENERIC) {
            embed.setTitle("Spontaneous death");
        } else if (recentDamage.getDamageSource() == DamageSource.FALL) {
            var furthestFallDamage = ((DamageTrackerAccessor) this.getDamageTracker()).getFurthestFall();
            if (furthestFallDamage.getDamageSource() != DamageSource.FALL && furthestFallDamage.getDamageSource() != DamageSource.OUT_OF_WORLD) {
                if (recentDamage.getAttacker() != null && furthestFallDamage.getAttacker() != null){
                    var recentAttackerUuid = recentDamage.getAttacker().getUuid();
                    var fallDamageAttackerUuid = furthestFallDamage.getAttacker().getUuid();
    
                    if (fallDamageAttackerUuid.equals(recentAttackerUuid) == false)
                        responsibleEntity = recentDamage.getAttacker();
                }
            }
    
            embed.setTitle("Fell from " + recentDamage.getFallDistance() + "m");
        } else {
            var embedTitle = switch (recentDamage.getDamageSource().getName()) {
                case "drown" -> "Drowned";
                case "starve" -> "Starved";
                case "inWall" -> "Suffocated";
                case "onFire", "inFire" -> "Burnt alive";
                case "lava" -> "Melted in lava";
                case "hotFloor" -> "Melted on magma";
                case "lightningBolt" -> "Struck by lightning";
                case "flyIntoWall" -> "Experienced kinetic energy";
                case "magic" -> "Killed with magic";
                case "wither", "witherSkull" -> "Withered away";
                case "explosion", "badRespawnPoint" -> "Died from explosion";
                case "fireworks" -> "Exploded with style";
                case "stalagmite" -> "Impaled by stalagmite";
                case "fallingStalactite" -> "Skewered by stalactite";
                case "dragonBreath" -> "Roasted by dragon breath";
                case "outOfWorld" -> "Absorbed by the void";
                case "sting" -> "Stung to death";
                case "frozen" -> "Froze to death";
                case "cactus" -> "Pricked to death";
                case "sweetBerryBush" -> "Poked to death";
                case "cramming" -> "Squished to death";
                case "anvil", "fallingBlock" -> "Squashed to death";
                default -> "Died";
            };
            
            embed.setTitle(embedTitle);
            responsibleEntity = this.getPrimeAdversary();
        }
    
        if (responsibleEntity != null)
            embed.setDescription("while fighting " + responsibleEntity.getDisplayName().getString() + "*");
        
        var deathCount = this.getStatHandler().getStat(Stats.CUSTOM, Stats.DEATHS) + 1;
        embed.setFooter(deathCount + (deathCount == 1 ? " death" : " deaths"));
        embed.setColor(Color.LIGHT_GRAY);
    
        // TODO: thumbnail of player skin with red overlay
        
        MessageProxy.sendPlayerMessageToDiscord(this, embed);
    }
}
