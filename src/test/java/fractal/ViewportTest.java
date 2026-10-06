package fractal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ViewportTest {

    @Test
    void zoomOneMatchesThe2017Framing() {
        // 2017: c_re = (col - W/2) * 4 / W, c_im = (row - H/2) * 4 / W
        Viewport v = Viewport.of(1.0, 0, 0, 2000, 1000);
        assertEquals(0.002, v.scale(), 1e-12);
        assertEquals(-2.0, v.re(0), 1e-12);
        assertEquals(0.0, v.re(1000), 1e-12);
        assertEquals(-1.0, v.im(0), 1e-12);
        assertEquals(0.0, v.im(500), 1e-12);
    }

    @Test
    void centerAndZoomAreApplied() {
        Viewport v = Viewport.of(0.5, -0.745, 0.11, 2000, 1000);
        assertEquals(-0.745, v.re(1000), 1e-12);
        assertEquals(0.11, v.im(500), 1e-12);
        assertEquals(-0.745 - 1.0, v.re(0), 1e-12);
    }
}
