package src.main.java.com.ratelimiter;

/**
 * LLM Service that makes external API calls.
 * This represents the internal service that may call external resources.
 */
public class LLMService implements ILLMService {

    @Override
    public void llmApiCall(RequestDto requestDto) {
        // Simulate external API call
        System.out.println("LLM API call made for: " + requestDto.getRateLimitKey());
    }
}