# Sombrero JavaFX

Animated spinning ripple surface rendered with JavaFX.

The application samples a cosine-based height field that decays with distance, spins the mesh about the Z axis, then applies a presentation tilt for projection. Quads are depth-sorted for hidden-line removal, and edges are colorized via HSV using height-based amplitude to map low-to-high elevations across the spectrum.

![](image.png)

## How to run in local

```bash
./mvnw clean compile exec:java
```

## References

- https://github.com/davepl/sombrero
- https://openjfx.io/
