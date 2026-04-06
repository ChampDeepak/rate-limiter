package src.main.java.com.ratelimiter;

import java.util.concurrent.TimeUnit;

/**
 * Configuration class for rate limiting.
 * Supports configuring limits like 100 requests per minute, 1000 requests per hour.
 */
public class RateLimitConfig {
    private final long maxRequests;
    private final long windowSize;
    private final TimeUnit timeUnit;
    private final String keyPrefix;

    public RateLimitConfig(long maxRequests, long windowSize, TimeUnit timeUnit) {
        this(maxRequests, windowSize, timeUnit, "default");
    }

    public RateLimitConfig(long maxRequests, long windowSize, TimeUnit timeUnit, String keyPrefix) {
        if (maxRequests <= 0 || windowSize <= 0) {
            throw new IllegalArgumentException("maxRequests and windowSize must be positive");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("timeUnit cannot be null");
        }
        this.maxRequests = maxRequests;
        this.windowSize = windowSize;
        this.timeUnit = timeUnit;
        this.keyPrefix = keyPrefix;
    }

    public long getMaxRequests() {
        return maxRequests;
    }

    public long getWindowSize() {
        return windowSize;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public long getWindowSizeInMillis() {
        return timeUnit.toMillis(windowSize);
    }

    @Override
    public String toString() {
        return "RateLimitConfig{maxRequests=" + maxRequests + ", windowSize=" + windowSize + 
               ", timeUnit=" + timeUnit + ", keyPrefix='" + keyPrefix + "'}";
    }
}