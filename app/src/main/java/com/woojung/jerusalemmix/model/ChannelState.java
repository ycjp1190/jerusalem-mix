package com.woojung.jerusalemmix.model;

import java.util.Arrays;

public final class ChannelState {
    public final int logicalIndex;
    public final int wireIndex;
    public final boolean stereo;
    public String name;
    public String colorName = "Blue";
    public int faderHundredthDb = -1200;
    public boolean channelOn = true;
    public int gainHundredthDb = 0;
    public boolean phantom48;
    public int pan = 0;
    public boolean peqOn = true;
    public final EqBand[] eqBands = {
            new EqBand(800, 0, 700),
            new EqBand(4000, 0, 1000),
            new EqBand(25000, 0, 1000),
            new EqBand(100000, 0, 700)
    };
    public final int[] mixSendHundredthDb = new int[24];
    public final boolean[] mixSendOn = new boolean[24];

    public ChannelState(int logicalIndex, int wireIndex, boolean stereo, String name) {
        this.logicalIndex = logicalIndex;
        this.wireIndex = wireIndex;
        this.stereo = stereo;
        this.name = name;
        Arrays.fill(mixSendHundredthDb, -32768);
    }

    public String protocolRoot() {
        return stereo ? "StInCh" : "InCh";
    }
}
