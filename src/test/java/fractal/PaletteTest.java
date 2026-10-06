package fractal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PaletteTest {

    @Test
    void firstStopIsReturnedAtZero() {
        assertEquals(0xFF000000, Palette.FIRE.colorAt(0));
        assertEquals(0xFF000814, Palette.OCEAN.colorAt(0));
        assertEquals(0xFF0D0221, Palette.NEON.colorAt(0));
    }

    @Test
    void classicStartsBlackLikeThe2017Formula() {
        // 2017: Color.hsb(i / 4, 1, i / (i + 5)) -> brightness 0 at i = 0
        assertEquals(0xFF000000, Palette.CLASSIC.colorAt(0));
    }

    @Test
    void samplingIsCyclic() {
        for (Palette p : Palette.values()) {
            assertEquals(p.colorAt(0), p.colorAt(1.0), p.name());
            assertEquals(p.colorAt(0.75), p.colorAt(-0.25), p.name());
            assertEquals(p.colorAt(0.3), p.colorAt(7.3), p.name());
        }
    }

    @Test
    void outOfRangeInputsNeverThrow() {
        for (Palette p : Palette.values()) {
            assertDoesNotThrow(() -> p.colorAt(Double.NaN));
            assertDoesNotThrow(() -> p.colorAt(1e12 + 0.5));
            assertDoesNotThrow(() -> p.colorAt(-1e12 - 0.5));
            assertDoesNotThrow(() -> p.colorAt(Math.nextDown(1.0)));
        }
    }

    @Test
    void everyColorIsOpaque() {
        for (Palette p : Palette.values()) {
            for (int i = 0; i < 1000; i++) {
                assertEquals(0xFF, p.colorAt(i / 1000.0) >>> 24, p.name() + " at " + i);
            }
        }
    }
}
