package demo;

import fractal.Palette;
import java.util.ArrayList;
import java.util.List;
import modele.Modele.ListControllers;
import modele.ViewState;

/** Frame-by-frame definition of the showcase clips. Pure data: no JavaFX. */
public final class DemoScript {

    /** SPHERE captures the rotating 3D view; FLAT captures the 2D fractal texture itself. */
    public enum View { SPHERE, FLAT }

    public record Frame(ViewState state, double yawDeg, double pitchDeg) {}
    public record Clip(String name, View view, List<Frame> frames) {}
    public record Still(String name, View view, Frame frame) {}

    public static final int FPS = 30;

    // Yaw that brings the texture center (the viewport center) in front of the camera.
    static final double FRONT_YAW_DEG = 0;

    private static final double JULIA_RADIUS = 0.7885;
    private static final double DEEP_ZOOM_TARGET = 1e-4;
    private static final int FRAMES_PER_FRACTAL = 45;
    private static final double SHIP_RE = -1.755;
    private static final double SHIP_IM = -0.02;
    // -2^(-1/3) is mapped onto 0 by the Newton step: all three basins meet there.
    private static final double NEWTON_JUNCTION_RE = -Math.cbrt(0.5);
    private static final int LEVY_MAX_DEPTH = 15;
    private static final int SQUARE_MAX_DEPTH = 6;
    private static final int FRAMES_PER_LEVY_LEVEL = 9;
    private static final int FRAMES_PER_SQUARE_LEVEL = 24;

    private static final ViewState MANDELBROT =
            new ViewState(ListControllers.MANDELBROT, Palette.FIRE, 200, 1.0, -0.5, 0, 0.3, -0.5);
    private static final ViewState JULIA =
            new ViewState(ListControllers.JULIA, Palette.OCEAN, 200, 0.75, 0, 0, -0.8, 0.156);
    private static final ViewState BURNING_SHIP =
            new ViewState(ListControllers.BURNING_SHIP, Palette.FIRE, 300, 0.035, -1.755, -0.02, 0.3, -0.5);
    private static final ViewState NEWTON =
            new ViewState(ListControllers.NEWTON, Palette.NEON, 50, 0.75, 0, 0, 0.3, -0.5);
    private static final ViewState LEVY =
            new ViewState(ListControllers.LEVY, Palette.CLASSIC, 50, 1.0, 0, 0, 0.3, -0.5);
    private static final ViewState SQUARE =
            new ViewState(ListControllers.SQUARE, Palette.CLASSIC, 50, 1.0, 0, 0, 0.3, -0.5);

    private static final List<ViewState> TOUR = List.of(MANDELBROT, JULIA, BURNING_SHIP, NEWTON, LEVY, SQUARE);
    private static final List<String> STILL_NAMES =
            List.of("mandelbrot", "julia", "burning-ship", "newton", "levy", "square");

    private DemoScript() {}

    public static List<Clip> clips() {
        return List.of(sphereSpin(), deepZoom(), juliaMorph(), tour(),
                burningShipZoom(), newtonZoom(), levyGrowth(), squareGrowth());
    }

    public static List<Still> stills() {
        List<Still> stills = new ArrayList<>();
        for (int k = 0; k < TOUR.size(); k++) {
            stills.add(new Still(STILL_NAMES.get(k), View.FLAT, new Frame(TOUR.get(k), FRONT_YAW_DEG, 0)));
        }
        return stills;
    }

    private static Clip sphereSpin() {
        int n = 6 * FPS;
        List<Frame> frames = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            frames.add(new Frame(MANDELBROT, FRONT_YAW_DEG + 360.0 * i / n, 15));
        }
        return new Clip("sphere-spin", View.SPHERE, frames);
    }

    private static Clip deepZoom() {
        int n = 8 * FPS;
        List<Frame> frames = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            // Seahorse valley; iterations grow with depth to keep details visible.
            ViewState s = new ViewState(ListControllers.MANDELBROT, Palette.OCEAN,
                    (int) Math.round(200 + 500 * t), Math.pow(DEEP_ZOOM_TARGET, t), -0.745, 0.11, 0.3, -0.5);
            frames.add(new Frame(s, FRONT_YAW_DEG + 20 * Math.sin(2 * Math.PI * t), 10));
        }
        return new Clip("deep-zoom", View.FLAT, frames);
    }

    private static Clip juliaMorph() {
        int n = 8 * FPS;
        List<Frame> frames = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double theta = 2 * Math.PI * i / n;
            ViewState s = new ViewState(ListControllers.JULIA, Palette.NEON, 150, 0.75, 0, 0,
                    JULIA_RADIUS * Math.cos(theta), JULIA_RADIUS * Math.sin(theta));
            frames.add(new Frame(s, FRONT_YAW_DEG + 10 * Math.sin(theta), 10));
        }
        return new Clip("julia-morph", View.FLAT, frames);
    }

    private static Clip tour() {
        int n = TOUR.size() * FRAMES_PER_FRACTAL;
        List<Frame> frames = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            frames.add(new Frame(TOUR.get(i / FRAMES_PER_FRACTAL), FRONT_YAW_DEG + 360.0 * i / n, 15));
        }
        return new Clip("tour", View.SPHERE, frames);
    }

    private static Clip burningShipZoom() {
        int n = 6 * FPS;
        List<Frame> frames = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            ViewState s = new ViewState(ListControllers.BURNING_SHIP, Palette.FIRE, 300,
                    0.4 * Math.pow(0.03 / 0.4, t), SHIP_RE, SHIP_IM, 0.3, -0.5);
            frames.add(new Frame(s, FRONT_YAW_DEG, 0));
        }
        return new Clip("burning-ship-zoom", View.FLAT, frames);
    }

    private static Clip newtonZoom() {
        int n = 5 * FPS;
        List<Frame> frames = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            ViewState s = new ViewState(ListControllers.NEWTON, Palette.NEON, 50,
                    0.75 * Math.pow(0.03 / 0.75, t), NEWTON_JUNCTION_RE, 0, 0.3, -0.5);
            frames.add(new Frame(s, FRONT_YAW_DEG, 0));
        }
        return new Clip("newton-zoom", View.FLAT, frames);
    }

    private static Clip levyGrowth() {
        List<Frame> frames = new ArrayList<>();
        for (int depth = 0; depth <= LEVY_MAX_DEPTH; depth++) {
            // For Lévy and Square, itMax is the recursion depth.
            ViewState s = new ViewState(ListControllers.LEVY, Palette.CLASSIC, depth, 1.0, 0, 0, 0.3, -0.5);
            for (int i = 0; i < FRAMES_PER_LEVY_LEVEL; i++) {
                frames.add(new Frame(s, FRONT_YAW_DEG, 0));
            }
        }
        return new Clip("levy-growth", View.FLAT, frames);
    }

    private static Clip squareGrowth() {
        List<Frame> frames = new ArrayList<>();
        for (int depth = 1; depth <= SQUARE_MAX_DEPTH; depth++) {
            ViewState s = new ViewState(ListControllers.SQUARE, Palette.CLASSIC, depth, 1.0, 0, 0, 0.3, -0.5);
            for (int i = 0; i < FRAMES_PER_SQUARE_LEVEL; i++) {
                frames.add(new Frame(s, FRONT_YAW_DEG, 0));
            }
        }
        return new Clip("square-growth", View.FLAT, frames);
    }
}
