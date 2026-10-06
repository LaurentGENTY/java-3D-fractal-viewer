package modele;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fractal.Palette;
import java.util.concurrent.atomic.AtomicInteger;
import modele.Modele.ListControllers;
import org.junit.jupiter.api.Test;

class ModeleViewStateTest {

    @Test
    void setViewStateAppliesEverythingWithOneNotification() {
        Modele m = new Modele();
        AtomicInteger notifications = new AtomicInteger();
        m.addObserver((o, arg) -> notifications.incrementAndGet());
        ViewState s = new ViewState(ListControllers.NEWTON, Palette.NEON, 77, 0.5, -0.745, 0.11, -0.8, 0.156);

        m.setViewState(s);

        assertEquals(s, m.getViewState());
        assertEquals(1, notifications.get());
        assertEquals(-0.745, m.getViewport().re(1000), 1e-12);
    }

    @Test
    void invalidStatesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ViewState(ListControllers.MANDELBROT, Palette.FIRE, -1, 1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ViewState(ListControllers.MANDELBROT, Palette.FIRE, 10, 0, 0, 0, 0, 0));
    }
}
