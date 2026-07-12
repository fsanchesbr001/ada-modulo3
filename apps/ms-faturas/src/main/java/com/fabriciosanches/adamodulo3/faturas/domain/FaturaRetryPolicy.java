package com.fabriciosanches.adamodulo3.faturas.domain;

public class FaturaRetryPolicy {

    public static final int MAX_RETRY_ATTEMPTS = 3;

    public int nextAttempt(int currentRetryCount) {
        if (currentRetryCount < 0) {
            throw new IllegalArgumentException("currentRetryCount must be >= 0");
        }
        if (currentRetryCount >= MAX_RETRY_ATTEMPTS) {
            return MAX_RETRY_ATTEMPTS;
        }
        return currentRetryCount + 1;
    }

    public boolean hasRetrySlot(int retryCount) {
        return retryCount < MAX_RETRY_ATTEMPTS;
    }

    public boolean isCeilingReached(int retryCount) {
        return retryCount >= MAX_RETRY_ATTEMPTS;
    }

    public FaturaStatus nextStatusAfterRefusal(int nextRetryCount) {
        return isCeilingReached(nextRetryCount) ? FaturaStatus.PROBLEMA : FaturaStatus.SOLICITADO;
    }
}
