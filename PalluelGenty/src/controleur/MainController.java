package controleur;

import modele.Modele;
import modele.Modele.ListControllers;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import java.awt.image.BufferedImage;

public class MainController {
	
	
	protected static Modele mm;
	private MandelBrotController mc;
	private SquareController sc;
	private LevyController lc;
	
	public MainController(){}
	
	public MainController(Modele mm){
		this.mm = mm;
		mc = new MandelBrotController();
		sc = new SquareController();
		lc = new LevyController();
		mm.setArrayColor();
	}
	
	public MainController getCurrentController(){
		switch(mm.getCurrentController()){
		case MANDELBROT:
			return mc;
		case LEVY:
			return lc;
		case SQUARE:
			return sc;
		default:
			return mc;
		}
	}	
	
	public WritableImage convertBufferedImage(BufferedImage image){
		WritableImage wr = null;
        if (image != null) {
            wr = new WritableImage(image.getWidth(), image.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int xi = 0; xi < image.getWidth(); xi++) {
                for (int yi = 0; yi < image.getHeight(); yi++) {
                    pw.setArgb(xi, yi, image.getRGB(xi, yi));
                }
            }
        }
        return wr;
	}

	public WritableImage paintSet() {
		return null;
	}

	public void setCurrentController(ListControllers c){
		mm.setCurrentController(c);
	}
	
}