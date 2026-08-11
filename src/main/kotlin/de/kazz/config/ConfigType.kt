package de.kazz.config

/**
 * Enum of supported config value types.
 * Used by the serializer to discriminate type during deserialization.
 */
enum class ConfigType {
    BOOLEAN,
    INT,
    DOUBLE,
    STRING,
    ENUM,
    COLOR,
    ACTION_BUTTON
}