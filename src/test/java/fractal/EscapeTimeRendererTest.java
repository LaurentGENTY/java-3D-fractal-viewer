package fractal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fractal.EscapeTimeRenderer.Formula;
import org.junit.jupiter.api.Test;

class EscapeTimeRendererTest {

    @Test
    void mandelbrotOriginIsInterior() {
        assertEquals(-1, EscapeTimeRenderer.smoothIterations(Formula.MANDELBROT, 0, 0, 0, 0, 1000));
    }

    @Test
    void mandelbrotOnePlusIEscapesFast() {
        // Orbit: 1+i, 1+3i, -7+7i, 1-97i, then |z| ~ 9400 > 256 at n = 5.
        double mu = EscapeTimeRenderer.smoothIterations(Formula.MANDELBROT, 1, 1, 0, 0, 1000);
        assertTrue(mu >= 0 && mu <= 6, "mu = " + mu);
    }

    @Test
    void burningShipRealAntennaIsInterior() {
        // Stays on the real axis: x -> x^2 - 1.75 is bounded.
        assertEquals(-1, EscapeTimeRenderer.smoothIterations(Formula.BURNING_SHIP, -1.75, 0, 0, 0, 1000));
    }

    @Test
    void burningShipFarPointEscapesFast() {
        // Orbit: 3+3i, 3+21i, -429+129i (> 256) at n = 3.
        double mu = EscapeTimeRenderer.smoothIterations(Formula.BURNING_SHIP, 3, 3, 0, 0, 1000);
        assertTrue(mu >= 0 && mu <= 4, "mu = " + mu);
    }

    @Test
    void juliaUsesThePixelAsStartingPoint() {
        // c = 0: z -> z^2, bounded inside the unit disc, escapes outside.
        assertEquals(-1, EscapeTimeRenderer.smoothIterations(Formula.JULIA, 0.5, 0, 0, 0, 1000));
        assertTrue(EscapeTimeRenderer.smoothIterations(Formula.JULIA, 2, 0, 0, 0, 1000) >= 0);
    }

    @Test
    void veryFastEscapeIsColoredNotInterior() {
        // Escapes at n = 1 with a huge modulus: the raw smooth count would be negative.
        double mu = EscapeTimeRenderer.smoothIterations(Formula.JULIA, 1000, 0, 0, 0, 1000);
        assertTrue(mu >= 0, "mu = " + mu);
        Viewport v = new Viewport(1000, 0, 1, 1, 1);
        int[] img = EscapeTimeRenderer.render(Formula.JULIA, v, 1000, 0, 0, Palette.OCEAN);
        assertNotEquals(EscapeTimeRenderer.INTERIOR, img[0]);
    }

    @Test
    void zeroIterationsGivesAnAllInteriorImage() {
        Viewport v = Viewport.of(1, 0, 0, 40, 20);
        for (Formula f : Formula.values()) {
            int[] img = EscapeTimeRenderer.render(f, v, 0, 0.3, -0.5, Palette.FIRE);
            for (int argb : img) {
                assertEquals(EscapeTimeRenderer.INTERIOR, argb, f.name());
            }
        }
    }

    @Test
    void parallelRenderMatchesPerPixelComputation() {
        Viewport v = Viewport.of(1, -0.5, 0, 64, 32);
        for (Formula f : Formula.values()) {
            int[] img = EscapeTimeRenderer.render(f, v, 100, 0.3, -0.5, Palette.NEON);
            int[] expected = new int[v.width() * v.height()];
            for (int row = 0; row < v.height(); row++) {
                for (int col = 0; col < v.width(); col++) {
                    double mu = EscapeTimeRenderer.smoothIterations(f, v.re(col), v.im(row), 0.3, -0.5, 100);
                    expected[row * v.width() + col] = EscapeTimeRenderer.colorFor(mu, Palette.NEON);
                }
            }
            assertArrayEquals(expected, img, f.name());
        }
    }
}
