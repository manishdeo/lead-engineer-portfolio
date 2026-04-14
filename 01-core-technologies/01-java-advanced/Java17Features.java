package com.interview.java.modern;

/**
 * Demonstrates Java 17+ Features: Records, Sealed Classes, and Pattern Matching
 */
public class Java17Features {

    // 1. Records (Introduced in Java 14, standard in Java 16)
    // A concise way to create immutable data carriers
    public record Point(int x, int y) {
        // Can add custom methods or validation in the compact constructor
        public Point {
            if (x < 0 || y < 0) {
                throw new IllegalArgumentException("Coordinates must be positive");
            }
        }
    }

    // 2. Sealed Classes (Introduced in Java 15, standard in Java 17)
    // Restricts which classes can extend or implement them
    public sealed interface Shape permits Circle, Rectangle, Square {}

    public final class Circle implements Shape {
        private final double radius;
        public Circle(double radius) { this.radius = radius; }
        public double radius() { return radius; }
    }

    public final class Rectangle implements Shape {
        private final double width, height;
        public Rectangle(double width, double height) { this.width = width; this.height = height; }
        public double width() { return width; }
        public double height() { return height; }
    }

    // Must be final, sealed, or non-sealed
    public non-sealed class Square implements Shape {
        private final double side;
        public Square(double side) { this.side = side; }
        public double side() { return side; }
    }

    // 3. Pattern Matching for switch (Preview in Java 17, Standard in Java 21)
    public static double calculateArea(Shape shape) {
        return switch (shape) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
            case Square s -> s.side() * s.side();
            // No default needed because Shape is sealed and all permits are covered
        };
    }

    public static void main(String[] args) {
        System.out.println("Demonstrating Java 17+ Features...");

        // Record Example
        Point p = new Point(10, 20);
        System.out.println("Record Point: " + p);
        System.out.println("X coordinate: " + p.x());

        // Sealed Class and Pattern Matching Example
        Shape circle = new Java17Features().new Circle(5.0);
        Shape rectangle = new Java17Features().new Rectangle(4.0, 6.0);
        Shape square = new Java17Features().new Square(4.0);

        System.out.println("Circle Area: " + calculateArea(circle));
        System.out.println("Rectangle Area: " + calculateArea(rectangle));
        System.out.println("Square Area: " + calculateArea(square));
    }
}
