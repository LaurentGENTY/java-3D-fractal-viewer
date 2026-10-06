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
