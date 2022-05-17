package me.thegiggitybyte.dsharpbridge;

public class ConfigurationFieldMissingError extends Error {
    public ConfigurationFieldMissingError(String fieldName) {
        super("Configuration file is missing field " + fieldName);
    }
}
