package me.thegiggitybyte.wumpuslink;

import com.google.gson.JsonPrimitive;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.thegiggitybyte.wumpuslink.config.JsonConfiguration;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URL;
import java.util.UUID;

public class WumpusLink implements DedicatedServerModInitializer {

    static { // Our initialization needs to be completed before the Fabric loader starts its own initialization process.
        WumpusLink.initialize();

        boolean canSendStatusMessages = JsonConfiguration.getUserInstance().getValue("minecraft-server-status-messages").getAsBoolean();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (canSendStatusMessages) {
                var embed = new EmbedBuilder()
                        .setTitle("Server Starting")
                        .setDescription("Loading worlds...")
                        .setColor(Color.ORANGE);

                MessageProxy.sendServerMessageToDiscord(embed);
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (canSendStatusMessages) {
                var modCount = FabricLoader.getInstance().getAllMods().stream()
                        .filter(mod -> {
                            var modId = mod.getMetadata().getId();

                            return !modId.equals("java") &&
                                    !modId.equals("minecraft") &&
                                    !modId.equals("fabricloader");
                        })
                        .filter(mod -> mod.getContainingMod().isEmpty()) // Exclude nested mods (JiJ)
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
            if (canSendStatusMessages) {
                var embed = new EmbedBuilder()
                        .setTitle("Server Stopping")
                        .setDescription("Unloading players and worlds...")
                        .setColor(Color.ORANGE);

                MessageProxy.sendServerMessageToDiscord(embed);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (canSendStatusMessages) {
                var embed = new EmbedBuilder()
                        .setTitle("Server Offline")
                        .setColor(Color.RED);

                MessageProxy.sendServerMessageToDiscord(embed).join();
            }

            MessageProxy.disconnectFromDiscord();
        });

        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resourceManager) -> JsonConfiguration.getUserInstance());
    }

    @Override
    public void onInitializeServer() {
        JsonConfiguration.getUserInstance();
        Commands.register();

        LoggerFactory.getLogger("WumpusLink").info("Loaded successfully :D");
    }



    static void initialize() {
        JsonConfiguration.getUserInstance();
        MessageProxy.connectToDiscord();
    }


}
