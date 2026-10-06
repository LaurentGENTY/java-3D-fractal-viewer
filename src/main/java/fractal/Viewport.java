package fractal;

/** Maps pixel coordinates to the complex plane. Pixels are square; {@code scale} is complex units per pixel. */
public record Viewport(double centerRe, double centerIm, double scale, int width, int height) {

    /** At zoom 1 the image is 4 units wide, as in the 2017 version. Smaller zoom = closer. */
    public static Viewport of(double zoom, double centerRe, double centerIm, int width, int height) {
        return new Viewport(centerRe, centerIm, 4.0 * zoom / width, width, height);
    }

    public double re(int col) {
        return centerRe + (col - width / 2.0) * scale;
    }

    public double im(int row) {
        return centerIm + (row - height / 2.0) * scale;
    }
}
