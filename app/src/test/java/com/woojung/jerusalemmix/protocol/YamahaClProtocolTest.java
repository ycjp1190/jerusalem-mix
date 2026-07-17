package com.woojung.jerusalemmix.protocol;

import com.woojung.jerusalemmix.model.ChannelState;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class YamahaClProtocolTest {
    @Test
    public void createsVerifiedMonoCommandsWithZeroBasedWireIndexes() {
        ChannelState channelOne = new ChannelState(0, 0, false, "CH 1");
        assertEquals("get MIXER:Current/InCh/Fader/Level 0 0",
                YamahaClProtocol.channelGet(channelOne, "Fader/Level"));
        assertEquals("set MIXER:Current/InCh/ToMix/Level 0 23 -1200",
                YamahaClProtocol.sendSet(channelOne, 23, "Level", -1200));
    }

    @Test
    public void createsStereoCommandsUsingStereoWireIndex() {
        ChannelState stereoEight = new ChannelState(79, 14, true, "ST 8");
        assertEquals("set MIXER:Current/StInCh/Fader/On 14 0 1",
                YamahaClProtocol.channelSet(stereoEight, "Fader/On", 1));
    }

    @Test
    public void parsesOkAndNotifyValues() {
        ProtocolMessage ok = YamahaClProtocol.parse(
                "OK get MIXER:Current/InCh/Fader/Level 0 0 -1234");
        assertEquals(ProtocolMessage.Kind.VALUE, ok.kind());
        assertEquals("MIXER:Current/InCh/Fader/Level", ok.parameter());
        assertEquals(-1234, ok.intValue(0));

        ProtocolMessage notify = YamahaClProtocol.parse(
                "NOTIFY MIXER:Current/InCh/Label/Name 4 0 Lead Vox");
        assertEquals(ProtocolMessage.Kind.VALUE, notify.kind());
        assertEquals("Lead Vox", notify.value());
    }

    @Test
    public void clampsFaderRange() {
        assertEquals(-32768, YamahaClProtocol.clampLevel(-90000));
        assertEquals(1000, YamahaClProtocol.clampLevel(1500));
        assertEquals(-600, YamahaClProtocol.clampLevel(-600));
    }
}
