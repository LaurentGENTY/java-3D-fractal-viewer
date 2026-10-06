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
