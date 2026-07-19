package com.woojung.jerusalemmix.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MixerState {
    public enum Connection { DEMO, CONNECTING, READ_ONLY, CONTROL, ERROR }

    private final List<ChannelState> channels = new ArrayList<>(80);
    private final List<OutputState> outputs = new ArrayList<>(34);
    private final List<OutputState> dcas = new ArrayList<>(16);
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
        String[] mixNames = {
                "Choir", "Center", "Front Fill", "Broadcast", "In-Ear 1", "In-Ear 2", "In-Ear 3", "In-Ear 4",
                "Band Mon 1", "Band Mon 2", "Pastor Mon", "Talkback", "FX Send 1", "FX Send 2", "Record", "Lobby"
        };
        for (int i = 0; i < 24; i++) {
            outputs.add(new OutputState("MIX", i, i < mixNames.length ? mixNames[i] : "Mix " + (i + 1), colors[i % colors.length]));
        }
        String[] matrixNames = {"Main L", "Main R", "Center", "Delay", "Lobby", "Broadcast", "Record L", "Record R"};
        for (int i = 0; i < 8; i++) outputs.add(new OutputState("MT", i, matrixNames[i], colors[(i + 2) % colors.length]));
        outputs.add(new OutputState("MASTER", 0, "Stereo", "Red"));
        outputs.add(new OutputState("MASTER", 1, "Mono", "Orange"));
        for (int i = 0; i < 16; i++) dcas.add(new OutputState("DCA", i, "DCA " + (i + 1), colors[(i + 4) % colors.length]));
    }

    public List<ChannelState> channels() {
        return Collections.unmodifiableList(channels);
    }

    public ChannelState channel(int index) {
        return channels.get(Math.max(0, Math.min(channels.size() - 1, index)));
    }

    public List<OutputState> outputs() { return Collections.unmodifiableList(outputs); }
    public List<OutputState> dcas() { return Collections.unmodifiableList(dcas); }
}
