package fractal;

import java.util.stream.IntStream;

/** Newton's method on f(z) = z^3 - 1: pixels are colored by the root they converge to. */
public final class NewtonRenderer {

    public record Convergence(int root, int iterations) {}

    private static final Convergence NOT_CONVERGED = new Convergence(-1, 0);
    private static final double TOLERANCE_SQ = 1e-12; // |z - root| < 1e-6
    private static final double[] ROOTS_RE = {1.0, -0.5, -0.5};
    private static final double[] ROOTS_IM = {0.0, Math.sqrt(3) / 2, -Math.sqrt(3) / 2};
    // Each iteration darkens the basin color, which outlines the fractal boundaries.
    private static final double SHADE_PER_ITERATION = 0.92;

    private NewtonRenderer() {}

    public static Convergence converge(double re, double im, int maxIter) {
        double x = re;
        double y = im;
        for (int n = 0; n <= maxIter; n++) {
            for (int k = 0; k < 3; k++) {
                double dx = x - ROOTS_RE[k];
                double dy = y - ROOTS_IM[k];
                if (dx * dx + dy * dy < TOLERANCE_SQ) {
                    return new Convergence(k, n);
                }
            }
            if (n == maxIter) {
                break;
            }
            // z <- z - (z^3 - 1) / (3 z^2) = (2 z^3 + 1) / (3 z^2)
            double z2Re = x * x - y * y;
            double z2Im = 2 * x * y;
            double denRe = 3 * z2Re;
            double denIm = 3 * z2Im;
            double den = denRe * denRe + denIm * denIm;
            if (den == 0.0) {
                return NOT_CONVERGED;
            }
            double numRe = 2 * (z2Re * x - z2Im * y) + 1;
            double numIm = 2 * (z2Re * y + z2Im * x);
            x = (numRe * denRe + numIm * denIm) / den;
            y = (numIm * denRe - numRe * denIm) / den;
        }
        return NOT_CONVERGED;
    }

    public static int colorFor(Convergence c, Palette p) {
        if (c.root() < 0) {
            return 0xFF000000;
        }
        int base = p.colorAt((c.root() + 0.5) / 3.0);
        double shade = Math.pow(SHADE_PER_ITERATION, c.iterations());
        int r = (int) (((base >> 16) & 0xFF) * shade);
        int g = (int) (((base >> 8) & 0xFF) * shade);
        int b = (int) ((base & 0xFF) * shade);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int[] render(Viewport v, int maxIter, Palette p) {
        int w = v.width();
        int[] argb = new int[w * v.height()];
        IntStream.range(0, v.height()).parallel().forEach(row -> {
            double im = v.im(row);
            for (int col = 0; col < w; col++) {
                argb[row * w + col] = colorFor(converge(v.re(col), im, maxIter), p);
            }
        });
        return argb;
    }
}
