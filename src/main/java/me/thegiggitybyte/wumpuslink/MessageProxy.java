package me.thegiggitybyte.wumpuslink;

import eu.pb4.placeholders.TextParser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.WebhookMessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class MessageProxy {
    public final static URL DEFAULT_AVATAR_URL;
    
    private static DiscordApi discordApi;
    private static MinecraftServer minecraftServer;
    
    static {
        DEFAULT_AVATAR_URL = WumpusLink.createUrl("https://i.imgur.com/x4IwajC.png"); // Repeating command block
        ServerLifecycleEvents.SERVER_STARTING.register(server -> minecraftServer = server);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> connectToDiscord());
    }
    
    public static CompletableFuture<Message> sendMessageToDiscord(String authorName, URL avatarUrl, String message) {
        return sendMessageToDiscord(authorName, avatarUrl, message, null);
    }
    
    public static CompletableFuture<Message> sendMessageToDiscord(String authorName, URL avatarUrl, EmbedBuilder embed) {
        return sendMessageToDiscord(authorName, avatarUrl, null, embed);
    }
    
    public static CompletableFuture<Message> sendMessageToDiscord(String authorName, URL avatarUrl, Text messageText) {
        return sendMessageToDiscord(authorName, avatarUrl, messageText.getString());
    }
    
    public static CompletableFuture<Message> sendMessageToDiscord(String message) {
        return sendMessageToDiscord(null, null, message);
    }
    
    public static CompletableFuture<Message> sendMessageToDiscord(EmbedBuilder embed) {
        return sendMessageToDiscord(null, null, embed);
    }
    
    public static CompletableFuture<Message> sendMessageToDiscord(Text messageText) {
        return sendMessageToDiscord(null, null, messageText);
    }
    
    public static CompletableFuture<Message> sendMessageToDiscord(String authorName, URL avatarUrl, String message, EmbedBuilder embed) {
        if ((message == null || message.isBlank()) && embed == null)
            throw new RuntimeException("message and embed cannot both be empty");
        
        String webhookUrl = WumpusLink.getConfig().get("discord-webhook-url");
    
        var author = (authorName == null || authorName.isBlank()) ? "Minecraft" : authorName;
        var avatar = avatarUrl == null ? DEFAULT_AVATAR_URL : avatarUrl;
    
        var allowedMentions = new AllowedMentionsBuilder()
                .setMentionEveryoneAndHere(false)
                .setMentionRoles(false)
                .setMentionUsers(false)
                .build();
    
        return new WebhookMessageBuilder()
                .setAllowedMentions(allowedMentions)
                .setContent(message)
                .addEmbed(embed)
                .setDisplayName(author)
                .setDisplayAvatar(avatar)
                .send(discordApi, webhookUrl)
                .exceptionally(ExceptionLogger.get());
    }
    
    static void connectToDiscord() {
        disconnectFromDiscord();
        
        var token = WumpusLink.getConfig().get("discord-bot-token");
        discordApi = new DiscordApiBuilder()
                .setToken(token)
                .login()
                .join();
        
        var channelId = WumpusLink.getConfig().get("discord-channel-id");
        var channel = discordApi.getServerTextChannelById(channelId).orElseThrow();
        
        
        
        channel.addMessageCreateListener(MessageProxy::sendMessageToMinecraft);
    }
    
    static void disconnectFromDiscord() {
        if (discordApi != null)
            discordApi.disconnect().join();
    }
    
    private static void sendMessageToMinecraft(MessageCreateEvent event) {
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
}
