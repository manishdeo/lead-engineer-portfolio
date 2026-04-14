import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Example demonstrating Virtual Threads (Project Loom), a key feature in modern Java.
 * Virtual threads are lightweight threads ideal for high-throughput, I/O-bound applications.
 */
public class VirtualThreadsExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- Running with Platform Threads ---");
        runWithPlatformThreads();

        System.out.println("\n--- Running with Virtual Threads ---");
        runWithVirtualThreads();
    }

    /**
     * Using traditional platform threads. Creating 10,000 platform threads can
     * exhaust system resources and lead to poor performance due to OS-level context switching.
     */
    public static void runWithPlatformThreads() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        try (var executor = Executors.newFixedThreadPool(200)) { // Limited pool size
            IntStream.range(0, 10_000).forEach(i -> {
                executor.submit(() -> {
                    ioBoundTask(i);
                });
            });
        }
        
        long endTime = System.currentTimeMillis();
        System.out.printf("Platform threads took %d ms%n", (endTime - startTime));
    }

    /**
     * Using virtual threads. The JVM can handle millions of virtual threads,
     * as they are not tied 1:1 to OS threads.
     */
    public static void runWithVirtualThreads() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        // newVirtualThreadPerTaskExecutor creates a new virtual thread for each task.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 10_000).forEach(i -> {
                executor.submit(() -> {
                    ioBoundTask(i);
                });
            });
        } // The executor automatically waits for all tasks to complete.

        long endTime = System.currentTimeMillis();
        System.out.printf("Virtual threads took %d ms%n", (endTime - startTime));
    }

    /**
     * A mock I/O-bound task, like calling a remote API or a database.
     */
    private static void ioBoundTask(int taskNumber) {
        try {
            // System.out.println("Executing task " + taskNumber + " on thread: " + Thread.currentThread());
            Thread.sleep(Duration.ofSeconds(1)); // Simulate I/O wait
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
