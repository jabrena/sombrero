package com.sombrero;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 2025-12-08
 * Author: Ported from Python/Pygame to JavaFX
 * Description: Animated spinning ripple surface rendered with JavaFX. It samples a
 * cosine-based height field that decays with distance, spins the mesh about the Z
 * axis, then applies a presentation tilt for projection. Quads are depth-sorted
 * for hidden-line removal, and edges are colorized via HSV using height-based
 * amplitude to map low-to-high elevations across the spectrum.
 */
public class SombreroApp extends Application {
    
    private static final int WIDTH = 960;
    private static final int HEIGHT = 720;
    private static final int FPS = 60;
    private static final double FOV = 700.0;
    private static final double VIEWER_DISTANCE = 90.0;
    
    private static final int GRID_RADIUS = 28;
    private static final int GRID_STEP = 1;
    private static final double BASE_TILT_X = Math.toRadians(55);
    private static final double SPIN_RATE = 0.25;
    
    private static final double RIPPLE_FREQ = 0.65;
    private static final double RIPPLE_SPEED = 4.0;
    private static final double RIPPLE_DECAY = 0.015;
    private static final double HEIGHT_SCALE = 20.0;
    private static final double AMPLITUDE_FALLOFF = 0.06;
    
    private static final Color BG_COLOR = Color.rgb(10, 12, 18);
    
    private Canvas canvas;
    private GraphicsContext gc;
    private long startTime;
    private boolean running = true;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Spinning Ripple Surface");
        
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                running = false;
                primaryStage.close();
            }
        });
        
        primaryStage.setOnCloseRequest(e -> running = false);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        
        startTime = System.nanoTime();
        
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!running) {
                    stop();
                    return;
                }
                
                double t = (now - startTime) / 1_000_000_000.0;
                render(t);
            }
        };
        timer.start();
    }
    
    private double rippleHeight(double x, double y, double t) {
        double r = Math.hypot(x, y);
        double wave = Math.cos(r * RIPPLE_FREQ - t * RIPPLE_SPEED);
        double envelope = Math.exp(-r * RIPPLE_DECAY);
        double amplitude = HEIGHT_SCALE / (1.0 + AMPLITUDE_FALLOFF * r);
        return wave * envelope * amplitude;
    }
    
    private Color hsvToRgb(double h, double s, double v) {
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
        
        return Color.color(r, g, b);
    }
    
    private static class Point {
        final double x, y, depth, amplitude;
        
        Point(double x, double y, double depth, double amplitude) {
            this.x = x;
            this.y = y;
            this.depth = depth;
            this.amplitude = amplitude;
        }
    }
    
    private List<List<Point>> generateGridPoints(double t) {
        List<List<Point>> points = new ArrayList<>();
        double cosx = Math.cos(BASE_TILT_X);
        double sinx = Math.sin(BASE_TILT_X);
        double spinAngle = t * SPIN_RATE;
        double cosz = Math.cos(spinAngle);
        double sinz = Math.sin(spinAngle);
        
        for (int x = -GRID_RADIUS; x <= GRID_RADIUS; x += GRID_STEP) {
            List<Point> row = new ArrayList<>();
            for (int y = -GRID_RADIUS; y <= GRID_RADIUS; y += GRID_STEP) {
                double z = rippleHeight(x, y, t);
                
                // Spin around Z axis
                double xSpin = x * cosz - y * sinz;
                double ySpin = x * sinz + y * cosz;
                double zSpin = z;
                
                // Presentation tilt
                double yTilt = ySpin * cosx - zSpin * sinx;
                double zTilt = ySpin * sinx + zSpin * cosx;
                
                double depth = VIEWER_DISTANCE + zTilt;
                if (depth <= 0.1) {
                    row.add(null);
                    continue;
                }
                
                double factor = FOV / depth;
                double px = xSpin * factor + WIDTH / 2.0;
                double py = -yTilt * factor + HEIGHT / 2.0;
                double amp = Math.abs(z);
                
                row.add(new Point(px, py, depth, amp));
            }
            points.add(row);
        }
        return points;
    }
    
    private static class Quad {
        final double avgDepth;
        final Point[] corners;
        
        Quad(double avgDepth, Point[] corners) {
            this.avgDepth = avgDepth;
            this.corners = corners;
        }
    }
    
    private void drawHiddenLineMesh(List<List<Point>> points) {
        int rows = points.size();
        int cols = points.isEmpty() ? 0 : points.get(0).size();
        
        List<Quad> quads = new ArrayList<>();
        
        for (int i = 0; i < rows - 1; i++) {
            for (int j = 0; j < cols - 1; j++) {
                Point p00 = points.get(i).get(j);
                Point p10 = points.get(i + 1).get(j);
                Point p11 = points.get(i + 1).get(j + 1);
                Point p01 = points.get(i).get(j + 1);
                
                if (p00 == null || p10 == null || p11 == null || p01 == null) {
                    continue;
                }
                
                double avgDepth = (p00.depth + p10.depth + p11.depth + p01.depth) / 4.0;
                Point[] corners = {p00, p10, p11, p01};
                quads.add(new Quad(avgDepth, corners));
            }
        }
        
        // Sort far to near (painter's algorithm)
        quads.sort((a, b) -> Double.compare(b.avgDepth, a.avgDepth));
        
        // Draw quads and edges
        for (Quad quad : quads) {
            Point[] corners = quad.corners;
            
            // Fill quad with background color
            double[] xPoints = new double[4];
            double[] yPoints = new double[4];
            for (int i = 0; i < 4; i++) {
                xPoints[i] = corners[i].x;
                yPoints[i] = corners[i].y;
            }
            gc.setFill(BG_COLOR);
            gc.fillPolygon(xPoints, yPoints, 4);
            
            // Draw edges with colorized lines
            int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}};
            for (int[] edge : edges) {
                Point a = corners[edge[0]];
                Point b = corners[edge[1]];
                double avgAmp = (a.amplitude + b.amplitude) * 0.5;
                double hue = Math.min(1.0, avgAmp / HEIGHT_SCALE);
                Color color = hsvToRgb(hue, 1.0, 1.0);
                gc.setStroke(color);
                gc.strokeLine(a.x, a.y, b.x, b.y);
            }
        }
    }
    
    private void render(double t) {
        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        List<List<Point>> points = generateGridPoints(t);
        drawHiddenLineMesh(points);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
