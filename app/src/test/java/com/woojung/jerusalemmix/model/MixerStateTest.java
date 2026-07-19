package com.woojung.jerusalemmix.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MixerStateTest {
    @Test
    public void exposesCl5InputOutputAndDcaCapacity() {
        MixerState state = new MixerState();
        assertEquals(80, state.channels().size());
        assertEquals(34, state.outputs().size());
        assertEquals(16, state.dcas().size());
        assertEquals(32, state.channel(0).mixSendHundredthDb.length);
        assertEquals("MIX", state.outputs().get(0).kind);
        assertEquals("MT", state.outputs().get(24).kind);
        assertEquals("DCA", state.dcas().get(0).kind);
    }

    @Test
    public void defaultsAllMixAndMatrixSendsToPre() {
        ChannelState channel = new MixerState().channel(0);
        for (boolean pre : channel.mixSendPre) assertTrue(pre);
    }
}
