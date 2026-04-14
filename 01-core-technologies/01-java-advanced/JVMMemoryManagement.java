package com.interview.java.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates JVM Memory Areas and how to cause OutOfMemoryError
 */
public class JVMMemoryManagement {

    /**
     * Demonstrates Heap Space OutOfMemoryError
     * Run with VM option: -Xmx10m
     */
    public static void causeHeapSpaceError() {
        System.out.println("Demonstrating Heap Space OutOfMemoryError...");
        List<byte[]> list = new ArrayList<>();
        try {
            while (true) {
                list.add(new byte[1024 * 1024]); // Allocate 1MB in each iteration
            }
        } catch (OutOfMemoryError e) {
            System.out.println("Caught OutOfMemoryError: " + e.getMessage());
        }
    }

    /**
     * Demonstrates Stack Overflow Error
     */
    public static void causeStackOverflowError() {
        System.out.println("Demonstrating StackOverflowError...");
        try {
            recursiveMethod(1);
        } catch (StackOverflowError e) {
            System.out.println("Caught StackOverflowError: " + e.getMessage());
        }
    }

    private static void recursiveMethod(int i) {
        System.out.println("Recursive call number: " + i);
        recursiveMethod(i + 1);
    }

    public static void main(String[] args) {
        causeHeapSpaceError();
        System.out.println("\n-----------------------------------\n");
        causeStackOverflowError();
    }
}
