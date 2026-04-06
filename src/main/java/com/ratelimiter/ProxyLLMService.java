package com.ratelimiter;

import java.util.concurrent.TimeUnit;

/**
 * Proxy pattern implementation for rate limiting.
 * 
 * Acts as a proxy between the client and the actual LLM service.
 * Applies rate limiting before allowing the actual API call.
 * 
 * Follows:
 * - Dependency Inversion Principle: depends on IRateLimiter abstraction
 * - Open/Closed Principle: can add new rate limiters without changing this class
 * - Single Responsibility: only handles rate limiting proxy logic
 */
public class ProxyLLMService implements ILLMService {
    
    private final ILLMService realService;
    private final IRateLimiter rateLimiter;
    
    /**
     * Constructor with dependency injection.
     * @param realService the actual LLM service
     * @param rateLimiter the rate limiter to use
     */
    public ProxyLLMService(ILLMService realService, IRateLimiter rateLimiter) {
        if (realService == null || rateLimiter == null) {
            throw new IllegalArgumentException("realService and rateLimiter cannot be null");
        }
        this.realService = realService;
        this.rateLimiter = rateLimiter;
    }
    
    /**
     * Convenience constructor with default configurations.
     * Creates a FixedWindowCounter with 5 requests per minute.
     */
    public ProxyLLMService() {
        this(new LLMService(), new FixedWindowCounter(5, 1, TimeUnit.MINUTES));
    }
    
    /**
     * Factory method to create with specific rate limiter.
     * Shows how a caller can switch between algorithms without changing business logic.
     */
    public static ProxyLLMService createWithFixedWindow(int maxRequests, int windowSize, TimeUnit timeUnit) {
        return new ProxyLLMService(new LLMService(), new FixedWindowCounter(maxRequests, windowSize, timeUnit));
    }
    
    public static ProxyLLMService createWithSlidingWindow(int maxRequests, int windowSize, TimeUnit timeUnit) {
        return new ProxyLLMService(new LLMService(), new SlidingWindowCounter(maxRequests, windowSize, timeUnit));
    }
    
    @Override
    public void llmApiCall(RequestDto requestDto) {
        // Rate limiting check - only at the point where external call is about to be made
        if (rateLimiter.validateRequest(requestDto)) {
            System.out.println("Rate limit OK for: " + requestDto.getRateLimitKey() + 
                               " (Algorithm: " + rateLimiter.getAlgorithmName() + ")");
            realService.llmApiCall(requestDto);
        } else {
            System.out.println("Rate limit EXCEEDED for: " + requestDto.getRateLimitKey() + 
                               " (Algorithm: " + rateLimiter.getAlgorithmName() + ")");
            throw new RateLimitExceededException("Rate limit exceeded for: " + requestDto.getRateLimitKey());
        }
    }
    
    /**
     * Gets the current rate limiter being used.
     * @return the rate limiter
     */
    public IRateLimiter getRateLimiter() {
        return rateLimiter;
    }
    
    /**
     * Custom exception for rate limit exceeded scenarios.
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}