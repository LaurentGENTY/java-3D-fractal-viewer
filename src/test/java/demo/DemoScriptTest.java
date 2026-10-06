package demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import demo.DemoScript.Clip;
import demo.DemoScript.Frame;
import demo.DemoScript.Still;
import java.util.List;
import modele.Modele.ListControllers;
import org.junit.jupiter.api.Test;

class DemoScriptTest {

    private static Clip clip(String name) {
        return DemoScript.clips().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void clipsHaveExpectedNamesAndLengths() {
        List<Clip> clips = DemoScript.clips();
        assertEquals(List.of("sphere-spin", "deep-zoom", "julia-morph", "tour",
                        "burning-ship-zoom", "newton-zoom", "levy-growth", "square-growth"),
                clips.stream().map(Clip::name).toList());
        assertEquals(List.of(180, 240, 240, 270, 180, 150, 144, 144),
                clips.stream().map(c -> c.frames().size()).toList());
    }

    @Test
    void deepZoomGoesStrictlyDeeperWithEnoughIterations() {
        List<Frame> frames = clip("deep-zoom").frames();
        for (int i = 1; i < frames.size(); i++) {
            assertTrue(frames.get(i).state().zoom() < frames.get(i - 1).state().zoom(), "frame " + i);
        }
        Frame last = frames.get(frames.size() - 1);
        assertEquals(1e-4, last.state().zoom(), 1e-12);
        // Too few iterations at depth turns the whole sphere black.
        assertTrue(last.state().itMax() >= 600, "itMax = " + last.state().itMax());
    }

    @Test
    void juliaMorphStaysOnTheCircle() {
        for (Frame f : clip("julia-morph").frames()) {
            assertEquals(ListControllers.JULIA, f.state().fractal());
            double r = Math.hypot(f.state().juliaRe(), f.state().juliaIm());
            assertEquals(0.7885, r, 1e-9);
        }
    }

    @Test
    void tourVisitsAllSixFractalsInOrder() {
        List<ListControllers> visited = clip("tour").frames().stream()
                .map(f -> f.state().fractal()).distinct().toList();
        assertEquals(List.of(ListControllers.MANDELBROT, ListControllers.JULIA, ListControllers.BURNING_SHIP,
                ListControllers.NEWTON, ListControllers.LEVY, ListControllers.SQUARE), visited);
    }

    @Test
    void zoomAndMorphAreFlatWhileSpinAndTourShowTheSphere() {
        assertEquals(List.of(DemoScript.View.SPHERE, DemoScript.View.FLAT, DemoScript.View.FLAT, DemoScript.View.SPHERE,
                        DemoScript.View.FLAT, DemoScript.View.FLAT, DemoScript.View.FLAT, DemoScript.View.FLAT),
                DemoScript.clips().stream().map(Clip::view).toList());
    }

    @Test
    void newZoomClipsGoStrictlyDeeper() {
        for (String name : List.of("burning-ship-zoom", "newton-zoom")) {
            List<Frame> frames = clip(name).frames();
            for (int i = 1; i < frames.size(); i++) {
                assertTrue(frames.get(i).state().zoom() < frames.get(i - 1).state().zoom(), name + " frame " + i);
            }
        }
    }

    @Test
    void growthClipsStepThroughEveryRecursionLevel() {
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
                clip("levy-growth").frames().stream().map(f -> f.state().itMax()).distinct().toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6),
                clip("square-growth").frames().stream().map(f -> f.state().itMax()).distinct().toList());
    }

    @Test
    void stillsAreFlat() {
        for (Still s : DemoScript.stills()) {
            assertEquals(DemoScript.View.FLAT, s.view(), s.name());
        }
    }

    @Test
    void oneStillPerFractal() {
        assertEquals(List.of("mandelbrot", "julia", "burning-ship", "newton", "levy", "square"),
                DemoScript.stills().stream().map(Still::name).toList());
    }
}
