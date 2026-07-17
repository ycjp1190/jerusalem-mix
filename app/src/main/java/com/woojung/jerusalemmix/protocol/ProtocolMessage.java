package com.woojung.jerusalemmix.protocol;

public record ProtocolMessage(
        Kind kind,
        String parameter,
        int x,
        int y,
        String value,
        String raw
) {
    public enum Kind { VALUE, DEVICE_INFO, ERROR, OTHER }

    public int intValue(int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
