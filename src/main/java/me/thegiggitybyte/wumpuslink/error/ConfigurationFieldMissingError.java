package me.thegiggitybyte.wumpuslink.error;

public class ConfigurationFieldMissingError extends Error {
    public ConfigurationFieldMissingError(String fieldName) {
        super("Configuration file is missing required field " + fieldName);
    }
}
