package com.fabriciosanches.adamodulo3.faturas.unit;

import com.fabriciosanches.adamodulo3.faturas.domain.FaturaRetryPolicy;
import com.fabriciosanches.adamodulo3.faturas.domain.FaturaStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaturaRetryPolicyTest {

    private final FaturaRetryPolicy policy = new FaturaRetryPolicy();

    @Test
    void nextAttemptMustIncrementWhenBelowCeiling() {
        assertEquals(1, policy.nextAttempt(0));
        assertEquals(2, policy.nextAttempt(1));
    }

    @Test
    void nextAttemptMustClampWhenAtOrAboveCeiling() {
        assertEquals(3, policy.nextAttempt(3));
        assertEquals(3, policy.nextAttempt(4));
    }

    @Test
    void nextAttemptMustRejectNegativeRetryCount() {
        assertThrows(IllegalArgumentException.class, () -> policy.nextAttempt(-1));
    }

    @Test
    void hasRetrySlotMustRespectMaxAttempts() {
        assertTrue(policy.hasRetrySlot(0));
        assertTrue(policy.hasRetrySlot(2));
        assertFalse(policy.hasRetrySlot(3));
    }

    @Test
    void nextStatusAfterRefusalMustBeSolicitadoBeforeCeilingAndProblemaAtCeiling() {
        assertEquals(FaturaStatus.SOLICITADO, policy.nextStatusAfterRefusal(1));
        assertEquals(FaturaStatus.SOLICITADO, policy.nextStatusAfterRefusal(2));
        assertEquals(FaturaStatus.PROBLEMA, policy.nextStatusAfterRefusal(3));
    }
}
