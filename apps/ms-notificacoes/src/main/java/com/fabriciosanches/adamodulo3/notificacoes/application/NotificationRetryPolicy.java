package com.fabriciosanches.adamodulo3.notificacoes.application;

public class NotificationRetryPolicy {

    public static final int MAX_ATTEMPTS = 3;

    public boolean hasExceededMaxAttempts(int attempts) {
        return attempts >= MAX_ATTEMPTS;
    }
}
