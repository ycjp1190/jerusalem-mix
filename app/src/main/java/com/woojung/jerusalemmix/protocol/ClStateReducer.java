package com.woojung.jerusalemmix.protocol;

import com.woojung.jerusalemmix.model.ChannelState;
import com.woojung.jerusalemmix.model.MixerState;

public final class ClStateReducer {
    private final MixerState state;

    public ClStateReducer(MixerState state) {
        this.state = state;
    }

    public boolean apply(ProtocolMessage message) {
        if (message.kind() != ProtocolMessage.Kind.VALUE) return false;
        String path = message.parameter();
        ChannelState channel = findChannel(path, message.x());
        if (channel == null) return false;

        if (path.endsWith("/Fader/Level")) channel.faderHundredthDb = message.intValue(channel.faderHundredthDb);
        else if (path.endsWith("/Fader/On")) channel.channelOn = message.intValue(channel.channelOn ? 1 : 0) != 0;
        else if (path.endsWith("/Label/Name")) channel.name = cleanString(message.value());
        else if (path.endsWith("/Label/Color")) channel.colorName = cleanString(message.value());
        else if (path.endsWith("/Port/HA/Gain")) channel.gainHundredthDb = message.intValue(channel.gainHundredthDb);
        else if (path.endsWith("/Port/HA/Phantom")) channel.phantom48 = message.intValue(channel.phantom48 ? 1 : 0) != 0;
        else if (path.endsWith("/ToSt/Pan")) channel.pan = message.intValue(channel.pan);
        else if (path.endsWith("/PEQ/On")) channel.peqOn = message.intValue(channel.peqOn ? 1 : 0) != 0;
        else if (path.contains("/PEQ/Band/")) return applyEq(channel, path, message.y(), message);
        else if (path.endsWith("/ToMix/Level") && validMix(message.y())) channel.mixSendHundredthDb[message.y()] = message.intValue(channel.mixSendHundredthDb[message.y()]);
        else if (path.endsWith("/ToMix/On") && validMix(message.y())) channel.mixSendOn[message.y()] = message.intValue(channel.mixSendOn[message.y()] ? 1 : 0) != 0;
        else return false;
        return true;
    }

    private boolean applyEq(ChannelState channel, String path, int band, ProtocolMessage message) {
        if (band < 0 || band >= channel.eqBands.length) return false;
        if (path.endsWith("/Freq")) channel.eqBands[band].frequencyTenthHz = message.intValue(channel.eqBands[band].frequencyTenthHz);
        else if (path.endsWith("/Gain")) channel.eqBands[band].gainHundredthDb = message.intValue(channel.eqBands[band].gainHundredthDb);
        else if (path.endsWith("/Q")) channel.eqBands[band].qThousandths = message.intValue(channel.eqBands[band].qThousandths);
        else if (path.endsWith("/Bypass")) channel.eqBands[band].bypassed = message.intValue(channel.eqBands[band].bypassed ? 1 : 0) != 0;
        else return false;
        return true;
    }

    private ChannelState findChannel(String path, int wireIndex) {
        boolean stereo = path.contains("/StInCh/");
        for (ChannelState channel : state.channels()) {
            if (channel.stereo == stereo && channel.wireIndex == wireIndex) return channel;
        }
        return null;
    }

    private static String cleanString(String value) {
        String result = value.trim();
        if (result.length() >= 2 && result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }

    private static boolean validMix(int mix) {
        return mix >= 0 && mix < 24;
    }
}
