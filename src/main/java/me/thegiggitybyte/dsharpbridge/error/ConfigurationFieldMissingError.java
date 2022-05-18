package me.thegiggitybyte.dsharpbridge.error;

public class ConfigurationFieldMissingError extends Error {
    public ConfigurationFieldMissingError(String fieldName) {
        super("Configuration file is missing field " + fieldName);
    }
}
