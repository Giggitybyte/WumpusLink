package me.thegiggitybyte.dsharpbridge;

import com.mojang.brigadier.context.CommandContext;
import eu.pb4.placeholders.TextParser;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.darktree.simpleconfig.SimpleConfig;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class DSharpBridge implements DedicatedServerModInitializer {
    public static final Logger LOGGER;
    
    private static SimpleConfig config;
    private static DiscordApi discordApi;
    private MinecraftServer minecraftServer;
    
    static {
        LOGGER = LoggerFactory.getLogger("dsharpbridge");
    }
    
    public DSharpBridge() {
        initializeConfig();
        initializeDiscord();
        
        ServerLifecycleEvents.SERVER_STARTED.register(server -> minecraftServer = server);
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resourceManager) -> reinitialize());
    }
    
    public static String getDefaultConfiguration() {
        return """
                # Create application and with bot account at https://discord.com/developers/applications/
                discord-bot-token=
                
                # Discord chat channel
                discord-channel-id=
                
                # Used to send Minecraft chat messages to the above channel.
                discord-webhook-url=
                """;
    }
    
    public static void proxyMessageToDiscord(Text message, String senderName, URL iconUrl) {
        var webhookUrl = config.get("discord-webhook-url");
        var webhook = discordApi.getIncomingWebhookByUrl(webhookUrl).join();

        webhook.sendMessage(message.getString(), senderName, iconUrl);
    }
    
    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            var reloadCommand = literal("reload")
                    .requires(Permissions.require("dsharpbridge.reload", 4)) // otherwise OP
                    .executes(this::reloadCommand)
                    .build();
            
            var dsharpBridgeCommand = literal("dsharpbridge")
                    .then(reloadCommand)
                    .build();
            
            dispatcher.getRoot().addChild(dsharpBridgeCommand);
        });
        
        LOGGER.info("DSharpBridge loaded ;)");
    }
    
    private int reloadCommand(CommandContext<ServerCommandSource> ctx) {
        reinitialize();
        ctx.getSource().sendFeedback(new LiteralText("DSharpBridge reload complete"), false);
        return 1;
    }
    
    private void initializeConfig() throws RuntimeException {
        config = SimpleConfig.of("dsharpbridge")
                .provider(fileName -> DSharpBridge.getDefaultConfiguration())
                .request();
        
        if (config.get("discord-bot-token") == null)
            throw new ConfigurationFieldMissingError("discord-bot-token");
        else if (config.get("discord-channel-id") == null)
            throw new ConfigurationFieldMissingError("discord-channel-id");
        else if (config.get("discord-webhook-url") == null)
            throw new ConfigurationFieldMissingError("discord-webhook-url");
    }
    
    private void initializeDiscord() {
        if (discordApi != null)
            discordApi.disconnect().join();
        
        var token = config.get("discord-bot-token");
        discordApi = new DiscordApiBuilder().setToken(token).login().join();
        
        var channelId = config.getOrDefault("discord-channel-id", 0);
        var channel = discordApi.getServerTextChannelById(channelId).orElseThrow();
        
        channel.addMessageCreateListener(this::proxyMessageToMinecraft);
    }
    
    private void proxyMessageToMinecraft(MessageCreateEvent event) {
        var author = event.getMessage().getAuthor();
        if (author.isBotUser() || author.isWebhook()) return;
        
        var messageText = new LiteralText("");
        
        var userInfo = author.getDiscriminatedName() + " (" + author.getIdAsString() + ")";
        var userInfoText = new LiteralText(userInfo).setStyle(Style.EMPTY.withItalic(true));
        var userHoverEvent = HoverEvent.Action.SHOW_TEXT.buildHoverEvent(userInfoText);
        var userStyle = Style.EMPTY.withHoverEvent(userHoverEvent);
        
        if (author.getRoleColor().isPresent())
            userStyle.withColor(author.getRoleColor().get().getRGB());

        var replyText = new LiteralText("");
        var optionalMessageReference = event.getMessage().getMessageReference();
        if(optionalMessageReference.isPresent() && optionalMessageReference.get().getMessage().isPresent()) {
            var replyMessage = optionalMessageReference.get().getMessage().get();
            var replyAuthor = replyMessage.getAuthor();
            var replyInfoText = replyAuthor.getDiscriminatedName() + " (" + replyAuthor.getIdAsString() + ")\n\n" + replyMessage.getReadableContent();
            var replyHoverEvent = HoverEvent.Action.SHOW_TEXT.buildHoverEvent(Text.of(replyInfoText));
            var replyStyle = Style.EMPTY.withHoverEvent(replyHoverEvent).withColor(Formatting.AQUA);
            replyText = (LiteralText) new LiteralText("[↩] ").setStyle(replyStyle); // hope that emoji works
        }
        
        var senderText = new LiteralText(author.getDisplayName()).setStyle(userStyle);
        
        messageText.append(replyText)
                .append(senderText)
                .append("»")
                .append(TextParser.parse(event.getReadableMessageContent())); // TODO: limit parsing to only markdown
        
        minecraftServer.getPlayerManager().broadcast(messageText, MessageType.CHAT, Util.NIL_UUID);
    }
    
    
    private void reinitialize() {
        initializeConfig();
        initializeDiscord();
    }
}
