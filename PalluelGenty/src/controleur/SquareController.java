package controleur;

import modele.Modele;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.awt.Graphics2D;

public class SquareController extends MainController{
	
	public SquareController(){
	}

    public void zoom(int sens){
        System.out.println(sens==1 ? "PLUS" : "MOINS");
		mm.increaseZOOM(sens==1 ? -mm.getIncreaseNumber() : mm.getIncreaseNumber());
		//mm.setIT_MAX(sens==1 ? mm.getIT_MAX()+1 : mm.getIT_MAX()-1);
    }
    
    private class ThreadCalculs extends Thread{

		private int nb;
		private Graphics2D g;

		public ThreadCalculs(int nb, Graphics2D g, double x, double y, double length, int itr, Random r){
			this.nb=nb;
			System.out.println("CONSTR");
			this.run(g,x,y,length,itr,r);
		}


		public void run(Graphics2D g, double x, double y, double length, int itr, Random r) {
			switch(nb) {
			case 0:
				paintSetRecur(g,x,y,length,itr,r);
				paintSetRecur(g,x,y-length,length,itr,r);
				paintSetRecur(g,x+length,y,length,itr,r);
				break;
			case 1:
				paintSetRecur(g,x,y+length,length,itr,r);
				paintSetRecur(g,x-length,y,length,itr,r);
				break;
			case 2:
				paintSetRecur(g,x + 2*length,y-length,length,itr,r);
				paintSetRecur(g,x-length,y-2*length,length,itr,r);
				break;
			case 3:
				paintSetRecur(g,x-2*length,y+length,length,itr,r);
				paintSetRecur(g,x+length,y+2*length,length,itr,r);
				break;
			default:
				break;
			}
		}

	}

    @Override
	public WritableImage paintSet() {
		mm.setArrayColor();

		long debut = System.currentTimeMillis();
		
		BufferedImage image = new BufferedImage(Modele.getWIDTH(), Modele.getHEIGHT(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		
		double length = 450.0/3;
		double x = 700+length;
		double y = 300+length;
		Random r = new Random();

		Thread t1 = new ThreadCalculs(0, g, x, y, length, 6, r);
		Thread t2 = new ThreadCalculs(1, g, x, y, length, 6, r);
		Thread t3 = new ThreadCalculs(2, g, x, y, length, 6, r);
		Thread t4 = new ThreadCalculs(3, g, x, y, length, 6, r);
		
		//paintSetRecur(g,x,y,length,6,r);
		
		System.out.println((System.currentTimeMillis()-debut)/1000f + " s");
		return convertBufferedImage(image);

	}
	
	public void paintSetRecur (Graphics2D g, double x, double y, double length, int itr, Random r){
		if (itr == 3){
			g.setColor(new java.awt.Color(r.nextInt(254),r.nextInt(254),r.nextInt(254)));
		}
		if (itr ==1){
			g.fillRect((int) Math.floor(x),(int) Math.floor(y),(int) Math.ceil(length),(int) Math.ceil(length));
		}
		else{
			length = length/3;
			itr -= 1;
			x = x + length;
			y = y + length;
			
			paintSetRecur(g,x,y,length,itr,r);
			paintSetRecur(g,x,y-length,length,itr,r);
			paintSetRecur(g,x+length,y,length,itr,r);
			paintSetRecur(g,x,y+length,length,itr,r);
			paintSetRecur(g,x-length,y,length,itr,r);
			paintSetRecur(g,x + 2*length,y-length,length,itr,r);
			paintSetRecur(g,x-length,y-2*length,length,itr,r);
			paintSetRecur(g,x-2*length,y+length,length,itr,r);
			paintSetRecur(g,x+length,y+2*length,length,itr,r);
		}
	}
	
	

	

}