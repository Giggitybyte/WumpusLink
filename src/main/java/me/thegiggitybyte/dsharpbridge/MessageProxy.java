package me.thegiggitybyte.dsharpbridge;

import eu.pb4.placeholders.TextParser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.net.URL;
import java.util.UUID;

public class MessageProxy {
    private final static URL DEFAULT_AVATAR_URL;
    
    private static DiscordApi discordApi;
    private static MinecraftServer minecraftServer;
    
    static {
        DEFAULT_AVATAR_URL = createUrl("https://i.imgur.com/x4IwajC.png"); // Repeating command block
        
        ServerLifecycleEvents.SERVER_STARTING.register(server -> minecraftServer = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> discordApi.disconnect().join());
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resourceManager) -> initializeDiscord());
    }
    
    public static void initializeDiscord() {
        if (discordApi != null)
            discordApi.disconnect().join();
        
        var token = DSharpBridge.getConfig().get("discord-bot-token");
        discordApi = new DiscordApiBuilder()
                .setToken(token)
                .login()
                .join();
        
        var channelId = DSharpBridge.getConfig().get("discord-channel-id");
        var channel = discordApi.getServerTextChannelById(channelId).orElseThrow();
        
        channel.addMessageCreateListener(MessageProxy::proxyMessageToMinecraft);
    }
    
    public static void proxyMessageToDiscord(String message, String senderName, UUID senderUuid) {
        String webhookUrl = DSharpBridge.getConfig().get("discord-webhook-url");
        URL avatarUrl;
        
        if (senderUuid == null || senderUuid == Util.NIL_UUID)
            avatarUrl = DEFAULT_AVATAR_URL;
        else
            avatarUrl = createUrl("https://crafatar.com/renders/head/" + senderUuid + "?default=MHF_Steve&overlay");
        
        discordApi.getIncomingWebhookByUrl(webhookUrl).thenAcceptAsync(webhook -> {
            webhook.sendMessage(message, senderName, avatarUrl).join();
        }).exceptionally(ExceptionLogger.get());
        
    }
    
    public static void proxyMessageToDiscord(Text message, String senderName, UUID senderUuid) {
        proxyMessageToDiscord(message.getString(), senderName, senderUuid);
    }
    
    private static void proxyMessageToMinecraft(MessageCreateEvent event) {
        var author = event.getMessage().getAuthor();
        if (author.isBotUser() || author.isWebhook()) return;
        
        var userInfo = author.getDiscriminatedName() + " (" + author.getIdAsString() + ")";
        var userInfoText = new LiteralText(userInfo).setStyle(Style.EMPTY.withItalic(true));
        var userHoverEvent = HoverEvent.Action.SHOW_TEXT.buildHoverEvent(userInfoText);
        var userStyle = Style.EMPTY.withHoverEvent(userHoverEvent);
        
        author.getRoleColor().ifPresent(color -> {
            userInfoText.setStyle(Style.EMPTY.withColor(color.getRGB()));
        });
        
        MutableText replyText = new LiteralText("");
        var optionalMessageReference = event.getMessage().getMessageReference();
        if (optionalMessageReference.isPresent() && optionalMessageReference.get().getMessage().isPresent()) {
            var replyMessage = optionalMessageReference.get().getMessage().get();
            var replyAuthor = replyMessage.getAuthor();
            var replyInfoText = replyAuthor.getDiscriminatedName() + " (" + replyAuthor.getIdAsString() + ")\n\n" + replyMessage.getReadableContent();
            var replyHoverEvent = HoverEvent.Action.SHOW_TEXT.buildHoverEvent(new LiteralText(replyInfoText));
            var replyStyle = Style.EMPTY.withHoverEvent(replyHoverEvent).withColor(Formatting.AQUA);
            replyText = new LiteralText("[↩] ").setStyle(replyStyle); // hope that emoji works
        }
        
        var senderText = new LiteralText(author.getDisplayName()).setStyle(userStyle);
        
        var messageText = new LiteralText("")
                .append(replyText)
                .append(senderText)
                .append(" » ")
                .append(TextParser.parse(event.getReadableMessageContent())); // TODO: limit parsing to only markdown
        
        minecraftServer.getPlayerManager().broadcast(messageText, MessageType.CHAT, Util.NIL_UUID);
    }
    
    static URL createUrl(String string) {
        try {
            return new URL(string); // Checked exceptions are for clowns.
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
