package controleur;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RecursionDepthTest {

    @Test
    void levyDepthFollowsIterationsUpToFifteen() {
        assertEquals(0, LevyController.depth(0));
        assertEquals(7, LevyController.depth(7));
        // UI default is 50 iterations: same picture as the 2017 hardcoded depth.
        assertEquals(15, LevyController.depth(50));
        assertEquals(0, LevyController.depth(-3));
    }

    @Test
    void levyCurveKeepsItsSizeAtEveryDepth() {
        // Each missing level shrinks the curve by sqrt(2), so segments grow by sqrt(2) to compensate.
        assertEquals(4.0, LevyController.segmentLength(15), 1e-9);
        assertEquals(4.0 * Math.sqrt(2), LevyController.segmentLength(14), 1e-9);
        assertEquals(4.0 * Math.pow(2, 7.5), LevyController.segmentLength(0), 1e-9);
    }

    @Test
    void squareDepthFollowsIterationsBetweenOneAndSix() {
        assertEquals(1, SquareController.depth(0));
        assertEquals(3, SquareController.depth(3));
        assertEquals(6, SquareController.depth(50));
    }
}
