import java.util.List;

/**
 * Example of the Strategy Design Pattern.
 * A technical leader would champion such patterns to promote clean, maintainable,
 * and extensible code, making it easier for the team to add new features
 * without modifying existing, tested logic.
 */

// 1. The Strategy Interface
interface NotificationStrategy {
    void send(String userId, String message);
}

// 2. Concrete Strategies
class EmailNotification implements NotificationStrategy {
    @Override
    public void send(String userId, String message) {
        System.out.println("Sending EMAIL to " + userId + ": " + message);
    }
}

class SmsNotification implements NotificationStrategy {
    @Override
    public void send(String userId, String message) {
        System.out.println("Sending SMS to " + userId + ": " + message);
    }
}

class PushNotification implements NotificationStrategy {
    @Override
    public void send(String userId, String message) {
        System.out.println("Sending PUSH notification to " + userId + ": " + message);
    }
}

// 3. The Context that uses the Strategy
class NotificationService {
    private NotificationStrategy strategy;

    // The strategy can be changed at runtime
    public void setStrategy(NotificationStrategy strategy) {
        this.strategy = strategy;
    }

    public void sendNotification(String userId, String message) {
        if (strategy == null) {
            throw new IllegalStateException("Notification strategy not set");
        }
        strategy.send(userId, message);
    }
}

// Example Usage
public class StrategyPatternExample {
    public static void main(String[] args) {
        NotificationService notificationService = new NotificationService();
        
        // A leader explains: "By using the Strategy pattern, we decouple the 'what' from the 'how'.
        // The NotificationService knows it needs to send a notification, but it doesn't
        // care about the specific mechanism. This allows us to add new notification
        // types (like Slack or Teams) in the future without touching the core service logic."

        // Send an email
        notificationService.setStrategy(new EmailNotification());
        notificationService.sendNotification("user-123", "Your order has shipped!");

        // Switch to sending an SMS
        notificationService.setStrategy(new SmsNotification());
        notificationService.sendNotification("user-123", "Your package is out for delivery.");
    }
}
