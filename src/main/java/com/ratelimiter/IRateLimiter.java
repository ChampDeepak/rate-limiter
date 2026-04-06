package com.ratelimiter;

/**
 * Interface for rate limiting algorithms.
 * Following Interface Segregation Principle - simple and focused interface.
 * 
 * The system should support configuring limits such as:
 * - 100 requests per minute
 * - 1000 requests per hour
 * 
 * The rate limiting key can vary based on use case, for example:
 * - per customer
 * - per tenant
 * - per API key
 * - per external provider
 */
public interface IRateLimiter {
    
    /**
     * Validates if a request is allowed based on rate limiting rules.
     * 
     * @param requestDto the request containing key information for rate limiting
     * @return true if the request is allowed, false if rate limit exceeded
     */
    boolean validateRequest(RequestDto requestDto);
    
    /**
     * Returns the name of the rate limiting algorithm.
     * @return algorithm name
     */
    String getAlgorithmName();
}