package fractal;

/** Color gradients sampled cyclically: {@code colorAt(t)} equals {@code colorAt(t + 1)}. */
public enum Palette {
    CLASSIC(null),
    FIRE(new int[] {0x000000, 0x7A0000, 0xFF4500, 0xFFD700, 0xFFFBE6}),
    OCEAN(new int[] {0x000814, 0x003566, 0x0077B6, 0x90E0EF, 0xFFFFFF}),
    NEON(new int[] {0x0D0221, 0xFF00A0, 0x00F0FF, 0x7B2FF7, 0xFAFF00});

    private static final int SIZE = 1024;
    // CLASSIC replays the 2017 formula, where one cycle spans 64 iterations.
    private static final double CLASSIC_ITERATIONS_PER_CYCLE = 64.0;

    private final int[] lut;

    Palette(int[] stops) {
        lut = stops == null ? classicLut() : gradientLut(stops);
    }

    public int colorAt(double t) {
        double frac = t - Math.floor(t);
        // NaN casts to 0; frac can round up to 1.0 for tiny negative t.
        int index = (int) (frac * SIZE);
        return lut[Math.min(Math.max(index, 0), SIZE - 1)];
    }

    private static int[] gradientLut(int[] stops) {
        int[] lut = new int[SIZE];
        for (int k = 0; k < SIZE; k++) {
            double pos = (double) k / SIZE * stops.length;
            int a = (int) pos;
            int from = stops[a];
            int to = stops[(a + 1) % stops.length];
            lut[k] = 0xFF000000 | lerp(from, to, pos - a);
        }
        return lut;
    }

    private static int[] classicLut() {
        int[] lut = new int[SIZE];
        for (int k = 0; k < SIZE; k++) {
            double i = k * CLASSIC_ITERATIONS_PER_CYCLE / SIZE;
            float hue = (float) (i / 4.0 / 360.0);
            float brightness = (float) (i / (i + 5.0));
            lut[k] = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 1f, brightness);
        }
        return lut;
    }

    private static int lerp(int from, int to, double f) {
        int r = channel(from, 16, to, f);
        int g = channel(from, 8, to, f);
        int b = channel(from, 0, to, f);
        return (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int shift, int to, double f) {
        int a = (from >> shift) & 0xFF;
        int b = (to >> shift) & 0xFF;
        return (int) Math.round(a + (b - a) * f);
    }
}
