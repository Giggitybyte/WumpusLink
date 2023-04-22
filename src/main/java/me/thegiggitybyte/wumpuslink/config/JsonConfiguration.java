package me.thegiggitybyte.wumpuslink.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class JsonConfiguration {
    private static final JsonConfiguration DEFAULT_INSTANCE;
    private static final JsonConfiguration USER_INSTANCE;
    private JsonObject jsonObject;
    private Path filePath;

    static {
        var defaultJson = new JsonObject();

        defaultJson.addProperty("discord-bot-token", "");
        defaultJson.addProperty("discord-channel-id", "");
        defaultJson.addProperty("discord-webhook-url", "");
        defaultJson.addProperty("minecraft-server-status-messages", true);
        defaultJson.addProperty("minecraft-player-death-messages", true);
        defaultJson.addProperty("minecraft-join-leave-messages", true);
        defaultJson.addProperty("minecraft-chat-messages", true);
        defaultJson.addProperty("minecraft-advancement-messages", true);

        DEFAULT_INSTANCE = new JsonConfiguration();
        DEFAULT_INSTANCE.jsonObject = defaultJson;

        var environment = FabricLoader.getInstance()
                .getEnvironmentType()
                .toString()
                .toLowerCase();

        var filePath = FabricLoader.getInstance()
                .getConfigDir()
                .normalize()
                .toAbsolutePath()
                .resolveSibling("config/wumpuslink-" + environment + ".json");

        USER_INSTANCE = new JsonConfiguration();
        USER_INSTANCE.filePath = filePath;

        try {
            if (Files.exists(filePath)) {
                var jsonString = Files.readString(filePath);
                if (!jsonString.isEmpty()) {
                    USER_INSTANCE.jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
                }
            } else {
                USER_INSTANCE.jsonObject = new JsonObject();
            }

            USER_INSTANCE.validateJsonStructure();
            USER_INSTANCE.writePendingChanges();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonConfiguration getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static JsonConfiguration getUserInstance() {
        return USER_INSTANCE;
    }

    public JsonPrimitive getValue(String key) {
        var jsonValue = jsonObject.get(key);

        if (jsonValue != null)
            return jsonValue.getAsJsonPrimitive();
        else
            throw new AssertionError("Key does not exist");
    }

    public Object getOrDefaultValue(String key, boolean def) {
        var jsonValue = jsonObject.get(key);

        if (jsonValue != null)
            return jsonValue.getAsJsonPrimitive();
        else
            return def;
    }

    public Set<String> getKeys() {
        return jsonObject.keySet();
    }

    public boolean hasKey(String key) {
        return jsonObject.has(key);
    }

    public void setValue(String key, Number value) {
        setValue(key, new JsonPrimitive(value));
    }

    public void setValue(String key, String value) {
        setValue(key, new JsonPrimitive(value));
    }

    public void setValue(String key, boolean value) {
        setValue(key, new JsonPrimitive(value));
    }

    private void setValue(String key, JsonPrimitive value) {
        jsonObject.add(key, value);
    }

    private void validateJsonStructure() {
        for (var entry : DEFAULT_INSTANCE.jsonObject.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();

            if (!this.jsonObject.has(key) || !this.jsonObject.get(key).isJsonPrimitive()) {
                jsonObject.add(key, value);
            }
        }
    }

    public void writePendingChanges() {
        if (filePath == null) throw new AssertionError("Configuration file path null.");

        try (var fileStream = Files.newOutputStream(filePath)) {
            var stringWriter = new StringWriter();
            var jsonWriter = new JsonWriter(stringWriter);

            jsonWriter.setLenient(true);
            jsonWriter.setIndent("  ");

            validateJsonStructure();

            Streams.write(jsonObject, jsonWriter);
            fileStream.write(stringWriter.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}