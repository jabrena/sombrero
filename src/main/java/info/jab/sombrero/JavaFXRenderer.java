package info.jab.sombrero;

import info.jab.sombrero.ColorUtils;
import info.jab.sombrero.Point;
import info.jab.sombrero.Quad;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX-specific renderer for the ripple surface.
 */
public class JavaFXRenderer {

    private static final Color BG_COLOR = Color.rgb(10, 12, 18);
    private static final int MAX_CACHED_COLORS = 1024;

    private final GraphicsContext gc;
    private final List<Quad> quads = new ArrayList<>();

    // Reusable arrays to avoid allocations per frame
    private final double[] cachedX = new double[4];
    private final double[] cachedY = new double[4];
    private final double[] cachedAmp = new double[4];

    // Color cache to avoid creating new Color objects
    private final Color[] colorCache = new Color[MAX_CACHED_COLORS];

    public JavaFXRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    public void clear(double width, double height) {
        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, width, height);
    }

    public void drawHiddenLineMesh(Point[][] points, double heightScale) {
        int rows = points.length;
        int cols = rows > 0 ? points[0].length : 0;

        quads.clear();
        int quadIndex = 0;

        // Build quads list
        for (int i = 0; i < rows - 1; i++) {
            for (int j = 0; j < cols - 1; j++) {
                Point p00 = points[i][j];
                Point p10 = points[i + 1][j];
                Point p11 = points[i + 1][j + 1];
                Point p01 = points[i][j + 1];

                if (p00 == null || !p00.valid() || p10 == null || !p10.valid() ||
                    p11 == null || !p11.valid() || p01 == null || !p01.valid()) {
                    continue;
                }

                double avgDepth = (p00.depth() + p10.depth() + p11.depth() + p01.depth()) * 0.25;

                // Reuse or create quad
                Quad quad;
                if (quadIndex < quads.size()) {
                    quad = quads.get(quadIndex);
                } else {
                    quad = new Quad();
                    quads.add(quad);
                }
                quad.set(avgDepth, p00, p10, p11, p01);
                quadIndex++;
            }
        }

        // Trim quads list if needed - use subList for O(1) operation
        if (quads.size() > quadIndex) {
            quads.subList(quadIndex, quads.size()).clear();
        }

        // Sort far to near (painter's algorithm) - use insertion sort for mostly sorted data
        insertionSortQuads(quads, quadIndex);

        // Draw quads and edges
        // Cache point values to avoid repeated memory segment reads
        for (Quad quad : quads) {
            Point[] corners = quad.corners;

            // Cache all point values once
            for (int i = 0; i < 4; i++) {
                cachedX[i] = corners[i].x();
                cachedY[i] = corners[i].y();
                cachedAmp[i] = corners[i].amplitude();
            }

            // Fill quad with background color - reuse arrays
            gc.setFill(BG_COLOR);
            gc.fillPolygon(cachedX, cachedY, 4);

            // Draw edges with colorized lines - use cached values
            for (int edge = 0; edge < 4; edge++) {
                int nextEdge = (edge + 1) % 4;
                double avgAmp = (cachedAmp[edge] + cachedAmp[nextEdge]) * 0.5;
                double hue = Math.min(1.0, avgAmp / heightScale);

                // Use cached color if possible
                Color color = getCachedColor(hue);
                gc.setStroke(color);
                gc.strokeLine(cachedX[edge], cachedY[edge], cachedX[nextEdge], cachedY[nextEdge]);
            }
        }
    }

    private Color getCachedColor(double hue) {
        // Clamp hue to [0, 1]
        hue = Math.max(0.0, Math.min(1.0, hue));

        // Map hue to cache index
        int index = (int) (hue * MAX_CACHED_COLORS);
        if (index >= MAX_CACHED_COLORS) index = MAX_CACHED_COLORS - 1;

        // Get or create cached color
        Color cached = colorCache[index];
        if (cached == null) {
            double[] rgb = ColorUtils.hsvToRgb(hue, 1.0, 1.0);
            cached = Color.color(rgb[0], rgb[1], rgb[2]);
            colorCache[index] = cached;
        }
        return cached;
    }

    /**
     * Insertion sort optimized for mostly-sorted data (depth-sorted quads).
     * Much faster than full sort when data is already mostly in order.
     */
    private void insertionSortQuads(List<Quad> quads, int size) {
        for (int i = 1; i < size; i++) {
            Quad current = quads.get(i);
            double currentDepth = current.avgDepth;
            int j = i - 1;

            // Move elements that are closer (smaller depth) to the right
            while (j >= 0 && quads.get(j).avgDepth < currentDepth) {
                quads.set(j + 1, quads.get(j));
                j--;
            }
            quads.set(j + 1, current);
        }
    }
}
