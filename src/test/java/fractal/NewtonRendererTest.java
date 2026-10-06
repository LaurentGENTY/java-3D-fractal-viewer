package fractal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NewtonRendererTest {

    private static final double SQRT3_2 = Math.sqrt(3) / 2;

    @Test
    void pointOnARootConvergesImmediately() {
        assertEquals(new NewtonRenderer.Convergence(0, 0), NewtonRenderer.converge(1, 0, 50));
        assertEquals(1, NewtonRenderer.converge(-0.5, SQRT3_2, 50).root());
        assertEquals(2, NewtonRenderer.converge(-0.5, -SQRT3_2, 50).root());
    }

    @Test
    void pointNearOneConvergesToRootZero() {
        NewtonRenderer.Convergence c = NewtonRenderer.converge(2, 0, 50);
        assertEquals(0, c.root());
    }

    @Test
    void originDoesNotConvergeAndDoesNotThrow() {
        // z^2 = 0: the Newton step divides by zero.
        assertEquals(-1, NewtonRenderer.converge(0, 0, 50).root());
    }

    @Test
    void zeroIterationsGivesAnAllBlackImage() {
        // Center offset by 0.01 so no pixel lands exactly on a root.
        Viewport v = Viewport.of(1, 0.01, 0.01, 40, 20);
        for (int argb : NewtonRenderer.render(v, 0, Palette.NEON)) {
            assertEquals(0xFF000000, argb);
        }
    }

    @Test
    void parallelRenderMatchesPerPixelComputation() {
        Viewport v = Viewport.of(1, 0, 0, 64, 32);
        int[] img = NewtonRenderer.render(v, 50, Palette.NEON);
        int[] expected = new int[v.width() * v.height()];
        for (int row = 0; row < v.height(); row++) {
            for (int col = 0; col < v.width(); col++) {
                expected[row * v.width() + col] =
                        NewtonRenderer.colorFor(NewtonRenderer.converge(v.re(col), v.im(row), 50), Palette.NEON);
            }
        }
        assertArrayEquals(expected, img);
    }
}
