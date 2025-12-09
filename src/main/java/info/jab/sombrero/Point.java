package info.jab.sombrero;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Represents a 3D point with screen coordinates, depth, and amplitude.
 * Uses off-heap memory for efficient storage.
 */
public class Point {
    private final MemorySegment segment;
    private final long offset;
    private static final long X_OFFSET = 0;
    private static final long Y_OFFSET = ValueLayout.JAVA_DOUBLE.byteSize();
    private static final long DEPTH_OFFSET = 2 * ValueLayout.JAVA_DOUBLE.byteSize();
    private static final long AMPLITUDE_OFFSET = 3 * ValueLayout.JAVA_DOUBLE.byteSize();
    private static final long VALID_OFFSET = 4 * ValueLayout.JAVA_DOUBLE.byteSize();

    Point(MemorySegment segment, long offset) {
        this.segment = segment;
        this.offset = offset;
    }

    public double x() {
        return segment.get(ValueLayout.JAVA_DOUBLE, offset + X_OFFSET);
    }

    public double y() {
        return segment.get(ValueLayout.JAVA_DOUBLE, offset + Y_OFFSET);
    }

    public double depth() {
        return segment.get(ValueLayout.JAVA_DOUBLE, offset + DEPTH_OFFSET);
    }

    public double amplitude() {
        return segment.get(ValueLayout.JAVA_DOUBLE, offset + AMPLITUDE_OFFSET);
    }

    public boolean valid() {
        return segment.get(ValueLayout.JAVA_BYTE, offset + VALID_OFFSET) != 0;
    }

    void set(double x, double y, double depth, double amplitude) {
        segment.set(ValueLayout.JAVA_DOUBLE, offset + X_OFFSET, x);
        segment.set(ValueLayout.JAVA_DOUBLE, offset + Y_OFFSET, y);
        segment.set(ValueLayout.JAVA_DOUBLE, offset + DEPTH_OFFSET, depth);
        segment.set(ValueLayout.JAVA_DOUBLE, offset + AMPLITUDE_OFFSET, amplitude);
        segment.set(ValueLayout.JAVA_BYTE, offset + VALID_OFFSET, (byte) 1);
    }

    void invalidate() {
        segment.set(ValueLayout.JAVA_BYTE, offset + VALID_OFFSET, (byte) 0);
    }
}
