package com.woojung.jerusalemmix.protocol;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControlSafetyPolicyTest {
    @Test
    public void readOnlyConnectionNeverAllowsWrites() {
        assertFalse(ControlSafetyPolicy.mayWriteVerified(true, false));
        assertFalse(ControlSafetyPolicy.mayWriteExperimental(true, false, true));
    }

    @Test
    public void disconnectedStateNeverAllowsWrites() {
        assertFalse(ControlSafetyPolicy.mayWriteVerified(false, true));
        assertFalse(ControlSafetyPolicy.mayWriteExperimental(false, true, true));
    }

    @Test
    public void experimentalWritesNeedBothSafetyLocks() {
        assertFalse(ControlSafetyPolicy.mayWriteExperimental(true, true, false));
        assertTrue(ControlSafetyPolicy.mayWriteExperimental(true, true, true));
    }
}
