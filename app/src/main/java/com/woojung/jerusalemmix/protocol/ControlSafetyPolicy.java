package com.woojung.jerusalemmix.protocol;

public final class ControlSafetyPolicy {
    private ControlSafetyPolicy() {}

    public static boolean mayWriteVerified(boolean connected, boolean controlEnabled) {
        return connected && controlEnabled;
    }

    public static boolean mayWriteExperimental(boolean connected, boolean controlEnabled, boolean experimentalEnabled) {
        return connected && controlEnabled && experimentalEnabled;
    }
}
