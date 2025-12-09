package info.jab.sombrero;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX application for rendering the ripple surface.
 * This class handles only UI concerns - all core logic is in RippleSurface.
 */
public class SombreroApp extends Application {

    private static final int DEFAULT_WIDTH = 960;
    private static final int DEFAULT_HEIGHT = 720;
    private static final int FPS = 60;
    private static final double FRAME_TIME_NS = 1_000_000_000.0 / FPS;

    private Canvas canvas;
    private JavaFXRenderer renderer;
    private RippleSurface surface;
    private long startTime;
    private long lastFrameTime;
    private boolean running = true;
    private AnimationTimer timer;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Spinning Ripple Surface");

        canvas = new Canvas(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        renderer = new JavaFXRenderer(canvas.getGraphicsContext2D());
        surface = new RippleSurface(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // Main layout - just the canvas
        StackPane root = new StackPane(canvas);

        // Bind canvas size to container (full width and height)
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                if (primaryStage.isFullScreen()) {
                    primaryStage.setFullScreen(false);
                } else {
                    // Close the window when ESC is pressed
                    primaryStage.close();
                }
            } else if (e.getCode() == KeyCode.F11) {
                // Toggle fullscreen
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
            }
        });

        primaryStage.setOnCloseRequest(e -> {
            // Do cleanup but don't prevent the default close behavior
            running = false;
            if (timer != null) {
                timer.stop();
            }
            surface.cleanup();
            // Exit JavaFX application after cleanup
            Platform.exit();
        });

        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(DEFAULT_WIDTH);
        primaryStage.setMinHeight(DEFAULT_HEIGHT);

        // Handle fullscreen changes
        primaryStage.fullScreenProperty().addListener((obs, wasFullscreen, isNowFullscreen) -> {
            if (!isNowFullscreen) {
                // Exited fullscreen - restore window size
                primaryStage.setWidth(DEFAULT_WIDTH);
                primaryStage.setHeight(DEFAULT_HEIGHT);
            }
        });

        primaryStage.show();

        startTime = System.nanoTime();
        lastFrameTime = startTime;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!running) {
                    stop();
                    return;
                }

                // Frame rate limiting: only render if enough time has passed
                if (now - lastFrameTime >= FRAME_TIME_NS) {
                    // Update canvas size if changed
                    double currentWidth = canvas.getWidth();
                    double currentHeight = canvas.getHeight();
                    if (currentWidth > 0 && currentHeight > 0) {
                        surface.setCanvasSize(currentWidth, currentHeight);
                    }

                    double t = (now - startTime) / 1_000_000_000.0;
                    render(t);
                    lastFrameTime = now;
                }
            }
        };
        timer.start();
    }

    private void render(double t) {
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        renderer.clear(width, height);

        Point[][] points = surface.generateGridPoints(t);
        renderer.drawHiddenLineMesh(points, surface.getHeightScale());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
