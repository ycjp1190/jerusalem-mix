package com.woojung.jerusalemmix.model;

public final class OutputState {
    public final String kind;
    public final int wireIndex;
    public String name;
    public String colorName;
    public int faderHundredthDb = -1200;
    public boolean on = true;

    public OutputState(String kind, int wireIndex, String name, String colorName) {
        this.kind = kind;
        this.wireIndex = wireIndex;
        this.name = name;
        this.colorName = colorName;
    }
}
