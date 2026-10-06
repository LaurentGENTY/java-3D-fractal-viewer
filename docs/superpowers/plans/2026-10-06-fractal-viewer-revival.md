# Fractal Viewer Revival Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the 2017 JavaFX fractal viewer build with Maven on Java 21, add Burning Ship + Newton + smooth coloring + palettes, and generate showcase media (MP4, GIF, PNG) with a scripted demo mode.

**Architecture:** A new pure-Java package `fractal` (no JavaFX) computes ARGB pixel arrays in parallel; the existing 2017 controllers become thin adapters that copy those arrays into a `WritableImage`. A `demo` package drives the model and the sphere frame by frame, snapshots it to PNG, and `scripts/make-media.sh` turns frames into MP4/GIF with ffmpeg.

**Tech Stack:** Java 21, OpenJFX 21 (`javafx-controls`, `javafx-swing`), Maven + `javafx-maven-plugin`, JUnit 5, ffmpeg.

**Spec:** `docs/superpowers/specs/2026-10-06-fractal-viewer-revival-design.md`

## Global Constraints

- Java 21 (`maven.compiler.release` = 21), OpenJFX 21.0.x, Maven only (Gradle is not installed).
- Run with `mvn javafx:run`; demo with `mvn javafx:run -Pdemo` (profile passes `--demo`).
- Package `fractal` must not import any `javafx.*` class.
- New code and comments in English; original 2017 French comments and identifiers stay.
- 2017 code changes limited to what the spec lists (no refactor of `SphereViewer`).
- Media go to `docs/media/` (committed); raw frames to `target/demo-frames/` (ignored).
- Every commit message ends with the trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Work on the current feature branch; never commit on `master`; never push without Laurent's explicit approval.

## Review Focus

1. Fast-escaping points give a negative smooth count (`μ < 0`): they must be colored, not painted as interior black → test in Task 3.
2. `maxIter = 0` is accepted by the UI (`setIT_MAX(0)`): renderers must return an all-black image, no exception → tests in Tasks 3 and 4.
3. Newton at `z = 0` (derivative zero) must not throw or produce garbage → test in Task 4.
4. Palette sampled with negative, huge or NaN `t` must not throw `ArrayIndexOutOfBoundsException` → test in Task 2.
5. Stale frames from a previous longer demo run would end up in videos → script wipes `target/demo-frames` first (Task 7); the demo texture center must face the camera (`FRONT_YAW_DEG` check in Task 6).

---

## File Structure

```
pom.xml                                        # Task 1
.gitignore                                     # Task 1
src/main/java/application/Application.java     # moved (T1), --demo switch (T6)
src/main/java/controleur/*.java                # moved (T1); MandelBrot + Main modified (T5)
src/main/java/controleur/NewtonController.java # new (T5)
src/main/java/exceptions/*.java                # moved (T1), unchanged
src/main/java/modele/Modele.java               # moved (T1), new fractals/palette/viewport/state (T5)
src/main/java/modele/ViewState.java            # new (T5): full render state, set atomically
src/main/java/vue/SphereViewer.java            # moved (T1), CSS line removed (T1), UI (T5), capture API (T6)
src/main/java/fractal/Palette.java             # new (T2)
src/main/java/fractal/Viewport.java            # new (T2)
src/main/java/fractal/EscapeTimeRenderer.java  # new (T3)
src/main/java/fractal/NewtonRenderer.java      # new (T4)
src/main/java/demo/DemoScript.java             # new (T6): pure clip definitions
src/main/java/demo/DemoDirector.java           # new (T6): JavaFX frame loop + PNG writing
src/test/java/fractal/*Test.java               # T2–T4
src/test/java/demo/DemoScriptTest.java         # T6
scripts/make-media.sh                          # T7
docs/media/*                                   # T7 (generated)
README.md                                      # T7
```

---

### Task 1: Maven build and repo cleanup

**Files:**
- Create: `pom.xml`, `.gitignore`
- Move: `PalluelGenty/src/*` → `src/main/java/`
- Delete: everything else under `PalluelGenty/`
- Modify: `src/main/java/vue/SphereViewer.java` (remove CSS line, fix mojibake comment)

**Interfaces:**
- Produces: `mvn compile`, `mvn test`, `mvn javafx:run`, profile `demo` (passes `--demo` to `application.Application`).

- [ ] **Step 1: Move sources with history**

```bash
mkdir -p src/main/java src/test/java
git mv PalluelGenty/src/application PalluelGenty/src/controleur PalluelGenty/src/exceptions PalluelGenty/src/modele PalluelGenty/src/vue src/main/java/
git rm -r -q PalluelGenty
ls PalluelGenty 2>/dev/null || echo "PalluelGenty removed"
```

Expected: `PalluelGenty removed`. If the directory still exists (untracked leftovers), list it and delete only Eclipse junk after checking it.

- [ ] **Step 2: Write `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>io.github.laurentgenty</groupId>
  <artifactId>fractal-viewer</artifactId>
  <version>2.0.0</version>
  <name>3D fractal viewer</name>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <javafx.version>21.0.5</javafx.version>
    <junit.version>5.11.3</junit.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-controls</artifactId>
      <version>${javafx.version}</version>
    </dependency>
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-swing</artifactId>
      <version>${javafx.version}</version>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>${junit.version}</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.5.2</version>
      </plugin>
      <plugin>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>0.0.8</version>
        <configuration>
          <mainClass>application.Application</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>

  <profiles>
    <profile>
      <id>demo</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <configuration>
              <commandlineArgs>--demo</commandlineArgs>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
</project>
```

If `21.0.5` fails to resolve, use the latest `21.0.x` listed on https://central.sonatype.com/artifact/org.openjfx/javafx-controls.

- [ ] **Step 3: Write `.gitignore`**

```gitignore
# Build output (includes target/demo-frames)
target/

# IDE files
.idea/
*.iml
.vscode/
.classpath
.project
.settings/
.metadata/

# Claude Code worktrees
.claude/worktrees/

.DS_Store
```

- [ ] **Step 4: Remove the dead stylesheet reference in `SphereViewer`**

In `src/main/java/vue/SphereViewer.java`, delete this line (the file `view.css` never existed; JavaFX only logs a warning):

```java
	    scene.getStylesheets().add("src/application/view.css");
```

- [ ] **Step 5: Fix the mis-encoded comment**

```bash
grep -n "Ãƒ" src/main/java/vue/SphereViewer.java
sed -i '' "s/meme Ãƒ.*l'observable/meme à l'observable/" src/main/java/vue/SphereViewer.java
grep -n "observable modele" src/main/java/vue/SphereViewer.java
```

Expected after sed: `// On ajoute en tant qu'observer nous meme à l'observable modele`.

- [ ] **Step 6: Compile**

Run: `mvn -q compile`
Expected: exits 0. Deprecation warnings for `java.util.Observable` are acceptable. If a compile error appears, fix only that error (it will be a Java 8 → 21 incompatibility) and note it in the commit message.

- [ ] **Step 7: Smoke-run the app**

Run `mvn -q javafx:run` in the background (agent: `run_in_background`), wait ~20 s, then `screencapture -x target/smoke.png` and look at it: a window titled `PROJET FRACTALE PALLUEL - GENTY` with a textured sphere. Then stop it: `pkill -f application.Application`.
Expected: no stack trace in the output. (Human: just run `mvn javafx:run` and close the window.)

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "build: migrate to Maven, Java 21 and OpenJFX; remove Eclipse files" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: `Palette` and `Viewport`

**Files:**
- Create: `src/main/java/fractal/Palette.java`, `src/main/java/fractal/Viewport.java`
- Test: `src/test/java/fractal/PaletteTest.java`, `src/test/java/fractal/ViewportTest.java`

**Interfaces:**
- Produces:
  - `enum Palette { CLASSIC, FIRE, OCEAN, NEON }` with `int colorAt(double t)` → opaque ARGB, cyclic (`t` modulo 1).
  - `record Viewport(double centerRe, double centerIm, double scale, int width, int height)` with `double re(int col)`, `double im(int row)`, `static Viewport of(double zoom, double centerRe, double centerIm, int width, int height)` (`scale = 4 * zoom / width`).

- [ ] **Step 1: Write the failing tests**

`src/test/java/fractal/PaletteTest.java`:

```java
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
```

`src/test/java/fractal/ViewportTest.java`:

```java
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
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q test -Dtest='PaletteTest,ViewportTest'`
Expected: compilation failure, `cannot find symbol: class Palette` / `Viewport`.

- [ ] **Step 3: Implement `Viewport`**

`src/main/java/fractal/Viewport.java`:

```java
package fractal;

/** Maps pixel coordinates to the complex plane. Pixels are square; {@code scale} is complex units per pixel. */
public record Viewport(double centerRe, double centerIm, double scale, int width, int height) {

    /** At zoom 1 the image is 4 units wide, as in the 2017 version. Smaller zoom = closer. */
    public static Viewport of(double zoom, double centerRe, double centerIm, int width, int height) {
        return new Viewport(centerRe, centerIm, 4.0 * zoom / width, width, height);
    }

    public double re(int col) {
        return centerRe + (col - width / 2.0) * scale;
    }

    public double im(int row) {
        return centerIm + (row - height / 2.0) * scale;
    }
}
```

- [ ] **Step 4: Implement `Palette`**

`src/main/java/fractal/Palette.java`:

```java
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
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -q test -Dtest='PaletteTest,ViewportTest'`
Expected: `Tests run: 7, Failures: 0, Errors: 0`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/fractal src/test/java/fractal
git commit -m "feat: add Palette and Viewport for the fractal core" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: `EscapeTimeRenderer` (Mandelbrot, Julia, Burning Ship)

**Files:**
- Create: `src/main/java/fractal/EscapeTimeRenderer.java`
- Test: `src/test/java/fractal/EscapeTimeRendererTest.java`

**Interfaces:**
- Consumes: `Palette.colorAt(double)`, `Viewport.re/im/width/height` (Task 2).
- Produces:
  - `enum EscapeTimeRenderer.Formula { MANDELBROT, JULIA, BURNING_SHIP }`
  - `static final int INTERIOR = 0xFF000000`
  - `static double smoothIterations(Formula f, double re, double im, double cRe, double cIm, int maxIter)` → `>= 0`, or `-1` if bounded.
  - `static int[] render(Formula f, Viewport v, int maxIter, double cRe, double cIm, Palette p)` → ARGB, row-major. `cRe/cIm` only used by `JULIA`.

- [ ] **Step 1: Write the failing tests**

`src/test/java/fractal/EscapeTimeRendererTest.java`:

```java
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
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q test -Dtest=EscapeTimeRendererTest`
Expected: compilation failure, `cannot find symbol: class EscapeTimeRenderer`.

- [ ] **Step 3: Implement**

`src/main/java/fractal/EscapeTimeRenderer.java`:

```java
package fractal;

import java.util.stream.IntStream;

/** Escape-time fractals of the z -> z^2 + c family, with smooth coloring. */
public final class EscapeTimeRenderer {

    public enum Formula { MANDELBROT, JULIA, BURNING_SHIP }

    public static final int INTERIOR = 0xFF000000;

    // A large bailout radius keeps the smooth-coloring formula accurate.
    private static final double BAILOUT_SQ = 256.0 * 256.0;
    private static final double ITERATIONS_PER_CYCLE = 64.0;
    private static final double LOG2 = Math.log(2.0);

    private EscapeTimeRenderer() {}

    /**
     * Smooth (fractional) iteration count at point (re, im), or -1 if the orbit stays bounded for
     * maxIter iterations. For JULIA the point is z0 and (cRe, cIm) is c; otherwise the point is c.
     */
    public static double smoothIterations(Formula f, double re, double im, double cRe, double cIm, int maxIter) {
        boolean julia = f == Formula.JULIA;
        double x = julia ? re : 0;
        double y = julia ? im : 0;
        double addRe = julia ? cRe : re;
        double addIm = julia ? cIm : im;
        for (int n = 1; n <= maxIter; n++) {
            if (f == Formula.BURNING_SHIP) {
                x = Math.abs(x);
                y = Math.abs(y);
            }
            double nx = x * x - y * y + addRe;
            y = 2 * x * y + addIm;
            x = nx;
            double mod2 = x * x + y * y;
            if (mod2 > BAILOUT_SQ) {
                // mu = n + 1 - log2(ln|z|); clamped because very fast escapes go negative.
                return Math.max(0.0, n + 1 - Math.log(0.5 * Math.log(mod2)) / LOG2);
            }
        }
        return -1;
    }

    public static int colorFor(double mu, Palette p) {
        return mu < 0 ? INTERIOR : p.colorAt(mu / ITERATIONS_PER_CYCLE);
    }

    public static int[] render(Formula f, Viewport v, int maxIter, double cRe, double cIm, Palette p) {
        int w = v.width();
        int[] argb = new int[w * v.height()];
        IntStream.range(0, v.height()).parallel().forEach(row -> {
            double im = v.im(row);
            for (int col = 0; col < w; col++) {
                argb[row * w + col] = colorFor(smoothIterations(f, v.re(col), im, cRe, cIm, maxIter), p);
            }
        });
        return argb;
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -q test -Dtest=EscapeTimeRendererTest`
Expected: `Tests run: 8, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/fractal/EscapeTimeRenderer.java src/test/java/fractal/EscapeTimeRendererTest.java
git commit -m "feat: add parallel escape-time renderer with smooth coloring" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: `NewtonRenderer` (z³ − 1)

**Files:**
- Create: `src/main/java/fractal/NewtonRenderer.java`
- Test: `src/test/java/fractal/NewtonRendererTest.java`

**Interfaces:**
- Consumes: `Palette`, `Viewport` (Task 2).
- Produces:
  - `record NewtonRenderer.Convergence(int root, int iterations)` (`root` = 0 for 1, 1 for e^{2iπ/3}, 2 for e^{-2iπ/3}, -1 if not converged).
  - `static Convergence converge(double re, double im, int maxIter)`
  - `static int colorFor(Convergence c, Palette p)`
  - `static int[] render(Viewport v, int maxIter, Palette p)`

- [ ] **Step 1: Write the failing tests**

`src/test/java/fractal/NewtonRendererTest.java`:

```java
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
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q test -Dtest=NewtonRendererTest`
Expected: compilation failure, `cannot find symbol: class NewtonRenderer`.

- [ ] **Step 3: Implement**

`src/main/java/fractal/NewtonRenderer.java`:

```java
package fractal;

import java.util.stream.IntStream;

/** Newton's method on f(z) = z^3 - 1: pixels are colored by the root they converge to. */
public final class NewtonRenderer {

    public record Convergence(int root, int iterations) {}

    private static final Convergence NOT_CONVERGED = new Convergence(-1, 0);
    private static final double TOLERANCE_SQ = 1e-12; // |z - root| < 1e-6
    private static final double[] ROOTS_RE = {1.0, -0.5, -0.5};
    private static final double[] ROOTS_IM = {0.0, Math.sqrt(3) / 2, -Math.sqrt(3) / 2};
    // Each iteration darkens the basin color, which outlines the fractal boundaries.
    private static final double SHADE_PER_ITERATION = 0.92;

    private NewtonRenderer() {}

    public static Convergence converge(double re, double im, int maxIter) {
        double x = re;
        double y = im;
        for (int n = 0; n <= maxIter; n++) {
            for (int k = 0; k < 3; k++) {
                double dx = x - ROOTS_RE[k];
                double dy = y - ROOTS_IM[k];
                if (dx * dx + dy * dy < TOLERANCE_SQ) {
                    return new Convergence(k, n);
                }
            }
            if (n == maxIter) {
                break;
            }
            // z <- z - (z^3 - 1) / (3 z^2) = (2 z^3 + 1) / (3 z^2)
            double z2Re = x * x - y * y;
            double z2Im = 2 * x * y;
            double denRe = 3 * z2Re;
            double denIm = 3 * z2Im;
            double den = denRe * denRe + denIm * denIm;
            if (den == 0.0) {
                return NOT_CONVERGED;
            }
            double numRe = 2 * (z2Re * x - z2Im * y) + 1;
            double numIm = 2 * (z2Re * y + z2Im * x);
            x = (numRe * denRe + numIm * denIm) / den;
            y = (numIm * denRe - numRe * denIm) / den;
        }
        return NOT_CONVERGED;
    }

    public static int colorFor(Convergence c, Palette p) {
        if (c.root() < 0) {
            return 0xFF000000;
        }
        int base = p.colorAt((c.root() + 0.5) / 3.0);
        double shade = Math.pow(SHADE_PER_ITERATION, c.iterations());
        int r = (int) (((base >> 16) & 0xFF) * shade);
        int g = (int) (((base >> 8) & 0xFF) * shade);
        int b = (int) ((base & 0xFF) * shade);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int[] render(Viewport v, int maxIter, Palette p) {
        int w = v.width();
        int[] argb = new int[w * v.height()];
        IntStream.range(0, v.height()).parallel().forEach(row -> {
            double im = v.im(row);
            for (int col = 0; col < w; col++) {
                argb[row * w + col] = colorFor(converge(v.re(col), im, maxIter), p);
            }
        });
        return argb;
    }
}
```

- [ ] **Step 4: Run all tests**

Run: `mvn -q test`
Expected: `Tests run: 20, Failures: 0, Errors: 0` (7 + 8 + 5).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/fractal/NewtonRenderer.java src/test/java/fractal/NewtonRendererTest.java
git commit -m "feat: add Newton z^3 - 1 renderer" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Wire the core into the 2017 app (model, controllers, UI)

**Files:**
- Create: `src/main/java/modele/ViewState.java`, `src/main/java/controleur/NewtonController.java`
- Modify: `src/main/java/modele/Modele.java`, `src/main/java/controleur/MandelBrotController.java` (full rewrite), `src/main/java/controleur/MainController.java`, `src/main/java/vue/SphereViewer.java`

**Interfaces:**
- Consumes: `EscapeTimeRenderer.render`, `NewtonRenderer.render`, `Palette`, `Viewport.of` (Tasks 2–4).
- Produces (used by Task 6):
  - `Modele.ListControllers` gains `BURNING_SHIP`, `NEWTON`.
  - `record ViewState(ListControllers fractal, Palette palette, int itMax, double zoom, double centerRe, double centerIm, double juliaRe, double juliaIm)` — validates `itMax >= 0`, `zoom > 0`.
  - `Modele`: `Palette getPalette()`, `void setPalette(Palette)`, `Viewport getViewport()`, `ViewState getViewState()`, `void setViewState(ViewState)` (sets everything, notifies observers once).

- [ ] **Step 1: Create `ViewState`**

`src/main/java/modele/ViewState.java`:

```java
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
```

- [ ] **Step 2: Extend `Modele`**

In `src/main/java/modele/Modele.java`:

a) Add imports after `import javafx.scene.paint.Color;`:

```java
import fractal.Palette;
import fractal.Viewport;
```

b) Extend the enum:

```java
	public enum ListControllers{
		MANDELBROT,
		JULIA,
		LEVY,
		SQUARE,
		BURNING_SHIP,
		NEWTON
	}
```

c) After the `// Couleurs` field block (`private Color color[] = ...;`), add:

```java
    private Palette palette = Palette.CLASSIC;

    // Viewport center; only the demo mode moves it.
    private double centerRe = 0;
    private double centerIm = 0;
```

d) After the `getCurrentController()` getter, add:

```java
    public Palette getPalette(){
    	return palette;
    }

    public Viewport getViewport(){
    	return Viewport.of(ZOOM, centerRe, centerIm, WIDTH, HEIGHT);
    }

    public ViewState getViewState(){
    	return new ViewState(currentController, palette, IT_MAX, ZOOM, centerRe, centerIm, Z, Zi);
    }
```

e) After `setZi(double zi)`, add:

```java
    public void setPalette(Palette p){
    	palette = p;
    	fire();
    }

    // Applies the whole state at once so the texture is recomputed only one time.
    public void setViewState(ViewState s){
    	currentController = s.fractal();
    	palette = s.palette();
    	IT_MAX = s.itMax();
    	ZOOM = s.zoom();
    	centerRe = s.centerRe();
    	centerIm = s.centerIm();
    	Z = s.juliaRe();
    	Zi = s.juliaIm();
    	setArrayColor();
    	fire();
    }
```

- [ ] **Step 3: Rewrite `MandelBrotController`**

Replace the whole file `src/main/java/controleur/MandelBrotController.java` with:

```java
package controleur;

import fractal.EscapeTimeRenderer;
import fractal.EscapeTimeRenderer.Formula;
import fractal.Viewport;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import modele.Modele;

public class MandelBrotController extends MainController{
	
	private WritableImage image;
	
	public MandelBrotController(){
		image = new WritableImage(Modele.getWIDTH(), Modele.getHEIGHT());
	}

	public void zoom(int sens){
		System.out.println(sens==1 ? "PLUS" : "MOINS");
		mm.increaseZOOM(sens==1 ? -mm.getIncreaseNumber() : mm.getIncreaseNumber());
	}

	// Mandelbrot, Julia and Burning Ship share one escape-time kernel.
	private Formula currentFormula() {
		switch (mm.getCurrentController()) {
		case JULIA:
			return Formula.JULIA;
		case BURNING_SHIP:
			return Formula.BURNING_SHIP;
		default:
			return Formula.MANDELBROT;
		}
	}

    @Override
	public WritableImage paintSet() {
		long debut = System.currentTimeMillis();

		Viewport v = mm.getViewport();
		int[] argb = EscapeTimeRenderer.render(currentFormula(), v, mm.getIT_MAX(),
				Modele.getZ(), Modele.getZi(), mm.getPalette());
		image.getPixelWriter().setPixels(0, 0, v.width(), v.height(),
				PixelFormat.getIntArgbInstance(), argb, 0, v.width());

		System.out.println((System.currentTimeMillis()-debut)/1000f + " s");
		return image;
	}

}
```

This removes the misnamed `isJulia` flag and the fake `ThreadCalculs` (its constructor called `run()` synchronously).

- [ ] **Step 4: Create `NewtonController`**

`src/main/java/controleur/NewtonController.java`:

```java
package controleur;

import fractal.NewtonRenderer;
import fractal.Viewport;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import modele.Modele;

public class NewtonController extends MainController{

	private WritableImage image;

	public NewtonController(){
		image = new WritableImage(Modele.getWIDTH(), Modele.getHEIGHT());
	}

    @Override
	public WritableImage paintSet() {
		Viewport v = mm.getViewport();
		int[] argb = NewtonRenderer.render(v, mm.getIT_MAX(), mm.getPalette());
		image.getPixelWriter().setPixels(0, 0, v.width(), v.height(),
				PixelFormat.getIntArgbInstance(), argb, 0, v.width());
		return image;
	}

}
```

- [ ] **Step 5: Route the new fractals in `MainController`**

In `src/main/java/controleur/MainController.java`:

a) Add the field after `private LevyController lc;`:

```java
	private NewtonController nc;
```

b) In the constructor, after `lc = new LevyController();`:

```java
		nc = new NewtonController();
```

c) Replace the `switch` in `getCurrentController()` with:

```java
		switch(mm.getCurrentController()){
		case MANDELBROT:
		case JULIA:
		case BURNING_SHIP:
			return mc;
		case NEWTON:
			return nc;
		case LEVY:
			return lc;
		case SQUARE:
			return sc;
		default:
			return mc;
		}
```

- [ ] **Step 6: Add the UI controls in `SphereViewer`**

In `src/main/java/vue/SphereViewer.java`:

a) Imports, after `import javafx.beans.value.ObservableValue;`:

```java
import javafx.collections.FXCollections;
import fractal.Palette;
```

b) Fields, after `private RadioButton mandelBrot;`:

```java
	private RadioButton burningShip;
	private RadioButton newton;
	private ComboBox<Palette> paletteChoice;
```

c) In the constructor, after `square.setToggleGroup(changeFractale);`:

```java
		burningShip = new RadioButton("Burning Ship");
		burningShip.setToggleGroup(changeFractale);
		newton = new RadioButton("Newton");
		newton.setToggleGroup(changeFractale);
```

d) After `colorPicker.isResizable();`:

```java
		paletteChoice = new ComboBox<>(FXCollections.observableArrayList(Palette.values()));
		paletteChoice.setValue(mm.getPalette());
```

e) Replace the `boxParameters.getChildren().addAll(...)` call with:

```java
		boxParameters.getChildren().addAll(buttonsZooms, new Separator(),
				mandelBrot, julia, levy, square, burningShip, newton, enX, sliderX,enY,sliderY, new Separator(),
				nombreIte, ite, alphaText,alpha, betaText, beta, colorPicker, paletteChoice, validateParam, reset);
```

f) After the `square.addEventHandler(...)` block (end of the `CHANGEMENTS DE FRACTALES` section), add:

```java
		burningShip.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
			deselectLast(burningShip);
			mc.setCurrentController(ListControllers.BURNING_SHIP);
		});

		newton.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
			deselectLast(newton);
			mc.setCurrentController(ListControllers.NEWTON);
		});

		paletteChoice.setOnAction(event -> mm.setPalette(paletteChoice.getValue()));
```

- [ ] **Step 7: Apply Alpha/Beta before the refresh**

In the `validateParam` handler, `setIT_MAX` triggers the refresh **before** `Modele.setZ/setZi` run, so a new Julia constant only shows on the next refresh. Move the two blocks `if(alpha.getText() != "") {...}` and `if(beta.getText() != "") {...}` above the `if(isNumeric(ite.getText()))` block, without changing their content. Resulting order in the handler body: alpha block, beta block, iterations block.

- [ ] **Step 8: Compile and run the tests**

Run: `mvn -q test`
Expected: `Tests run: 20, Failures: 0, Errors: 0`.

- [ ] **Step 9: Manual check**

Run `mvn -q javafx:run` (agent: background + `screencapture -x target/check-<name>.png` after each change, then `pkill -f application.Application`). Check:
- Mandelbrot shows without color banding (smooth gradient).
- Burning Ship and Newton radio buttons switch the texture.
- The palette combo box recolors Mandelbrot, Julia, Burning Ship and Newton.
- Julia: set Alpha `-0.8`, Beta `0.156`, click "Valider les parametres" → texture updates at once.
- Iterations `0` + validate → black sphere, no stack trace.

Agents: the UI needs mouse clicks; if not possible, ask Laurent to run this check and report.

- [ ] **Step 10: Commit**

```bash
git add src/main/java
git commit -m "feat: add Burning Ship, Newton and palettes; render via the parallel core" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Demo mode (`DemoScript`, `DemoDirector`, capture API)

**Files:**
- Create: `src/main/java/demo/DemoScript.java`, `src/main/java/demo/DemoDirector.java`
- Modify: `src/main/java/vue/SphereViewer.java`, `src/main/java/application/Application.java`
- Test: `src/test/java/demo/DemoScriptTest.java`

**Interfaces:**
- Consumes: `ViewState`, `Modele.setViewState`, `Modele.ListControllers` (Task 5), `Palette` (Task 2).
- Produces:
  - `DemoScript.clips()` → `List<Clip>`; `DemoScript.stills()` → `List<Still>`; records `Frame(ViewState state, double yawDeg, double pitchDeg)`, `Clip(String name, List<Frame> frames)`, `Still(String name, Frame frame)`.
  - `SphereViewer.setSphereRotation(double yawDeg, double pitchDeg)`, `SphereViewer.captureSphere()` → `WritableImage`.
  - Output layout used by Task 7: `target/demo-frames/<clip>/frame-0001.png …`, `target/demo-frames/stills/<name>.png`.

- [ ] **Step 1: Write the failing test**

`src/test/java/demo/DemoScriptTest.java`:

```java
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
        assertEquals(List.of("sphere-spin", "deep-zoom", "julia-morph", "tour"),
                clips.stream().map(Clip::name).toList());
        assertEquals(List.of(180, 240, 240, 270),
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
    void oneStillPerFractal() {
        assertEquals(List.of("mandelbrot", "julia", "burning-ship", "newton", "levy", "square"),
                DemoScript.stills().stream().map(Still::name).toList());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q test -Dtest=DemoScriptTest`
Expected: compilation failure, `cannot find symbol: class DemoScript`.

- [ ] **Step 3: Implement `DemoScript`**

`src/main/java/demo/DemoScript.java`:

```java
package demo;

import fractal.Palette;
import java.util.ArrayList;
import java.util.List;
import modele.Modele.ListControllers;
import modele.ViewState;

/** Frame-by-frame definition of the showcase clips. Pure data: no JavaFX. */
public final class DemoScript {

    public record Frame(ViewState state, double yawDeg, double pitchDeg) {}
    public record Clip(String name, List<Frame> frames) {}
    public record Still(String name, Frame frame) {}

    public static final int FPS = 30;

    // Yaw that brings the texture center (the viewport center) in front of the camera.
    static final double FRONT_YAW_DEG = 0;

    private static final double JULIA_RADIUS = 0.7885;
    private static final double DEEP_ZOOM_TARGET = 1e-4;
    private static final int FRAMES_PER_FRACTAL = 45;

    private static final ViewState MANDELBROT =
            new ViewState(ListControllers.MANDELBROT, Palette.FIRE, 200, 1.0, 0, 0, 0.3, -0.5);
    private static final ViewState JULIA =
            new ViewState(ListControllers.JULIA, Palette.OCEAN, 200, 0.75, 0, 0, -0.8, 0.156);
    private static final ViewState BURNING_SHIP =
            new ViewState(ListControllers.BURNING_SHIP, Palette.FIRE, 200, 0.9, -0.45, -0.5, 0.3, -0.5);
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
        return List.of(sphereSpin(), deepZoom(), juliaMorph(), tour());
    }

    public static List<Still> stills() {
        List<Still> stills = new ArrayList<>();
        for (int k = 0; k < TOUR.size(); k++) {
            stills.add(new Still(STILL_NAMES.get(k), new Frame(TOUR.get(k), FRONT_YAW_DEG, 0)));
        }
        return stills;
    }

    private static Clip sphereSpin() {
        int n = 6 * FPS;
        List<Frame> frames = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            frames.add(new Frame(MANDELBROT, FRONT_YAW_DEG + 360.0 * i / n, 15));
        }
        return new Clip("sphere-spin", frames);
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
        return new Clip("deep-zoom", frames);
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
        return new Clip("julia-morph", frames);
    }

    private static Clip tour() {
        int n = TOUR.size() * FRAMES_PER_FRACTAL;
        List<Frame> frames = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            frames.add(new Frame(TOUR.get(i / FRAMES_PER_FRACTAL), FRONT_YAW_DEG + 360.0 * i / n, 15));
        }
        return new Clip("tour", frames);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -q test -Dtest=DemoScriptTest`
Expected: `Tests run: 5, Failures: 0, Errors: 0`.

- [ ] **Step 5: Add the capture API and the refresh fix to `SphereViewer`**

In `src/main/java/vue/SphereViewer.java`:

a) After the `private static int SPHERE_RADIUS  = GLOBALWIDTH/4;` line, add:

```java
	// Margin around the sphere in demo captures, in scene pixels.
	private static final int CAPTURE_MARGIN = 50;
```

b) In `refreshSphere()`, replace `pane.getChildren().add(group);` with:

```java
		// setAll: the demo refreshes hundreds of times, add() kept every old group.
		pane.getChildren().setAll(group);
```

c) Before the `// FUNCTIONS UTILS` banner, add:

```java
	// ********************************************************
	//					DEMO MODE API
	// ********************************************************

	public void setSphereRotation(double yawDeg, double pitchDeg) {
		sphere.getTransforms().setAll(
				new Rotate(pitchDeg, new Point3D(1, 0, 0)),
				new Rotate(yawDeg, new Point3D(0, 1, 0)));
	}

	/** Square snapshot centered on the sphere, without the parameter panel. */
	public WritableImage captureSphere() {
		WritableImage full = scene.snapshot(null);
		// The snapshot may be in device pixels (HiDPI): convert scene coordinates.
		double ratio = full.getWidth() / scene.getWidth();
		int side = (int) Math.round(2 * (SPHERE_RADIUS + CAPTURE_MARGIN) * ratio);
		side -= side % 2; // even size for yuv420p video
		Point3D center = sphere.localToScene(Point3D.ZERO);
		int x = Math.clamp(Math.round(center.getX() * ratio) - side / 2, 0, (int) full.getWidth() - side);
		int y = Math.clamp(Math.round(center.getY() * ratio) - side / 2, 0, (int) full.getHeight() - side);
		return new WritableImage(full.getPixelReader(), x, y, side, side);
	}
```

- [ ] **Step 6: Implement `DemoDirector`**

`src/main/java/demo/DemoDirector.java`:

```java
package demo;

import demo.DemoScript.Clip;
import demo.DemoScript.Frame;
import demo.DemoScript.Still;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import modele.Modele;
import modele.ViewState;
import vue.SphereViewer;

/** Plays the demo script frame by frame, independent of real time, and writes one PNG per frame. */
public final class DemoDirector {

    private record Shot(Path file, Frame frame) {}

    private static final int LOG_EVERY = DemoScript.FPS;

    private final Modele model;
    private final SphereViewer viewer;
    private final List<Shot> shots = new ArrayList<>();
    private ViewState applied;

    public DemoDirector(Modele model, SphereViewer viewer, Path outputDir) {
        this.model = model;
        this.viewer = viewer;
        for (Clip clip : DemoScript.clips()) {
            for (int i = 0; i < clip.frames().size(); i++) {
                Path file = outputDir.resolve(clip.name()).resolve(String.format("frame-%04d.png", i + 1));
                shots.add(new Shot(file, clip.frames().get(i)));
            }
        }
        for (Still still : DemoScript.stills()) {
            shots.add(new Shot(outputDir.resolve("stills").resolve(still.name() + ".png"), still.frame()));
        }
    }

    /** Starts the frame loop; the application exits when every shot is written. */
    public void start() {
        Platform.runLater(() -> shoot(0));
    }

    private void shoot(int index) {
        if (index == shots.size()) {
            System.out.println("Demo: " + shots.size() + " images written");
            Platform.exit();
            return;
        }
        Shot shot = shots.get(index);
        try {
            // Re-rendering the texture is the expensive part: only do it when the state changes.
            if (!shot.frame().state().equals(applied)) {
                model.setViewState(shot.frame().state());
                applied = shot.frame().state();
            }
            viewer.setSphereRotation(shot.frame().yawDeg(), shot.frame().pitchDeg());
            Files.createDirectories(shot.file().getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(viewer.captureSphere(), null), "png", shot.file().toFile());
        } catch (IOException | RuntimeException e) {
            System.err.println("Demo failed on " + shot.file());
            e.printStackTrace();
            System.exit(1);
        }
        if (index % LOG_EVERY == 0) {
            System.out.println("Demo: " + index + "/" + shots.size());
        }
        // One shot per pulse keeps the window responsive during the run.
        Platform.runLater(() -> shoot(index + 1));
    }
}
```

- [ ] **Step 7: Add the `--demo` switch to `Application`**

Replace the body of `start` in `src/main/java/application/Application.java` and add the imports:

```java
package application;

import controleur.MainController;
import demo.DemoDirector;
import java.nio.file.Path;
import javafx.stage.Stage;
import modele.Modele;
import vue.SphereViewer;

public class Application extends javafx.application.Application{

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Modele mm = new Modele();
		MainController mc = new MainController(mm);
		SphereViewer sphere = new SphereViewer(mc, mm);
		if (getParameters().getRaw().contains("--demo")) {
			new DemoDirector(mm, sphere, Path.of("target", "demo-frames")).start();
		}
	}


}
```

- [ ] **Step 8: Run the demo**

```bash
rm -rf target/demo-frames
mvn -q -Pdemo javafx:run
find target/demo-frames -name '*.png' | wc -l
```

Expected: progress lines, then `Demo: 936 images written` and the process exits 0 (930 clip frames + 6 stills); `find` prints `936`. Note the total run time (spec estimate: 1–3 min).

- [ ] **Step 9: Check framing and orientation**

Open `target/demo-frames/stills/mandelbrot.png` and `target/demo-frames/deep-zoom/frame-0240.png` (Read tool or Preview).
- The sphere is fully visible and centered in the square image.
- In `mandelbrot.png`, the main cardioid faces the camera. If it is on the side or back, the texture center is not at yaw 0: change `FRONT_YAW_DEG` in `DemoScript` (try 90, then 180, then 270, then -90), rerun Step 8 and recheck.
- `deep-zoom/frame-0240.png` shows spiral detail, not a uniform color.
- `burning-ship.png` shows the ship shape. If it is upside down or off-frame, do not guess: report it to Laurent with the image.

- [ ] **Step 10: Run all tests and commit**

Run: `mvn -q test` → `Tests run: 25, Failures: 0, Errors: 0`.

```bash
git add src/main/java src/test/java
git commit -m "feat: add scripted demo mode that captures the sphere frame by frame" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Media script, generated media and README

**Files:**
- Create: `scripts/make-media.sh`, `docs/media/*` (generated)
- Modify: `README.md`

**Interfaces:**
- Consumes: `mvn -Pdemo javafx:run` and the frame layout from Task 6.
- Produces: `docs/media/{sphere-spin,deep-zoom,julia-morph,tour}.{mp4,gif}` and `docs/media/{mandelbrot,julia,burning-ship,newton,levy,square}.png`.

- [ ] **Step 1: Write `scripts/make-media.sh`**

```bash
#!/usr/bin/env bash
# Regenerates every showcase media file in docs/media from the scripted demo.
set -euo pipefail

cd "$(dirname "$0")/.."

for tool in mvn ffmpeg; do
  command -v "$tool" >/dev/null 2>&1 || { echo "error: '$tool' not found in PATH" >&2; exit 1; }
done

FRAMES=target/demo-frames
OUT=docs/media
GIF_WIDTH=${GIF_WIDTH:-640}
GIF_MAX_BYTES=$((5 * 1024 * 1024))
CLIPS=(sphere-spin deep-zoom julia-morph tour)

# Stale frames from a previous, longer run would leak into the videos.
rm -rf "$FRAMES"
mvn -q -Pdemo javafx:run

mkdir -p "$OUT"
for clip in "${CLIPS[@]}"; do
  echo "Encoding $clip"
  ffmpeg -y -loglevel error -framerate 30 -i "$FRAMES/$clip/frame-%04d.png" \
    -c:v libx264 -pix_fmt yuv420p -crf 20 -movflags +faststart "$OUT/$clip.mp4"
  ffmpeg -y -loglevel error -framerate 30 -i "$FRAMES/$clip/frame-%04d.png" \
    -vf "fps=15,scale=${GIF_WIDTH}:-1:flags=lanczos,split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle" \
    "$OUT/$clip.gif"
  size=$(wc -c < "$OUT/$clip.gif" | tr -d ' ')
  if [ "$size" -gt "$GIF_MAX_BYTES" ]; then
    echo "warning: $OUT/$clip.gif is $((size / 1024)) KiB (target < 5 MiB); retry with GIF_WIDTH=480" >&2
  fi
done

cp "$FRAMES"/stills/*.png "$OUT/"
ls -lh "$OUT"
```

```bash
chmod +x scripts/make-media.sh
```

- [ ] **Step 2: Check the error path**

Run: `PATH=/usr/bin:/bin scripts/make-media.sh; echo "exit=$?"`
Expected: `error: 'mvn' not found in PATH` and `exit=1`.

- [ ] **Step 3: Generate the media**

Run: `scripts/make-media.sh`
Expected: 14 files listed in `docs/media/` (4 `.mp4`, 4 `.gif`, 6 `.png`), no warning. If a GIF warning appears, run `GIF_WIDTH=480 scripts/make-media.sh` and keep that width.

- [ ] **Step 4: Review the media**

Open `docs/media/tour.gif` and two MP4s (`open docs/media/deep-zoom.mp4`). Check: smooth rotation, no black frames, no cut-off sphere. Ask Laurent to look at `tour.gif` before committing (it becomes the README hero).

- [ ] **Step 5: Rewrite `README.md`**

Replace `README.md` with:

````markdown
# 3D fractal viewer

A **JavaFX** desktop app that generates fractals and wraps them as a texture on a **3D sphere** you can rotate, with PNG export.

![Tour of the fractals on the sphere](docs/media/tour.gif)

> School project, DUT Informatique, University of Bordeaux (2017), by Laurent Genty and Luis Palluel. Revived in 2026: Maven build, Java 21, new fractals and smooth coloring.

## Gallery

| Mandelbrot | Julia | Burning Ship |
|---|---|---|
| ![Mandelbrot](docs/media/mandelbrot.png) | ![Julia](docs/media/julia.png) | ![Burning Ship](docs/media/burning-ship.png) |
| **Newton (z³ − 1)** | **Lévy C curve** | **Recursive square** |
| ![Newton](docs/media/newton.png) | ![Lévy C curve](docs/media/levy.png) | ![Square](docs/media/square.png) |

Videos: [sphere spin](docs/media/sphere-spin.mp4) · [deep zoom](docs/media/deep-zoom.mp4) · [Julia morph](docs/media/julia-morph.mp4) · [tour](docs/media/tour.mp4)

## Features

- Six fractals: **Mandelbrot**, **Julia**, **Burning Ship**, **Newton (z³ − 1)**, **Lévy C curve** and a recursive **square** fractal.
- Smooth coloring (no color bands) and four palettes: Classic (2017), Fire, Ocean, Neon.
- Configurable parameters (iterations, Julia constant) with input validation.
- Fractal rendered as a texture on a 3D sphere, rotatable with the mouse.
- Parallel rendering on all CPU cores.
- Export of the current fractal to PNG.
- Scripted demo mode that regenerates every image and video of this README.

## Getting started

Requirements: Java 21 and Maven.

```bash
mvn javafx:run
```

Regenerate the media in `docs/media/` (requires `ffmpeg`, takes a few minutes):

```bash
scripts/make-media.sh
```

## Architecture

Classic **MVC** from 2017, plus a pure-Java rendering core added in 2026:

```
src/main/java/
  application/Application.java   # entry point (--demo starts the demo mode)
  modele/                        # model (observable) + ViewState
  vue/SphereViewer.java          # JavaFX view: 3D scene, controls
  controleur/                    # one controller per fractal family + MainController
  fractal/                       # rendering core, no JavaFX: palettes, escape-time and Newton renderers
  demo/                          # scripted clips and frame capture
  exceptions/                    # input validation errors
```

Tests: `mvn test`.
````

- [ ] **Step 6: Verify and commit**

Run: `mvn -q verify` → exit 0. Run `git status --short` and confirm only `README.md`, `scripts/`, `docs/media/` appear (no `target/`).

```bash
git add README.md scripts/make-media.sh docs/media
git commit -m "docs: add showcase media, media script and refreshed README" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 7: Stop before pushing**

Report to Laurent: test count, demo run time, media sizes (`du -sh docs/media`). Ask before any `git push` / PR.
