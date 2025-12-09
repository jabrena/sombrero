package info.jab.sombrero;

/**
 * Represents a quadrilateral face of the mesh.
 */
public class Quad {
    public double avgDepth;
    public final Point[] corners;

    public Quad() {
        this.corners = new Point[4];
    }

    public void set(double avgDepth, Point p00, Point p10, Point p11, Point p01) {
        this.avgDepth = avgDepth;
        this.corners[0] = p00;
        this.corners[1] = p10;
        this.corners[2] = p11;
        this.corners[3] = p01;
    }
}
