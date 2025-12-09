package info.jab.sombrero;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Core engine for calculating ripple surface geometry.
 * No UI dependencies - pure computation.
 */
public class RippleSurface {

    private static final double BASE_FOV = 700.0;
    private static final double VIEWER_DISTANCE = 90.0;
    private static final double BASE_TILT_X = Math.toRadians(55);
    private static final double SPIN_RATE = 0.25;
    private static final double RIPPLE_FREQ = 0.65;
    private static final double RIPPLE_SPEED = 4.0;
    private static final double RIPPLE_DECAY = 0.015;
    private static final double HEIGHT_SCALE = 20.0;
    private static final double AMPLITUDE_FALLOFF = 0.06;

    // Pre-calculated constants
    private static final double COSX = Math.cos(BASE_TILT_X);
    private static final double SINX = Math.sin(BASE_TILT_X);

    // Point structure: 4 doubles (32 bytes) + 1 byte (valid) + 7 bytes padding = 40 bytes
    private static final long POINT_SIZE = 5 * ValueLayout.JAVA_DOUBLE.byteSize();

    private int gridRadius = 20;
    private int gridStep = 1;
    private double fov = BASE_FOV;
    private double canvasWidth;
    private double canvasHeight;

    private Point[][] pointsArray;
    private Arena offHeapArena;
    private MemorySegment pointDataSegment;

    public RippleSurface(double canvasWidth, double canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        rebuildGrid();
    }

    public void setCanvasSize(double width, double height) {
        this.canvasWidth = width;
        this.canvasHeight = height;
    }

    public void setFov(double fov) {
        this.fov = fov;
    }

    public double getFov() {
        return fov;
    }

    private int getGridSize() {
        return (gridRadius * 2 / gridStep) + 1;
    }

    private void rebuildGrid() {
        int gridSize = getGridSize();
        long totalPoints = (long) gridSize * gridSize;

        // Close previous arena if it exists
        if (offHeapArena != null && offHeapArena.scope().isAlive()) {
            offHeapArena.close();
        }

        // Allocate off-heap memory for all points
        offHeapArena = Arena.ofAuto();
        long totalSize = totalPoints * POINT_SIZE;
        pointDataSegment = offHeapArena.allocate(totalSize);

        // Rebuild points array with off-heap references
        pointsArray = new Point[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                long pointIndex = (long) i * gridSize + j;
                long pointOffset = pointIndex * POINT_SIZE;
                pointsArray[i][j] = new Point(pointDataSegment, pointOffset);
            }
        }
    }

    private double rippleHeight(double x, double y, double t) {
        // Optimize: use x*x + y*y to avoid sqrt in hypot, then sqrt only once
        double rSquared = x * x + y * y;
        double r = Math.sqrt(rSquared);
        double wave = Math.cos(r * RIPPLE_FREQ - t * RIPPLE_SPEED);
        double envelope = Math.exp(-r * RIPPLE_DECAY);
        double amplitude = HEIGHT_SCALE / (1.0 + AMPLITUDE_FALLOFF * r);
        return wave * envelope * amplitude;
    }

    public Point[][] generateGridPoints(double t) {
        double spinAngle = t * SPIN_RATE;
        double cosz = Math.cos(spinAngle);
        double sinz = Math.sin(spinAngle);

        // Pre-calculate constants to avoid repeated division
        double halfWidth = canvasWidth * 0.5;
        double halfHeight = canvasHeight * 0.5;

        int rowIdx = 0;
        int rows = pointsArray.length;
        for (int x = -gridRadius; x <= gridRadius; x += gridStep) {
            int colIdx = 0;
            int cols = rows > 0 ? pointsArray[0].length : 0;
            for (int y = -gridRadius; y <= gridRadius; y += gridStep) {
                double z = rippleHeight(x, y, t);

                // Spin around Z axis (automatic animation)
                double xSpin = x * cosz - y * sinz;
                double ySpin = x * sinz + y * cosz;
                double zSpin = z;

                // Presentation tilt (base tilt) - no mouse rotation
                double yTilt = ySpin * COSX - zSpin * SINX;
                double zTilt = ySpin * SINX + zSpin * COSX;

                double depth = VIEWER_DISTANCE + zTilt;
                // Optimize bounds checking - rowIdx and colIdx are always valid in this loop
                if (rowIdx < rows && colIdx < cols) {
                    Point p = pointsArray[rowIdx][colIdx];
                    if (depth <= 0.01) {
                        p.invalidate();
                    } else {
                        double factor = fov / depth;
                        double px = xSpin * factor + halfWidth;
                        double py = -yTilt * factor + halfHeight;
                        double amp = Math.abs(z);
                        p.set(px, py, depth, amp);
                    }
                }
                colIdx++;
            }
            rowIdx++;
        }
        return pointsArray;
    }

    public double getHeightScale() {
        return HEIGHT_SCALE;
    }

    public void cleanup() {
        if (offHeapArena != null && offHeapArena.scope().isAlive()) {
            try {
                offHeapArena.close();
            } catch (Exception ex) {
                // Ignore errors during cleanup
            }
        }
    }
}
