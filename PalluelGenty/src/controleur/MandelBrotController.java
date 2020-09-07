package controleur;

import modele.Modele;
import modele.Modele.ListControllers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MandelBrotController extends MainController{
	
	private WritableImage image;
	
	public MandelBrotController(){
		image = new WritableImage(Modele.getWIDTH(), Modele.getHEIGHT());
	}

	public void zoom(int sens){
		System.out.println(sens==1 ? "PLUS" : "MOINS");
		mm.increaseZOOM(sens==1 ? -mm.getIncreaseNumber() : mm.getIncreaseNumber());
		//mm.setIT_MAX(sens==1 ? mm.getIT_MAX()+1 : mm.getIT_MAX()-1);
	}

	private class ThreadCalculs extends Thread{

		private int nb;

		public ThreadCalculs(int nb){
			this.nb=nb;
			this.run();
		}


		public void run() {
			PixelWriter pi = image.getPixelWriter();

			boolean isJulia = mm.getCurrentController()!=ListControllers.JULIA;

			for (int row = Modele.getHEIGHT()/4*nb; row < Modele.getHEIGHT()/4*(nb+1); row++) {
				for (int col = 0; col < Modele.getWIDTH(); col++) {
					double c_re = (col - Modele.getWIDTH()/2.0)*4.0/ Modele.getWIDTH();
					double c_im = (row - Modele.getHEIGHT()/2.0)*4.0/ Modele.getWIDTH();
					double x = (isJulia ? 0 : c_re) * (isJulia ? 1 : mm.getZOOM());
					double y = (isJulia ? 0 : c_im) * (isJulia ? 1 : mm.getZOOM());
					int iteration = 0;
					while (x*x+y*y <= 4 && iteration < mm.getIT_MAX()) {
						double x_new = x*x - y*y + (isJulia ? c_re : Modele.getZ());
						y = (2*x*y + (isJulia ? c_im : Modele.getZi())) * (isJulia ? mm.getZOOM() : 1);
						x = x_new * (isJulia ? mm.getZOOM() : 1);
						iteration++;
					}
					if (iteration < mm.getIT_MAX()) pi.setColor(col, row, mm.getColor()[iteration]);
					else pi.setColor(col, row, Color.BLACK);
				}
			}
		}

	}

    @Override
	public WritableImage paintSet() {
		mm.setArrayColor(); 

		long debut = System.currentTimeMillis();

		ArrayList<ThreadCalculs> array= new ArrayList<ThreadCalculs>();
		
		for (int i = 0;i<4;i++){
			array.add(new ThreadCalculs(i));
		}
		
		
		System.out.println((System.currentTimeMillis()-debut)/1000f + " s");
		return image;

	}

}