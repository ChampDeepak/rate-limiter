package src.main.java.com.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sliding Window Counter rate limiting algorithm.
 * 
 * Uses a more granular approach that slides the time window continuously.
 * Provides better burst control than fixed window at the cost of more memory.
 * 
 * Trade-offs:
 * - Pros: Smoother rate limiting, better burst control than fixed window
 * - Cons: More complex, uses more memory
 * 
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class SlidingWindowCounter implements IRateLimiter {
    
    private final RateLimitConfig config;
    private final ConcurrentMap<String, SlidingWindow> windows;
    
    public SlidingWindowCounter(RateLimitConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("RateLimitConfig cannot be null");
        }
        this.config = config;
        this.windows = new ConcurrentHashMap<>();
    }
    
    /**
     * Convenience constructor with simple parameters.
     * @param maxRequests maximum requests allowed in the window
     * @param windowSize size of the window
     * @param timeUnit time unit for the window
     */
    public SlidingWindowCounter(long maxRequests, long windowSize, TimeUnit timeUnit) {
        this(new RateLimitConfig(maxRequests, windowSize, timeUnit));
    }
    
    @Override
    public boolean validateRequest(RequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("RequestDto cannot be null");
        }
        
        String key = requestDto.getRateLimitKey();
        long currentTime = System.currentTimeMillis();
        long windowSizeInMillis = config.getWindowSizeInMillis();
        
        // Atomically check and add request to avoid TOCTOU race condition
        boolean[] allowed = new boolean[1];
        SlidingWindow slidingWindow = windows.compute(key, (k, existing) -> {
            if (existing == null) {
                SlidingWindow newWindow = new SlidingWindow(windowSizeInMillis, currentTime);
                newWindow.addRequest(currentTime);
                allowed[0] = true;
                return newWindow;
            }
            existing.cleanup(currentTime);
            long count = existing.getCount(currentTime);
            if (count < config.getMaxRequests()) {
                existing.addRequest(currentTime);
                allowed[0] = true;
            } else {
                allowed[0] = false;
            }
            return existing;
        });
        
        return allowed[0];
    }
    
    @Override
    public String getAlgorithmName() {
        return "SlidingWindowCounter";
    }
    
    /**
     * Gets current count for a key (useful for monitoring).
     * @param requestDto the request
     * @return current count within the sliding window
     */
    public long getCurrentCount(RequestDto requestDto) {
        String key = requestDto.getRateLimitKey();
        SlidingWindow window = windows.get(key);
        if (window == null) {
            return 0;
        }
        return window.getCount(System.currentTimeMillis());
    }
    
    /**
     * Internal class to manage sliding window state.
     * Uses a simple approach: tracks request count and window start time.
     */
    private static class SlidingWindow {
        private final long windowSizeInMillis;
        private final ConcurrentMap<Long, AtomicLong> requestCounts;
        private volatile long lastCleanupTime;
        
        SlidingWindow(long windowSizeInMillis, long startTime) {
            this.windowSizeInMillis = windowSizeInMillis;
            this.requestCounts = new ConcurrentHashMap<>();
            this.lastCleanupTime = startTime;
        }
        
        void cleanup(long currentTime) {
            // Only cleanup every windowSize to reduce overhead
            if (currentTime - lastCleanupTime > windowSizeInMillis) {
                long windowStart = (currentTime / windowSizeInMillis) * windowSizeInMillis;
                long cutoffTime = windowStart - windowSizeInMillis;
                
                requestCounts.keySet().removeIf(time -> time < cutoffTime);
                lastCleanupTime = currentTime;
            }
        }
        
        long getCount(long currentTime) {
            long windowStart = (currentTime / windowSizeInMillis) * windowSizeInMillis;
            long windowEnd = windowStart + windowSizeInMillis;
            
            long totalCount = 0;
            for (java.util.Map.Entry<Long, AtomicLong> entry : requestCounts.entrySet()) {
                if (entry.getKey() >= windowStart && entry.getKey() < windowEnd) {
                    totalCount += entry.getValue().get();
                }
            }
            return totalCount;
        }
        
        void addRequest(long timestamp) {
            // Round down to window granularity
            long bucket = (timestamp / windowSizeInMillis) * windowSizeInMillis;
            requestCounts.computeIfAbsent(bucket, k -> new AtomicLong(0)).incrementAndGet();
        }
    }
}