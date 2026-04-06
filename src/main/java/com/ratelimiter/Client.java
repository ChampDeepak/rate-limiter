package com.ratelimiter;

import java.util.concurrent.TimeUnit;

/**
 * Client demonstration of the pluggable rate limiting system.
 * 
 * Shows how the system can be used to rate limit external resource calls.
 * Demonstrates switching between algorithms without changing business logic.
 */
public class Client {
    
    public static void main(String[] args) {
        System.out.println("=== Pluggable Rate Limiting System Demo ===\n");
        
        // Example 1: Fixed Window Counter - 5 requests per minute
        System.out.println("--- Example 1: Fixed Window Counter (5 req/min) ---");
        demonstrateFixedWindow();
        
        System.out.println();
        
        // Example 2: Sliding Window Counter - 5 requests per minute
        System.out.println("--- Example 2: Sliding Window Counter (5 req/min) ---");
        demonstrateSlidingWindow();
        
        System.out.println();
        
        // Example 3: Different rate limiting keys (per customer, per API key, etc.)
        System.out.println("--- Example 3: Different Rate Limit Keys ---");
        demonstrateDifferentKeys();
        
        System.out.println();
        
        // Example 4: Show how to switch algorithms easily
        System.out.println("--- Example 4: Switching Algorithms ---");
        demonstrateAlgorithmSwitching();
        
        System.out.println("\n=== Demo Complete ===");
    }
    
    private static void demonstrateFixedWindow() {
        // 5 requests per minute for customer T1
        IRateLimiter rateLimiter = new FixedWindowCounter(5, 1, TimeUnit.MINUTES);
        ProxyLLMService proxy = new ProxyLLMService(new LLMService(), rateLimiter);
        
        // Make 6 requests - last one should be rejected
        for (int i = 1; i <= 6; i++) {
            RequestDto request = new RequestDto("customerT1", null, null, null);
            try {
                proxy.llmApiCall(request);
                System.out.println("Request " + i + ": ALLOWED");
            } catch (ProxyLLMService.RateLimitExceededException e) {
                System.out.println("Request " + i + ": DENIED - " + e.getMessage());
            }
        }
    }
    
    private static void demonstrateSlidingWindow() {
        // 5 requests per minute with sliding window
        IRateLimiter rateLimiter = new SlidingWindowCounter(5, 1, TimeUnit.MINUTES);
        ProxyLLMService proxy = new ProxyLLMService(new LLMService(), rateLimiter);
        
        // Make 6 requests - last one should be rejected
        for (int i = 1; i <= 6; i++) {
            RequestDto request = new RequestDto("customerT1", null, null, null);
            try {
                proxy.llmApiCall(request);
                System.out.println("Request " + i + ": ALLOWED");
            } catch (ProxyLLMService.RateLimitExceededException e) {
                System.out.println("Request " + i + ": DENIED - " + e.getMessage());
            }
        }
    }
    
    private static void demonstrateDifferentKeys() {
        // 2 requests per minute per API key
        IRateLimiter rateLimiter = new FixedWindowCounter(2, 1, TimeUnit.MINUTES);
        ProxyLLMService proxy = new ProxyLLMService(new LLMService(), rateLimiter);
        
        // Different API keys have separate limits
        String[] apiKeys = {"key1", "key2", "key1"};
        
        for (String apiKey : apiKeys) {
            RequestDto request = new RequestDto(null, null, apiKey, null);
            try {
                proxy.llmApiCall(request);
                System.out.println("API Key " + apiKey + ": ALLOWED");
            } catch (ProxyLLMService.RateLimitExceededException e) {
                System.out.println("API Key " + apiKey + ": DENIED");
            }
        }
    }
    
    private static void demonstrateAlgorithmSwitching() {
        // Show how easy it is to switch algorithms
        // Just change the IRateLimiter implementation
        
        // Using Fixed Window
        ProxyLLMService fixedProxy = ProxyLLMService.createWithFixedWindow(3, 1, TimeUnit.MINUTES);
        System.out.println("Using: " + fixedProxy.getRateLimiter().getAlgorithmName());
        
        // Switch to Sliding Window - no business logic changes needed
        ProxyLLMService slidingProxy = ProxyLLMService.createWithSlidingWindow(3, 1, TimeUnit.MINUTES);
        System.out.println("Switched to: " + slidingProxy.getRateLimiter().getAlgorithmName());
        
        System.out.println("Business logic (llmApiCall) remains the same!");
    }
}