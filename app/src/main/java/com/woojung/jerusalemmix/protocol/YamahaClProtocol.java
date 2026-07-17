package com.woojung.jerusalemmix.protocol;

import com.woojung.jerusalemmix.model.ChannelState;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Yamaha CL/QL text control commands over TCP 49280.
 *
 * Fader/On/ToMix commands are present in Yamaha's official Crestron example.
 * Label, HA and PEQ paths are isolated as experimental CL paths because Yamaha
 * publishes these MMS parameter IDs for newer consoles but not as a CL API.
 */
public final class YamahaClProtocol {
    public static final int DEFAULT_PORT = 49280;
    public static final int MIN_LEVEL = -32768;
    public static final int MAX_LEVEL = 1000;

    private static final Pattern VALUE_LINE = Pattern.compile(
            "^(?:OK|NOTIFY)(?:\\s+get|\\s+set)?\\s+(MIXER:[^\\s]+)\\s+(-?\\d+)\\s+(-?\\d+)(?:\\s+(.*))?$");

    private YamahaClProtocol() {}

    public static String get(String path, int x, int y) {
        return String.format(Locale.US, "get MIXER:Current/%s %d %d", path, x, y);
    }

    public static String set(String path, int x, int y, int value) {
        return String.format(Locale.US, "set MIXER:Current/%s %d %d %d", path, x, y, value);
    }

    public static String setString(String path, int x, int y, String value) {
        String safe = value.replace('\r', ' ').replace('\n', ' ').trim();
        return String.format(Locale.US, "set MIXER:Current/%s %d %d %s", path, x, y, safe);
    }

    public static String channelGet(ChannelState ch, String suffix) {
        return get(ch.protocolRoot() + "/" + suffix, ch.wireIndex, 0);
    }

    public static String channelSet(ChannelState ch, String suffix, int value) {
        return set(ch.protocolRoot() + "/" + suffix, ch.wireIndex, 0, value);
    }

    public static String sendGet(ChannelState ch, int mixZeroBased, String suffix) {
        return get(ch.protocolRoot() + "/ToMix/" + suffix, ch.wireIndex, mixZeroBased);
    }

    public static String sendSet(ChannelState ch, int mixZeroBased, String suffix, int value) {
        return set(ch.protocolRoot() + "/ToMix/" + suffix, ch.wireIndex, mixZeroBased, value);
    }

    public static ProtocolMessage parse(String line) {
        if (line == null) return new ProtocolMessage(ProtocolMessage.Kind.OTHER, "", 0, 0, "", "");
        String trimmed = line.trim();
        if (trimmed.startsWith("ERROR")) {
            return new ProtocolMessage(ProtocolMessage.Kind.ERROR, "", 0, 0, trimmed, trimmed);
        }
        Matcher matcher = VALUE_LINE.matcher(trimmed);
        if (matcher.matches()) {
            return new ProtocolMessage(
                    ProtocolMessage.Kind.VALUE,
                    matcher.group(1),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    matcher.group(4) == null ? "" : matcher.group(4),
                    trimmed
            );
        }
        if (trimmed.startsWith("OK devinfo") || trimmed.startsWith("OK devstatus")) {
            return new ProtocolMessage(ProtocolMessage.Kind.DEVICE_INFO, "", 0, 0, trimmed, trimmed);
        }
        return new ProtocolMessage(ProtocolMessage.Kind.OTHER, "", 0, 0, trimmed, trimmed);
    }

    public static int clampLevel(int value) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, value));
    }
}
