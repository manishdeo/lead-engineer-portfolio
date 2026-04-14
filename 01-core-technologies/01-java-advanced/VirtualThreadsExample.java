package com.interview.java.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Demonstrates Virtual Threads (Project Loom), introduced in Java 19 (Preview) and standard in Java 21.
 * Requires JDK 21+ to run without preview flags.
 */
public class VirtualThreadsExample {

    public static void main(String[] args) {
        System.out.println("Demonstrating Virtual Threads...");

        // Using Executors.newVirtualThreadPerTaskExecutor()
        // This executor creates a new virtual thread for each submitted task.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 10_000).forEach(i -> {
                executor.submit(() -> {
                    // Simulate a blocking I/O operation
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (i % 1000 == 0) {
                        System.out.println("Task " + i + " completed on: " + Thread.currentThread());
                    }
                });
            });
        } // The executor will wait for all tasks to complete before closing.

        System.out.println("All virtual threads completed.");
    }
}
