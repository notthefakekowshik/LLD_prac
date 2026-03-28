package com.lldprep.ratelimiter.exception;

/**
 * Exception thrown when rate limit is exceeded.
 * 
 * This is a checked exception to force callers to handle rate limiting explicitly.
 * Alternative: Could be unchecked if you prefer fail-fast behavior.
 */
public class RateLimitExceededException extends Exception {
    
    private final long retryAfterMillis;
    
    public RateLimitExceededException(String message) {
        super(message);
        this.retryAfterMillis = 0;
    }
    
    public RateLimitExceededException(String message, long retryAfterMillis) {
        super(message);
        this.retryAfterMillis = retryAfterMillis;
    }
    
    /**
     * Returns the suggested retry delay in milliseconds.
     * 
     * Useful for implementing exponential backoff or informing clients
     * when they can retry (HTTP 429 Retry-After header).
     * 
     * @return Milliseconds to wait before retrying (0 if not specified)
     */
    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
