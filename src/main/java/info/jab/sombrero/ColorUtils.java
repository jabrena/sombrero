package info.jab.sombrero;

/**
 * Utility class for color conversions.
 * Platform-agnostic - returns RGB values as doubles [0.0, 1.0].
 */
public class ColorUtils {

    private static final int COLOR_CACHE_SIZE = 256;
    private static final double[][] colorCache = new double[COLOR_CACHE_SIZE][3];

    static {
        // Initialize color cache
        for (int i = 0; i < COLOR_CACHE_SIZE; i++) {
            double hue = i / (double) COLOR_CACHE_SIZE;
            double[] rgb = hsvToRgbUncached(hue, 1.0, 1.0);
            colorCache[i] = rgb;
        }
    }

    /**
     * Converts HSV to RGB.
     * @param h hue [0.0, 1.0]
     * @param s saturation [0.0, 1.0]
     * @param v value [0.0, 1.0]
     * @return RGB array [r, g, b] with values [0.0, 1.0]
     * Note: For cached values, returns the cached array directly (read-only).
     * For uncached values, returns a new array.
     */
    public static double[] hsvToRgb(double h, double s, double v) {
        // Use cache for common cases - return cached array directly (read-only)
        if (s == 1.0 && v == 1.0 && h >= 0.0 && h <= 1.0) {
            int index = (int) (h * COLOR_CACHE_SIZE);
            if (index >= COLOR_CACHE_SIZE) index = COLOR_CACHE_SIZE - 1;
            return colorCache[index]; // No clone - cache is read-only
        }
        return hsvToRgbUncached(h, s, v);
    }

    private static double[] hsvToRgbUncached(double h, double s, double v) {
        // Clamp h to [0, 1]
        h = Math.max(0.0, Math.min(1.0, h));

        int i = (int) (h * 6);
        double f = h * 6 - i;
        double p = v * (1 - s);
        double q = v * (1 - f * s);
        double t = v * (1 - (1 - f) * s);

        double r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5: r = v; g = p; b = q; break;
            default: r = g = b = 0; break;
        }

        return new double[]{r, g, b};
    }
}
