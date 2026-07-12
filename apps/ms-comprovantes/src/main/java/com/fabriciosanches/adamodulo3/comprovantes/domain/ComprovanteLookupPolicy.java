package com.fabriciosanches.adamodulo3.comprovantes.domain;

public class ComprovanteLookupPolicy {

    public static final int MAX_ATTEMPTS = 3;

    public boolean shouldRetry(int attemptCount) {
        return attemptCount < MAX_ATTEMPTS;
    }
}
