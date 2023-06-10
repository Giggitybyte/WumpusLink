package me.thegiggitybyte.wumpuslink.mixin;

import me.thegiggitybyte.wumpuslink.MessageProxy;
import me.thegiggitybyte.wumpuslink.WumpusLink;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(PlayerAdvancementTracker.class)
public class PlayerAdvancementTrackerMixin {
    @Shadow
    private ServerPlayerEntity owner;

    @Inject(
            method = "grantCriterion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V")
    )
    public void playerAdvancementMessageProxy(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        boolean canRelayAdvancements = WumpusLink.getConfig().getOrDefault("minecraft-advancement-messages", true);
        if (!canRelayAdvancements) return;

        var embed = new EmbedBuilder();
        var advancementDisplay = advancement.getDisplay();
        assert advancementDisplay != null;
        var advancementFrame = advancementDisplay.getFrame();

        switch (advancementFrame.getId()) {
            case "task" -> embed.setTitle("Advancement Made!");
            case "challenge" -> embed.setTitle("Challenge Complete!");
            case "goal" -> embed.setTitle("Goal Reached!");
        }

        embed.addField(
                advancementDisplay.getTitle().getString(),
                advancementDisplay.getDescription().getString()
        );

        var rgbInteger = advancementFrame.getTitleFormat().getColorValue();
        var advancementColor = new Color(rgbInteger);
        embed.setColor(advancementColor);

        // TODO: add advancement icon as embed image

        MessageProxy.sendPlayerMessageToDiscord(owner, embed);
    }

}
