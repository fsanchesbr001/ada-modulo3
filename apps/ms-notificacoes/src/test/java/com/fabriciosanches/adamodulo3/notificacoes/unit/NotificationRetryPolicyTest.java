package com.fabriciosanches.adamodulo3.notificacoes.unit;

import com.fabriciosanches.adamodulo3.notificacoes.application.NotificationRetryPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationRetryPolicyTest {

    @Test
    void hasExceededMaxAttemptsMustCapAtThree() {
        NotificationRetryPolicy policy = new NotificationRetryPolicy();

        assertFalse(policy.hasExceededMaxAttempts(1));
        assertFalse(policy.hasExceededMaxAttempts(2));
        assertTrue(policy.hasExceededMaxAttempts(3));
        assertTrue(policy.hasExceededMaxAttempts(4));
    }
}
