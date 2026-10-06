package modele;

import fractal.Palette;
import modele.Modele.ListControllers;

/** Everything that defines the rendered texture, so it can be applied with a single refresh. */
public record ViewState(ListControllers fractal, Palette palette, int itMax, double zoom,
                        double centerRe, double centerIm, double juliaRe, double juliaIm) {

    public ViewState {
        if (itMax < 0) {
            throw new IllegalArgumentException("itMax must be >= 0, got " + itMax);
        }
        if (!(zoom > 0)) {
            throw new IllegalArgumentException("zoom must be > 0, got " + zoom);
        }
    }
}
