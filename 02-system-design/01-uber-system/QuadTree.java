import java.util.ArrayList;
import java.util.List;

/**
 * Basic QuadTree implementation for Geospatial Indexing.
 * Used in systems like Uber to efficiently find nearby drivers.
 */
public class QuadTree {
    private static final int MAX_CAPACITY = 50;
    
    private final Boundary boundary;
    private final List<Location> points;
    private QuadTree[] children;

    public QuadTree(Boundary boundary) {
        this.boundary = boundary;
        this.points = new ArrayList<>();
    }

    public boolean insert(Location point) {
        if (!boundary.contains(point)) {
            return false;
        }

        if (points.size() < MAX_CAPACITY && children == null) {
            points.add(point);
            return true;
        }

        if (children == null) {
            subdivide();
        }

        for (QuadTree child : children) {
            if (child.insert(point)) {
                return true;
            }
        }
        return false;
    }

    public List<Location> queryRadius(Location center, double radiusKm) {
        List<Location> result = new ArrayList<>();
        
        if (!boundary.intersectsCircle(center, radiusKm)) {
            return result;
        }

        for (Location point : points) {
            if (point.distanceTo(center) <= radiusKm) {
                result.add(point);
            }
        }

        if (children != null) {
            for (QuadTree child : children) {
                result.addAll(child.queryRadius(center, radiusKm));
            }
        }
        
        return result;
    }

    private void subdivide() {
        double x = boundary.centerX;
        double y = boundary.centerY;
        double w = boundary.width / 2;
        double h = boundary.height / 2;

        children = new QuadTree[4];
        children[0] = new QuadTree(new Boundary(x - w, y + h, w, h)); // NW
        children[1] = new QuadTree(new Boundary(x + w, y + h, w, h)); // NE
        children[2] = new QuadTree(new Boundary(x - w, y - h, w, h)); // SW
        children[3] = new QuadTree(new Boundary(x + w, y - h, w, h)); // SE
    }

    // Stub classes for demonstration
    static class Location {
        double lat, lon;
        double distanceTo(Location other) { return 0; /* Haversine formula */ }
    }
    static class Boundary {
        double centerX, centerY, width, height;
        Boundary(double cx, double cy, double w, double h) { centerX=cx; centerY=cy; width=w; height=h; }
        boolean contains(Location p) { return true; }
        boolean intersectsCircle(Location center, double radius) { return true; }
    }
}
