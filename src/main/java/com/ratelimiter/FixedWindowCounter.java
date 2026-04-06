package com.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class FixedWindowCounter implements IRateLimiter {
    
    private final RateLimitConfig config;
    private final ConcurrentMap<String, WindowCounter> counters;
    
    public FixedWindowCounter(RateLimitConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("RateLimitConfig cannot be null");
        }
        this.config = config;
        this.counters = new ConcurrentHashMap<>();
    }
    
    /**
     * Convenience constructor with simple parameters.
     * @param maxRequests maximum requests allowed in the window
     * @param windowSize size of the window
     * @param timeUnit time unit for the window
     */
    public FixedWindowCounter(long maxRequests, long windowSize, TimeUnit timeUnit) {
        this(new RateLimitConfig(maxRequests, windowSize, timeUnit));
    }
    
    @Override
    public boolean validateRequest(RequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("RequestDto cannot be null");
        }
        
        String key = requestDto.getRateLimitKey();
        long currentTime = System.currentTimeMillis();
        long windowStart = getWindowStart(currentTime);
        
        // Atomically update the counter
        WindowCounter windowCounter = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStart != windowStart) {
                // New window or window has reset
                return new WindowCounter(windowStart, 1);
            } else {
                // Increment existing counter atomically
                existing.incrementAndGet();
                return existing;
            }
        });
        
        return windowCounter.getCount() <= config.getMaxRequests();
    }
    
    private long getWindowStart(long timestamp) {
        long windowSizeInMillis = config.getWindowSizeInMillis();
        return (timestamp / windowSizeInMillis) * windowSizeInMillis;
    }
    
    @Override
    public String getAlgorithmName() {
        return "FixedWindowCounter";
    }
    
    /**
     * Gets current count for a key (useful for monitoring).
     * @param requestDto the request
     * @return current count or -1 if key not found
     */
    public long getCurrentCount(RequestDto requestDto) {
        String key = requestDto.getRateLimitKey();
        WindowCounter counter = counters.get(key);
        if (counter == null) {
            return 0;
        }
        
        // Check if window has expired
        long currentTime = System.currentTimeMillis();
        long windowStart = getWindowStart(currentTime);
        if (counter.windowStart != windowStart) {
            return 0;
        }
        return counter.getCount();
    }    /**
    * Internal class to hold window counter state.
    * Uses AtomicLong for thread-safe count operations.
    */
    private static class WindowCounter {
        private final long windowStart;
        private final AtomicLong count;
        
        WindowCounter(long windowStart, long initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicLong(initialCount);
        }
        
        long getCount() {
            return count.get();
        }
        
        long incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}