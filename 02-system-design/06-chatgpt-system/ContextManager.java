import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the conversation context for a ChatGPT-like system.
 * Implements a sliding window and summarization strategy to stay within the LLM's token budget.
 */
public class ContextManager {

    private final int tokenBudget;
    private final LinkedList<Message> conversationHistory = new LinkedList<>();
    private final Tokenizer tokenizer; // Assume a tokenizer like tiktoken

    public ContextManager(int tokenBudget) {
        this.tokenBudget = tokenBudget;
        this.tokenizer = new Tokenizer();
    }

    public void addMessage(Message message) {
        conversationHistory.add(message);
        ensureBudget();
    }

    public String buildPrompt(String systemPrompt, String userQuery) {
        String historyString = conversationHistory.stream()
            .map(m -> m.getRole() + ": " + m.getContent())
            .collect(Collectors.joining("\n"));
        
        return systemPrompt + "\n\n" + historyString + "\n\nUser: " + userQuery;
    }

    private void ensureBudget() {
        int currentTokens = countTokens();
        
        while (currentTokens > tokenBudget && conversationHistory.size() > 1) {
            // Strategy 1: Simple Sliding Window - remove the oldest message
            conversationHistory.removeFirst();
            
            // Strategy 2: Summarization (more advanced)
            // If history is long, summarize the oldest few messages into one
            // Message summary = summarize(conversationHistory.subList(0, 3));
            // conversationHistory.subList(0, 3).clear();
            // conversationHistory.addFirst(summary);
            
            currentTokens = countTokens();
        }
    }

    private int countTokens() {
        return conversationHistory.stream()
            .mapToInt(m -> tokenizer.count(m.getContent()))
            .sum();
    }

    // Stub classes for demonstration
    static class Message {
        String role; String content;
        public Message(String role, String content) { this.role = role; this.content = content; }
        public String getRole() { return role; }
        public String getContent() { return content; }
    }
    static class Tokenizer {
        int count(String text) { return text.split("\\s+").length; /* Simple word count */ }
    }
}
