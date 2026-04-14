import java.util.ArrayList;
import java.util.List;

/**
 * Example of the Saga Orchestration pattern for distributed transactions.
 * The orchestrator coordinates a series of steps and their corresponding
 * compensating actions if a failure occurs.
 */
public class SagaOrchestrator {

    private final List<SagaStep> steps = new ArrayList<>();

    public SagaOrchestrator addStep(Runnable action, Runnable compensation) {
        steps.add(new SagaStep(action, compensation));
        return this;
    }

    public boolean execute() {
        int executedSteps = 0;
        try {
            for (SagaStep step : steps) {
                step.action.run();
                executedSteps++;
            }
            return true; // Saga completed successfully
        } catch (Exception e) {
            System.err.println("Saga failed at step " + (executedSteps + 1) + ". Initiating compensation.");
            compensate(executedSteps);
            return false;
        }
    }

    private void compensate(int executedSteps) {
        for (int i = executedSteps - 1; i >= 0; i--) {
            try {
                steps.get(i).compensation.run();
            } catch (Exception e) {
                System.err.println("Compensation failed for step " + (i + 1) + ". Manual intervention required.");
                // Log the failure for manual recovery
            }
        }
    }

    private static class SagaStep {
        final Runnable action;
        final Runnable compensation;

        SagaStep(Runnable action, Runnable compensation) {
            this.action = action;
            this.compensation = compensation;
        }
    }

    // --- Example Usage ---
    public static void main(String[] args) {
        // Mock services
        OrderService orderService = new OrderService();
        InventoryService inventoryService = new InventoryService();
        PaymentService paymentService = new PaymentService();

        // Define the saga
        SagaOrchestrator saga = new SagaOrchestrator()
            .addStep(orderService::createOrder, orderService::cancelOrder)
            .addStep(inventoryService::reserveInventory, inventoryService::releaseInventory)
            .addStep(paymentService::processPayment, paymentService::refundPayment);

        System.out.println("Executing successful saga...");
        saga.execute();

        System.out.println("\nExecuting failing saga...");
        paymentService.setShouldFail(true); // Configure payment to fail
        saga.execute();
    }
}

// Mock service classes for the example
class OrderService {
    void createOrder() { System.out.println("Order created."); }
    void cancelOrder() { System.out.println("COMPENSATION: Order canceled."); }
}
class InventoryService {
    void reserveInventory() { System.out.println("Inventory reserved."); }
    void releaseInventory() { System.out.println("COMPENSATION: Inventory released."); }
}
class PaymentService {
    private boolean shouldFail = false;
    void setShouldFail(boolean fail) { this.shouldFail = fail; }
    void processPayment() {
        if (shouldFail) throw new RuntimeException("Payment processor offline");
        System.out.println("Payment processed.");
    }
    void refundPayment() { System.out.println("COMPENSATION: Payment refunded."); }
}
