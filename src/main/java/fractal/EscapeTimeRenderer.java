package fractal;

import java.util.stream.IntStream;

/** Escape-time fractals of the z -> z^2 + c family, with smooth coloring. */
public final class EscapeTimeRenderer {

    public enum Formula { MANDELBROT, JULIA, BURNING_SHIP }

    public static final int INTERIOR = 0xFF000000;

    // A large bailout radius keeps the smooth-coloring formula accurate.
    private static final double BAILOUT_SQ = 256.0 * 256.0;
    private static final double ITERATIONS_PER_CYCLE = 64.0;
    private static final double LOG2 = Math.log(2.0);

    private EscapeTimeRenderer() {}

    /**
     * Smooth (fractional) iteration count at point (re, im), or -1 if the orbit stays bounded for
     * maxIter iterations. For JULIA the point is z0 and (cRe, cIm) is c; otherwise the point is c.
     */
    public static double smoothIterations(Formula f, double re, double im, double cRe, double cIm, int maxIter) {
        boolean julia = f == Formula.JULIA;
        double x = julia ? re : 0;
        double y = julia ? im : 0;
        double addRe = julia ? cRe : re;
        double addIm = julia ? cIm : im;
        for (int n = 1; n <= maxIter; n++) {
            if (f == Formula.BURNING_SHIP) {
                x = Math.abs(x);
                y = Math.abs(y);
            }
            double nx = x * x - y * y + addRe;
            y = 2 * x * y + addIm;
            x = nx;
            double mod2 = x * x + y * y;
            if (mod2 > BAILOUT_SQ) {
                // mu = n + 1 - log2(ln|z|); clamped because very fast escapes go negative.
                return Math.max(0.0, n + 1 - Math.log(0.5 * Math.log(mod2)) / LOG2);
            }
        }
        return -1;
    }

    public static int colorFor(double mu, Palette p) {
        return mu < 0 ? INTERIOR : p.colorAt(mu / ITERATIONS_PER_CYCLE);
    }

    public static int[] render(Formula f, Viewport v, int maxIter, double cRe, double cIm, Palette p) {
        int w = v.width();
        int[] argb = new int[w * v.height()];
        IntStream.range(0, v.height()).parallel().forEach(row -> {
            double im = v.im(row);
            for (int col = 0; col < w; col++) {
                argb[row * w + col] = colorFor(smoothIterations(f, v.re(col), im, cRe, cIm, maxIter), p);
            }
        });
        return argb;
    }
}
