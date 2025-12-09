# Sombrero JavaFX

Animated spinning ripple surface rendered with JavaFX. This is a JavaFX port of the Python/Pygame implementation from [davepl/sombrero](https://github.com/davepl/sombrero).

The application samples a cosine-based height field that decays with distance, spins the mesh about the Z axis, then applies a presentation tilt for projection. Quads are depth-sorted for hidden-line removal, and edges are colorized via HSV using height-based amplitude to map low-to-high elevations across the spectrum.

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Building

To compile the project:

```bash
mvn clean compile
```

## Running

### Using Maven Exec Plugin

The easiest way to run the application is using the Maven Exec plugin:

```bash
mvn exec:java
```

This will automatically compile the project (if needed) and launch the JavaFX application.

### Alternative: Running with explicit JavaFX module path

If you need to run it directly with Java, you'll need to ensure JavaFX modules are available:

```bash
mvn clean package
java --module-path <path-to-javafx-sdk>/lib --add-modules javafx.controls -cp target/classes com.sombrero.SombreroApp
```

However, the Maven Exec plugin handles this automatically, so `mvn exec:java` is recommended.

## Controls

- **ESC**: Exit the application
- **Close Window**: Click the window close button to exit

## Project Structure

```
.
├── pom.xml                          # Maven project configuration
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── sombrero/
│                   └── SombreroApp.java  # Main application class
└── README.md                        # This file
```

## Technical Details

- **Window Size**: 960x720 pixels
- **Frame Rate**: 60 FPS (target)
- **Grid Resolution**: 28x28 units with 1-unit step
- **Rendering**: Hidden-line removal using painter's algorithm (depth sorting)
- **Colorization**: HSV color space mapped from height amplitude

## License

See LICENSE file for details.
