package com.woojung.jerusalemmix.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MixerState {
    public enum Connection { DEMO, CONNECTING, READ_ONLY, CONTROL, ERROR }

    private final List<ChannelState> channels = new ArrayList<>(80);
    public volatile Connection connection = Connection.DEMO;
    public volatile String connectionDetail = "OFFLINE DEMO";
    public volatile int bank = 0;
    public volatile int selectedChannel = 0;
    public volatile int selectedMix = 0;
    public volatile boolean sendsOnFader;

    public MixerState() {
        String[] demoNames = {
                "KICK", "SNARE", "HAT", "TOM 1", "TOM 2", "BASS", "E.GTR", "A.GTR",
                "KEY L", "KEY R", "LEAD", "VOCAL1", "VOCAL2", "VOCAL3", "PASTOR", "MC"
        };
        String[] colors = {"Red", "Yellow", "Green", "SkyBlue", "Blue", "Purple", "Pink", "Orange"};
        for (int i = 0; i < 72; i++) {
            ChannelState ch = new ChannelState(i, i, false,
                    i < demoNames.length ? demoNames[i] : "CH " + (i + 1));
            ch.colorName = colors[i % colors.length];
            ch.faderHundredthDb = i < 16 ? -600 - (i % 4) * 250 : -1200;
            channels.add(ch);
        }
        for (int i = 0; i < 8; i++) {
            ChannelState ch = new ChannelState(72 + i, i * 2, true, "ST " + (i + 1));
            ch.colorName = colors[(i + 3) % colors.length];
            channels.add(ch);
        }
    }

    public List<ChannelState> channels() {
        return Collections.unmodifiableList(channels);
    }

    public ChannelState channel(int index) {
        return channels.get(Math.max(0, Math.min(channels.size() - 1, index)));
    }
}
