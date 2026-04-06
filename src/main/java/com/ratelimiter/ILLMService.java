package src.main.java.com.ratelimiter;

/**
 * Interface for LLM Service.
 * Follows Interface Segregation Principle - focused on a single behavior.
 */
public interface ILLMService {
    
    /**
     * Makes an LLM API call.
     * @param requestDto the request data
     */
    void llmApiCall(RequestDto requestDto);
}