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
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import modele.Modele;
import modele.ViewState;
import vue.SphereViewer;

/** Plays the demo script frame by frame, independent of real time, and writes one PNG per frame. */
public final class DemoDirector {

    private record Shot(Path file, DemoScript.View view, Frame frame) {}

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
                shots.add(new Shot(file, clip.view(), clip.frames().get(i)));
            }
        }
        for (Still still : DemoScript.stills()) {
            shots.add(new Shot(outputDir.resolve("stills").resolve(still.name() + ".png"), still.view(), still.frame()));
        }
    }

    /** Starts the frame loop; the application exits when every shot is written. */
    public void start() {
        Platform.runLater(() -> shoot(0));
    }

    private void shoot(int index) {
        if (index == shots.size()) {
            System.out.println("Demo: " + shots.size() + " images written");
            // Closing the window first avoids a JavaFX shutdown exception on macOS.
            viewer.close();
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
            WritableImage image = shot.view() == DemoScript.View.FLAT ? viewer.currentTexture() : viewer.captureSphere();
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", shot.file().toFile());
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
