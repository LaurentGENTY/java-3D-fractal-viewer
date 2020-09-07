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
import java.util.Random;
import java.awt.Graphics2D;

public class LevyController extends MainController{
	
	public LevyController(){

	}

    public void zoom(int sens){
        System.out.println(sens==1 ? "PLUS" : "MOINS");
		mm.increaseZOOM(sens==1 ? -mm.getIncreaseNumber() : mm.getIncreaseNumber());
		//mm.setIT_MAX(sens==1 ? mm.getIT_MAX()+1 : mm.getIT_MAX()-1);
    }
    
    private class ThreadCalculs extends Thread{

		private int nb;
		private BufferedImage bi;

		public ThreadCalculs(int nb, BufferedImage bi){
			this.nb=nb;
			this.bi=bi;
			this.run();
		}


		public void run() {

		}

	}

    @Override
	public WritableImage paintSet() {
    	mm.setArrayColor();

    	long debut = System.currentTimeMillis();
    	
		BufferedImage image = new BufferedImage(Modele.getWIDTH(), Modele.getHEIGHT(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();

		String axiom = "F";
		for (int i =0; i<15 ; i++){
			axiom = "+" + axiom + "--" + axiom + "+";
		}
		
		Random r = new Random();
		java.awt.Color custom = new java.awt.Color(0,0,0);
		
		double length = 4;
		double currentAngle = 0;
		double x = 500;
		double y = 1000;
		
		for (int i = 0; i < 4; i++){
			char step = axiom.charAt(i);
			switch (step){
			case 'F':
				double x2 = x + (length * Math.cos(currentAngle));
				double y2 = y + (length * Math.sin(currentAngle));
				if (i%40 == 0)
					custom = new java.awt.Color(r.nextInt(254),r.nextInt(254),r.nextInt(254));
				g.setColor(custom);
				g.drawLine((int) Math.round(x),(int) Math.round(y),(int) Math.round(x2),(int) Math.round(y2));
				x = x2;
				y = y2;
				break;
			case '+':
				currentAngle += -Math.PI/4;
				break;
			case '-':
				currentAngle += Math.PI/4;
				break;
			}
		}
		System.out.println((System.currentTimeMillis()-debut)/1000f + " s");
		return convertBufferedImage(image);

	}

}