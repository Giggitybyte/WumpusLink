package me.thegiggitybyte.wumpuslink;

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.thegiggitybyte.wumpuslink.error.ConfigurationFieldMissingError;
import me.thegiggitybyte.wumpuslink.error.ConfigurationValueEmptyError;
import net.darktree.simpleconfig.SimpleConfig;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.LiteralText;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URL;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class WumpusLink implements DedicatedServerModInitializer {
    private static final String[] REQUIRED_CONFIG_KEYS;
    private static SimpleConfig config;
    
    static {
        REQUIRED_CONFIG_KEYS = new String[]{
                "discord-bot-token",
                "discord-channel-id",
                "discord-webhook-url"
        };
    
        WumpusLink.initialize();
        
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            var canSendStartingMessage = WumpusLink.getConfig().getOrDefault("minecraft-server-status-messages", true);
            if (canSendStartingMessage) {
                var embed = new EmbedBuilder()
                        .setTitle("Server Starting")
                        .setDescription("Loading worlds...")
                        .setColor(Color.ORANGE);
    
                MessageProxy.sendServerMessageToDiscord(embed);
            }
        });
    
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var canSendOnlineMessage = WumpusLink.getConfig().getOrDefault("minecraft-server-status-messages", true);
            if (canSendOnlineMessage) {
                var modCount = FabricLoader.getInstance().getAllMods().stream()
                        .filter(mod -> {
                            var modId = mod.getMetadata().getId();
                            
                            return !modId.equals("java") &&
                            !modId.equals("minecraft") &&
                            !modId.equals("fabricloader");
                        })
                        .filter(mod -> mod.getContainingMod().isEmpty()) // Top level
                        .count(); // TODO: filter out library mods
                
                var embed = new EmbedBuilder()
                        .setTitle("Server Online")
                        .setDescription("Ready for players")
                        .setFooter(modCount + " mods loaded")
                        .setColor(Color.GREEN);
    
                MessageProxy.sendServerMessageToDiscord(embed);
            }
        });
    
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            var canSendStoppingMessage = WumpusLink.getConfig().getOrDefault("minecraft-server-status-messages", true);
            if (canSendStoppingMessage) {
                var embed = new EmbedBuilder()
                        .setTitle("Server Stopping")
                        .setDescription("Unloading players and worlds...")
                        .setColor(Color.ORANGE);
            
                MessageProxy.sendServerMessageToDiscord(embed);
            }
        });
    
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            var canSendOfflineMessage = WumpusLink.getConfig().getOrDefault("minecraft-server-status-messages", true);
            if (canSendOfflineMessage) {
                var embed = new EmbedBuilder()
                        .setTitle("Server Offline")
                        .setColor(Color.RED);
    
                MessageProxy.sendServerMessageToDiscord(embed).join();
            }
        
            MessageProxy.disconnectFromDiscord();
        });
        
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resourceManager) -> initializeConfig());
    }
    
    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            var configCommand = literal("config")
                    .requires(Permissions.require("wumpuslink.reload.config", 4))
                    .executes(ctx -> {
                        initializeConfig();
                        ctx.getSource().sendFeedback(new LiteralText("WumpusLink configuration reload complete"), false);
                        return 1;
                    });
            
            var discordCommand = literal("discord")
                    .requires(Permissions.require("wumpuslink.reload.discord", 4))
                    .executes(ctx -> {
                        MessageProxy.connectToDiscord();
                        ctx.getSource().sendFeedback(new LiteralText("Discord client reload complete"), false);
                        return 1;
                    });
            
            var reloadCommand = literal("reload")
                    .requires(Permissions.require("wumpuslink.reload", 4)) // otherwise OP
                    .executes(ctx -> {
                        WumpusLink.initialize();
                        ctx.getSource().sendFeedback(new LiteralText("WumpusLink reload complete"), false);
                        return 1;
                    })
                    .then(configCommand)
                    .then(discordCommand)
                    .build();
            
            var wumpusLinkCommand = literal("wumpuslink")
                    .then(reloadCommand)
                    .build();
            
            dispatcher.getRoot().addChild(wumpusLinkCommand);
        });
        
        LoggerFactory.getLogger("WumpusLink").info("Loaded successfully :D");
    }
    
    public static URL getMinecraftPlayerHeadUrl(UUID playerUuid) {
        return createUrl("https://crafatar.com/renders/head/" + playerUuid + "?default=mhf_Steve&overlay");
    }
    
    public static String getMinecraftPlayerRender(UUID playerUuid) {
        return "https://crafatar.com/renders/body/" + playerUuid;
    }
    
    public static SimpleConfig getConfig() {
        return config;
    }
    
    static void initialize() {
        WumpusLink.initializeConfig();
        MessageProxy.connectToDiscord();
    }
    
    private static void initializeConfig() throws RuntimeException {
        config = SimpleConfig.of("wumpuslink")
                .provider(fileName -> getDefaultConfig())
                .request();
        
        for (var key : REQUIRED_CONFIG_KEYS) {
            if (config.get(key) == null)
                throw new ConfigurationFieldMissingError(key);
            else if (config.get(key).trim().length() == 0)
                throw new ConfigurationValueEmptyError(key);
        }
    }
    
    private static String getDefaultConfig() {
        return """
                # Create application and with bot account at https://discord.com/developers/applications/
                discord-bot-token=
                
                # Desired Discord channel to proxy messages to and from the Minecraft server.
                # Bot account will require permissions to send messages and embeds in this channel.
                discord-channel-id=
                
                # Discord webhook linked to the channel above.
                # Used to display Minecraft player skins and usernames in relayed Discord chat messages.
                discord-webhook-url=
                
                # Whether to send Minecraft server status messages to the Discord channel.
                minecraft-server-status-messages=true
                
                # Whether to send Minecraft player death messages to the Discord channel.
                minecraft-player-death-messages=true
                
                # Whether to send Minecraft player join and leave messages to the Discord channel.
                minecraft-join-leave-messages=true
                
                # Whether to relay Minecraft player chat messages to the Discord channel.
                minecraft-chat-messages=true
                
                # Whether to send Minecraft player advancement messages to the Discord channel.
                minecraft-advancement-messages=true
                """;
    }
    
    static URL createUrl(String string) {
        try {
            return new URL(string); // Checked exceptions are for clowns.
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
