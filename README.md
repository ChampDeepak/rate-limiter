# Pluggable Rate Limiting System for External Resource Usage

## Overview

This project implements a **pluggable rate limiting system** for controlling external resource calls. The system follows the **Proxy Design Pattern** to intercept calls to external services and apply rate limiting before allowing the actual API call.

### Design Philosophy

The system is designed to rate limit **external resource calls**, not incoming client API requests. This means:
- Not every API request consumes quota
- Rate limiting is applied only at the point where the system is about to call the external resource
- Business logic runs first; rate limiting is checked only when an external call is needed

```
Client API Request
        |
        v
   Business Logic
        |
        v
  (No external call) -----> Return Response
        |
   (External call needed)
        |
        v
  Rate Limiter Check
        |
        +-- Allowed --> Call External API --> Return Response
        |
        +-- Denied   --> Reject with Exception
```

---

## Design Patterns Used

### 1. Proxy Pattern
The `ProxyLLMService` acts as a proxy between the client and the actual LLM service. It intercepts calls and applies rate limiting before delegating to the real service.

### 2. Strategy Pattern
The `IRateLimiter` interface defines the strategy for rate limiting. `FixedWindowCounter` and `SlidingWindowCounter` are concrete implementations of this strategy. Clients can switch algorithms by simply substituting one implementation for another without changing business logic.

### 3. Static Factory Methods
`ProxyLLMService` provides static factory methods (`createWithFixedWindow`, `createWithSlidingWindow`) for convenient creation with specific rate limiters. This is a simplified form of the Factory pattern using static methods rather than inheritance.

### 4. Dependency Injection
The `ProxyLLMService` accepts `ILLMService` and `IRateLimiter` through its constructor, enabling easy testing and swapping of implementations.

---

## Class Diagram

```
                    <<interface>>
                   +-----------------+
                   |   ILLMService   |
                   +-----------------+
                   | + llmApiCall()  |
                   +--------+--------+
                            |
              +-------------+-------------+
              |                           |
    +---------v---------+       +---------+--------+
    |    LLMService     |       |  ProxyLLMService |
    +-------------------+       +---------+--------+
                                     |
                                     | has-a (composition)
                                     v
                           +---------+--------+
                           |  IRateLimiter    |
                           +---------+--------+
                                     ^
                                     |
              +---------------------+--------------------+
              |                     |                    |
    +---------v-----+     +---------+--------+   +------v------+
    | FixedWindow   |     | SlidingWindow    |   | RateLimit   |
    |   Counter     |     |   Counter        |   |   Config    |
    +---------------+     +------------------+   +-------------+
              |
              | uses (dependency)
              v
    +---------v------+
    |   RequestDto   |
    +----------------+
```

---

## Key Design Decisions

### 1. Interface Segregation Principle (ISP)
- `IRateLimiter` is a focused, single-method interface
- `ILLMService` defines only one behavior (`llmApiCall`)
- This makes implementations simple and testable

### 2. Open/Closed Principle (OCP)
- New rate limiting algorithms can be added without modifying existing code
- Just implement `IRateLimiter` and plug it into `ProxyLLMService`

### 3. Dependency Inversion Principle (DIP)
- `ProxyLLMService` depends on abstractions (`ILLMService`, `IRateLimiter`) not concrete implementations
- This enables easy mocking for testing and swapping implementations

### 4. Thread Safety
- Using `ConcurrentHashMap` for thread-safe storage
- Using `AtomicLong` for thread-safe counter operations
- `compute()` method ensures atomic check-and-update operations

### 5. Extensible Rate Limiting Keys
- `RequestDto` supports multiple key types: customerId, tenantId, apiKey, providerId
- Priority-based key selection: apiKey > customerId > tenantId > providerId
- Format: `"api:key1"`, `"customer:T1"`, etc.

---

## Class Reference

### 1. `ILLMService` (Interface)
| Aspect | Details |
|--------|---------|
| **Purpose** | Defines the contract for LLM service implementations |
| **Methods** | `void llmApiCall(RequestDto requestDto)` |
| **Relationships** | Implemented by `LLMService` and `ProxyLLMService` |
| **Pattern** | Interface Segregation Principle - focused interface |

---

### 2. `LLMService` (Class)
| Aspect | Details |
|--------|---------|
| **Purpose** | Represents the actual external LLM service that makes API calls |
| **Attributes** | None (stateless) |
| **Methods** | `llmApiCall(RequestDto requestDto)` - Simulates external API call |
| **Relationships** | Implements `ILLMService` |
| **Note** | In production, this would make actual HTTP calls to external LLM APIs |

---

### 3. `IRateLimiter` (Interface)
| Aspect | Details |
|--------|---------|
| **Purpose** | Defines the contract for rate limiting algorithms |
| **Methods** | `boolean validateRequest(RequestDto requestDto)`, `String getAlgorithmName()` |
| **Relationships** | Implemented by `FixedWindowCounter`, `SlidingWindowCounter` |
| **Pattern** | Strategy Pattern - allows swapping algorithms |

---

### 4. `FixedWindowCounter` (Class)
| Aspect | Details |
|--------|---------|
| **Purpose** | Implements the Fixed Window Counter rate limiting algorithm |
| **Attributes** | `RateLimitConfig config`, `ConcurrentMap<String, WindowCounter> counters` |
| **Methods** | `validateRequest(RequestDto)`, `getAlgorithmName()`, `getCurrentCount(RequestDto)` |
| **Relationships** | Implements `IRateLimiter`, uses `RateLimitConfig` and `RequestDto` |
| **Algorithm** | Divides time into fixed windows; counts requests per window |
| **Thread Safety** | Uses `ConcurrentHashMap` and atomic operations via `compute()` |

**Inner Class: `WindowCounter`**
- Holds window start timestamp and request count
- Uses `AtomicLong` for thread-safe count operations
- Package-private visibility

---

### 5. `SlidingWindowCounter` (Class)
| Aspect | Details |
|--------|---------|
| **Purpose** | Implements the Sliding Window Counter rate limiting algorithm |
| **Attributes** | `RateLimitConfig config`, `ConcurrentMap<String, SlidingWindow> windows` |
| **Methods** | `validateRequest(RequestDto)`, `getAlgorithmName()`, `getCurrentCount(RequestDto)` |
| **Relationships** | Implements `IRateLimiter`, uses `RateLimitConfig` and `RequestDto` |
| **Algorithm** | Uses a sliding time window for more granular burst control |
| **Thread Safety** | Uses `ConcurrentHashMap` with atomic compute operations |

**Inner Class: `SlidingWindow`**
- Tracks requests in time buckets within the sliding window
- Provides cleanup to remove expired buckets
- Uses `ConcurrentHashMap` to store time-bucketed request counts
- Package-private visibility

---

### 6. `ProxyLLMService` (Class)
| Aspect | Details |
|--------|---------|
| **Purpose** | Proxy pattern implementation that applies rate limiting before external calls |
| **Attributes** | `ILLMService realService`, `IRateLimiter rateLimiter` |
| **Methods** | `llmApiCall(RequestDto)`, `getRateLimiter()`, factory methods |
| **Relationships** | Implements `ILLMService`, has-a relationship with `ILLMService` and `IRateLimiter` (composition) |
| **Pattern** | Proxy Pattern, Factory Method Pattern |

**Inner Class: `RateLimitExceededException`**
- Custom runtime exception for rate limit exceeded scenarios

---

### 7. `RateLimitConfig` (Class)
| Aspect | Details |
|--------|---------|
| **Purpose** | Configuration class for rate limiting parameters |
| **Attributes** | `long maxRequests`, `long windowSize`, `TimeUnit timeUnit`, `String keyPrefix` |
| **Methods** | Getters, `getWindowSizeInMillis()` |
| **Relationships** | Used by rate limiter implementations |
| **Validation** | Ensures positive values and non-null time unit |

---

### 8. `RequestDto` (Class)
| Aspect | Details |
|--------|---------|
| **Purpose** | Data transfer object containing request information for rate limiting |
| **Attributes** | `customerId`, `tenantId`, `apiKey`, `providerId`, `timestamp` |
| **Methods** | `getRateLimitKey()`, getters/setters |
| **Relationships** | Passed to rate limiters for key extraction |
| **Key Priority** | apiKey > customerId > tenantId > providerId > "default" |

---

### 9. `Client` (Class)
| Aspect | Details |
|--------|---------|
| **Purpose** | Demonstration of the pluggable rate limiting system |
| **Attributes** | None (contains main method) |
| **Methods** | `main()`, demo methods |
| **Relationships** | Uses all other classes to demonstrate functionality |
| **Usage** | Run to see examples of algorithm switching and rate limiting |

---

## Relationships Summary

| Relationship Type | Classes |
|-------------------|---------|
| **Implementation** | `LLMService` implements `ILLMService` |
| **Implementation** | `FixedWindowCounter` implements `IRateLimiter` |
| **Implementation** | `SlidingWindowCounter` implements `IRateLimiter` |
| **Implementation** | `ProxyLLMService` implements `ILLMService` |
| **Composition** | `ProxyLLMService` has-a `ILLMService` (owns) |
| **Composition** | `ProxyLLMService` has-a `IRateLimiter` (owns) |
| **Dependency** | Rate limiters depend on `RequestDto` |
| **Dependency** | Rate limiters depend on `RateLimitConfig` |
| **Dependency** | `RequestDto` is passed to rate limiters (used by) |

---

## Switching Between Algorithms

The system allows easy switching between rate limiting algorithms **without changing business logic**. Here are the ways to do it:

### Method 1: Direct Constructor Injection

```java
// Using Fixed Window
IRateLimiter rateLimiter = new FixedWindowCounter(5, 1, TimeUnit.MINUTES);
ProxyLLMService proxy = new ProxyLLMService(new LLMService(), rateLimiter);

// Switch to Sliding Window - just change the implementation
rateLimiter = new SlidingWindowCounter(5, 1, TimeUnit.MINUTES);
proxy = new ProxyLLMService(new LLMService(), rateLimiter);

// Business logic (llmApiCall) remains exactly the same!
proxy.llmApiCall(request);
```

### Method 2: Factory Methods

```java
// Using Fixed Window
ProxyLLMService fixedProxy = ProxyLLMService.createWithFixedWindow(5, 1, TimeUnit.MINUTES);

// Switch to Sliding Window
ProxyLLMService slidingProxy = ProxyLLMService.createWithSlidingWindow(5, 1, TimeUnit.MINUTES);

// Both provide the same interface - business logic is unchanged
fixedProxy.llmApiCall(request);   // Same method call
slidingProxy.llmApiCall(request); // Same method call
```

### Method 3: Runtime Switching

```java
public class RateLimitingService {
    private IRateLimiter rateLimiter;
    
    public void setRateLimiter(IRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }
    
    public void processRequest(RequestDto request) {
        // Business logic unchanged regardless of rate limiter
        ProxyLLMService proxy = new ProxyLLMService(new LLMService(), rateLimiter);
        proxy.llmApiCall(request);
    }
}

// At runtime, switch algorithms
RateLimitingService service = new RateLimitingService();
service.setRateLimiter(new FixedWindowCounter(100, 1, TimeUnit.MINUTES));
// Later...
service.setRateLimiter(new SlidingWindowCounter(100, 1, TimeUnit.MINUTES));
```

The key insight is that **the `llmApiCall` method never changes** - only the `IRateLimiter` implementation is swapped.

---

## Algorithm Trade-offs

### Fixed Window Counter

**How it works:**
- Divides time into fixed-size windows (e.g., 1-minute windows)
- Counts requests within each window
- Resets counter at the start of each new window

**Example Timeline (5 requests per minute):**
```
Time: 0:00 -----> 1:00 -----> 2:00 -----> 3:00
Window: [W1]       [W2]       [W3]
Count:  0/5        0/5        0/5
```

**Pros:**
- Simple to implement
- Low memory overhead (one counter per key)
- Predictable behavior
- Good for low-traffic scenarios

**Cons:**
- **Burst problem**: Allows up to 2x requests at window boundaries
  - Example: 5 requests at 0:59 and 5 more at 1:00 = 10 requests in 2 seconds!
- Less smooth rate limiting
- Not ideal for high-traffic APIs needing consistent throughput

**Time Complexity:** O(1) per request
**Space Complexity:** O(n) where n = number of unique rate limit keys

---

### Sliding Window Counter

**How it works:**
- Maintains a sliding window that moves continuously
- Tracks requests with timestamps
- Counts only requests within the current window period

**Example Timeline (5 requests per minute, sliding):**
```
Time: 0:00 -----> 0:30 -----> 1:00
Window: [Sliding from 0:00] [Sliding from 0:30]
```

**Pros:**
- **Smoother rate limiting**: No burst at window boundaries
- **Better burst control**: Distributes requests evenly
- More accurate tracking of request rates
- Ideal for high-traffic APIs

**Cons:**
- More complex implementation
- Higher memory overhead (stores timestamp data)
- Slightly more CPU for cleanup operations
- More complex to monitor and debug

**Time Complexity:** O(1) amortized per request (includes cleanup)
**Space Complexity:** O(n × m) where n = keys, m = requests in window

---

### Comparison Table

| Aspect | Fixed Window | Sliding Window |
|--------|--------------|----------------|
| **Implementation Complexity** | Low | Medium |
| **Memory Usage** | Low | Medium |
| **Burst Control** | Poor (2x burst possible) | Good |
| **Smoothness** | Blocky | Smooth |
| **Accuracy** | Lower (at boundaries) | Higher |
| **Use Case** | Low traffic, simple needs | High traffic, precise control |
| **Debugging** | Easy | Moderate |

---

## Usage Examples

### Example 1: Rate Limit by Customer (5 requests/minute)

```java
IRateLimiter rateLimiter = new FixedWindowCounter(5, 1, TimeUnit.MINUTES);
ProxyLLMService proxy = new ProxyLLMService(new LLMService(), rateLimiter);

RequestDto request = new RequestDto("customerT1", null, null, null);
try {
    proxy.llmApiCall(request);
    System.out.println("Request allowed");
} catch (ProxyLLMService.RateLimitExceededException e) {
    System.out.println("Rate limit exceeded");
}
```

### Example 2: Rate Limit by API Key (100 requests/hour)

```java
IRateLimiter rateLimiter = new SlidingWindowCounter(100, 1, TimeUnit.HOURS);
ProxyLLMService proxy = new ProxyLLMService(new LLMService(), rateLimiter);

RequestDto request = new RequestDto(null, null, "api-key-123", null);
try {
    proxy.llmApiCall(request);
} catch (ProxyLLMService.RateLimitExceededException e) {
    // Handle rate limit exceeded
}
```

### Example 3: Different Keys Have Separate Limits

```java
IRateLimiter rateLimiter = new FixedWindowCounter(2, 1, TimeUnit.MINUTES);
ProxyLLMService proxy = new ProxyLLMService(new LLMService(), rateLimiter);

// key1: 2 requests allowed
// key2: 2 requests allowed (separate limit)
RequestDto req1 = new RequestDto(null, null, "key1", null);
RequestDto req2 = new RequestDto(null, null, "key2", null);
```

---

## Getting Started

### Prerequisites
- Java 8 or higher
- Maven 3.6 or higher

### Clone the Repository

```bash
# Clone the repository 
git clone https://github.com/ChampDeepak/rate-limiter
# Navigate to the project directory
cd rate-limiter
```

### Verify Prerequisites

```bash
# Check Java and Maven versions
java -version
mvn -version
```

### Build and Run

```bash
# Compile and run the client demo
mvn clean compile
mvn exec:java
```

Maven will automatically compile the code if needed. Class files are placed in `target/classes` (not the source directory), keeping the project clean.

---

## Running the Demo

Expected output shows:
1. Fixed Window Counter limiting 5 requests/minute
2. Sliding Window Counter limiting 5 requests/minute
3. Different rate limit keys (per API key)
4. Algorithm switching demonstration

---

## Extending the System

To add a new rate limiting algorithm (e.g., Token Bucket):

1. Create a new class implementing `IRateLimiter`:
```java
public class TokenBucket implements IRateLimiter {
    @Override
    public boolean validateRequest(RequestDto requestDto) {
        // Implementation here
    }
    
    @Override
    public String getAlgorithmName() {
        return "TokenBucket";
    }
}
```

2. Use it in `ProxyLLMService`:
```java
IRateLimiter rateLimiter = new TokenBucket(/* config */);
ProxyLLMService proxy = new ProxyLLMService(new LLMService(), rateLimiter);
```

No changes to business logic required!

---

## Design Principles Summary

| Principle | How It's Applied |
|-----------|------------------|
| **Single Responsibility** | Each class has one clear purpose |
| **Open/Closed** | New algorithms added without modifying existing code |
| **Liskov Substitution** | All IRateLimiter implementations are interchangeable |
| **Interface Segregation** | Focused, minimal interfaces |
| **Dependency Inversion** | Depends on abstractions, not concretions |

---

## Non-Functional Requirements Met

- **Extensibility**: Strategy pattern allows easy addition of new algorithms
- **Design Principles**: SOLID principles followed throughout
- **Thread Safety**: ConcurrentHashMap and AtomicLong for thread-safe operations
- **Performance**: O(1) operations with efficient memory usage
- **Testability**: Dependency injection enables easy mocking and testing
