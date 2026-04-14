package com.interview.java.jvm;

/**
 * Demonstrates Garbage Collection concepts
 */
public class GarbageCollectionExamples {

    public static void main(String[] args) {
        System.out.println("Demonstrating Garbage Collection...");

        // Create a large number of objects to trigger GC
        for (int i = 0; i < 1000; i++) {
            new GCObject("Object " + i);
        }

        // Suggest the JVM to run the garbage collector
        System.gc();

        System.out.println("Garbage Collection suggested. Check console for finalize() method calls.");
    }
}

class GCObject {
    private String name;

    public GCObject(String name) {
        this.name = name;
        System.out.println(this.name + " created.");
    }

    /**
     * This method is called by the garbage collector on an object when garbage
     * collection determines that there are no more references to the object.
     */
    @Override
    protected void finalize() throws Throwable {
        System.out.println(this.name + " is being finalized.");
        super.finalize();
    }
}
