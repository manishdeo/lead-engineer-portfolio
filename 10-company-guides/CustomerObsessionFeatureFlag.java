import java.util.Set;

/**
 * This example embodies Amazon's "Customer Obsession" Leadership Principle.
 * A common interview topic at Amazon is how to design systems that prioritize
 * the customer experience. This feature flag system shows a customer-aware
 * rollout strategy, minimizing negative impact.
 */
public class CustomerObsessionFeatureFlag {

    private final FeatureFlagProvider flagProvider;

    public CustomerObsessionFeatureFlag(FeatureFlagProvider flagProvider) {
        this.flagProvider = flagProvider;
    }

    public void executeNewCheckoutFlow(String customerId) {
        // Instead of a simple on/off, the flag is evaluated based on customer context.
        // This demonstrates a customer-obsessed approach to rolling out new features.
        if (flagProvider.isFeatureEnabled("new-checkout-flow", customerId)) {
            System.out.println("Executing NEW checkout flow for customer: " + customerId);
            // newCheckoutLogic();
        } else {
            System.out.println("Executing OLD checkout flow for customer: " + customerId);
            // oldCheckoutLogic();
        }
    }

    /**
     * A mock provider that simulates a sophisticated feature flag service
     * like AWS AppConfig or LaunchDarkly.
     */
    interface FeatureFlagProvider {
        boolean isFeatureEnabled(String flagName, String customerId);
    }

    /**
     * A simple implementation showing a percentage-based and targeted-customer rollout.
     */
    static class SimpleFlagProvider implements FeatureFlagProvider {
        private final double rolloutPercentage;
        private final Set<String> includedCustomers; // For internal testing or specific customer groups

        public SimpleFlagProvider(double rolloutPercentage, Set<String> includedCustomers) {
            this.rolloutPercentage = rolloutPercentage;
            this.includedCustomers = includedCustomers;
        }

        @Override
        public boolean isFeatureEnabled(String flagName, String customerId) {
            // Principle: Always enable for internal or beta-tester customers first.
            if (includedCustomers.contains(customerId)) {
                return true;
            }
            
            // Principle: Roll out slowly to the general population to monitor impact.
            // A simple hash function ensures a customer consistently sees the same experience.
            int customerHash = Math.abs(customerId.hashCode());
            return (customerHash % 100) < rolloutPercentage;
        }
    }

    public static void main(String[] args) {
        System.out.println("Demonstrating Customer-Obsessed Feature Rollout (Amazon LP)");

        // The lead decides on a safe, customer-centric rollout plan:
        // 1. Enable only for internal test accounts.
        // 2. Slowly ramp up to 1%, then 10%, then 50%, etc., while monitoring metrics.
        Set<String> internalTesters = Set.of("internal-tester-1", "beta-user-xyz");
        FeatureFlagProvider flagProvider = new SimpleFlagProvider(10.0, internalTesters); // Start with 10% rollout
        
        CustomerObsessionFeatureFlag feature = new CustomerObsessionFeatureFlag(flagProvider);

        // --- Simulate different customers ---
        
        // An internal tester always gets the new feature.
        feature.executeNewCheckoutFlow("internal-tester-1"); 
        
        // A regular customer may or may not get it based on the rollout percentage.
        feature.executeNewCheckoutFlow("customer-456");
        feature.executeNewCheckoutFlow("customer-789");
    }
}
