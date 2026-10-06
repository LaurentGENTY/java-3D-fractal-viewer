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

	// 2017 hardcoded depth: 2^15 segments of 4 px.
	private static final int MAX_DEPTH = 15;
	private static final double FULL_DEPTH_SEGMENT = 4;
	// Fixed seed: identical colors on every render, so demo videos are reproducible.
	private static final long COLOR_SEED = 2017;

	/** Recursion depth driven by the iterations field, capped at the 2017 depth. */
	static int depth(int itMax) {
		return Math.max(0, Math.min(itMax, MAX_DEPTH));
	}

	// Full brightness keeps the curve readable on the black background.
	private static java.awt.Color brightColor(Random r) {
		return java.awt.Color.getHSBColor(r.nextFloat(), 0.8f, 1f);
	}

	/** Segment length that keeps the whole curve the same size at every depth. */
	static double segmentLength(int depth) {
		return FULL_DEPTH_SEGMENT * Math.pow(Math.sqrt(2), MAX_DEPTH - depth);
	}
	
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

		int depth = depth(mm.getIT_MAX());
		String axiom = "F";
		for (int i =0; i<depth ; i++){
			axiom = "+" + axiom + "--" + axiom + "+";
		}
		
		Random r = new Random(COLOR_SEED);
		// Start with a color: at shallow depths no 'F' falls on a multiple of 40, so black stayed black.
		java.awt.Color custom = brightColor(r);
		// 1 px lines vanished once the 2000 px texture was scaled down for videos.
		g.setStroke(new java.awt.BasicStroke(3f));
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		
		double length = segmentLength(depth);
		double currentAngle = 0;
		// Start point chosen so the whole curve (1442 x 900 px) is centered in the image.
		double x = 638;
		double y = 772;
		
		for (int i = 0; i < axiom.length(); i++){
			char step = axiom.charAt(i);
			switch (step){
			case 'F':
				double x2 = x + (length * Math.cos(currentAngle));
				double y2 = y + (length * Math.sin(currentAngle));
				if (i%40 == 0)
					custom = brightColor(r);
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