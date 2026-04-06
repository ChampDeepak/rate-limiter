package com.ratelimiter;

/**
 * Data transfer object for requests.
 * Contains information needed for rate limiting decisions.
 * 
 * The rate limiting key can vary based on use case:
 * - per customer (customerId)
 * - per tenant (tenantId)
 * - per API key (apiKey)
 * - per external provider (providerId)
 */
public class RequestDto {
    private String customerId;
    private String tenantId;
    private String apiKey;
    private String providerId;
    private long timestamp;

    public RequestDto() {
        this.timestamp = System.currentTimeMillis();
    }

    public RequestDto(String customerId, String tenantId, String apiKey, String providerId) {
        this.customerId = customerId;
        this.tenantId = tenantId;
        this.apiKey = apiKey;
        this.providerId = providerId;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Gets the rate limiting key based on available information.
     * Priority: apiKey > customerId > tenantId > providerId
     * @return the rate limiting key
     */
    public String getRateLimitKey() {
        if (apiKey != null && !apiKey.isEmpty()) {
            return "api:" + apiKey;
        }
        if (customerId != null && !customerId.isEmpty()) {
            return "customer:" + customerId;
        }
        if (tenantId != null && !tenantId.isEmpty()) {
            return "tenant:" + tenantId;
        }
        if (providerId != null && !providerId.isEmpty()) {
            return "provider:" + providerId;
        }
        return "default";
    }

    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "RequestDto{customerId='" + customerId + "', tenantId='" + tenantId + 
               "', apiKey='" + apiKey + "', providerId='" + providerId + "'}";
    }
}