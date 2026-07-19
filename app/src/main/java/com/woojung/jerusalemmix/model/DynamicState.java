package com.woojung.jerusalemmix.model;

public final class DynamicState {
    public boolean on;
    public String type;
    public int thresholdHundredthDb = -2600;
    public int ratioHundredths = 200;
    public int rangeHundredthDb = -5600;
    public int attackTenthsMs = 300;
    public int holdTenthsMs = 20;
    public int releaseTenthsMs = 3040;
    public int outGainHundredthDb;
    public int knee;

    public DynamicState(String type) {
        this.type = type;
    }
}
