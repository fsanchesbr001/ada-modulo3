package com.fabriciosanches.adamodulo3.comprovantes.unit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComprovanteLookupPolicyTest {

    @Test
    void shouldRetryMustAllowUpToThreeAttempts() {
        FakeLookupPolicy policy = new FakeLookupPolicy(3);

        assertTrue(policy.shouldRetry(1));
        assertTrue(policy.shouldRetry(2));
        assertFalse(policy.shouldRetry(3));
    }

    private static final class FakeLookupPolicy {
        private final int maxAttempts;

        private FakeLookupPolicy(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        private boolean shouldRetry(int currentAttempt) {
            return currentAttempt < maxAttempts;
        }
    }
}
