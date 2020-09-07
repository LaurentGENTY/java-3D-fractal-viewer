package modele;
import java.util.ArrayList;
import java.util.Observable;

import exceptions.NombreNegatifException;
import exceptions.NombreNegatifException;
import javafx.scene.paint.Color;

//************************************************************************
//					Cree par Palluel Luis et Genty Laurent
//								le 01/11/17
//************************************************************************

public class Modele extends Observable {
	
	// *********************************************************
	// 			LISTE DE TOUS LES MODELES DE FRACTALES
	// *********************************************************
	public enum ListControllers{
		MANDELBROT,
		JULIA,
		LEVY,
		SQUARE
	}
	
	private ListControllers currentController = ListControllers.MANDELBROT;
	
	// *******************************************************
	// 							ATTRIBUTS
	// *******************************************************
	
	// *******************************************************
	// 				ATTRIBUTS GERANT LA FRACTALE
	// *******************************************************
	
	// Repere
    private static double X1_REEL = -2;
    private static double X2_REEL = 1;
    private static double Y1_IMA = -1.2;
    private static double Y2_IMA = 1.2;
    
    // Parametres
    private static double Z = 0.3;
    private static double Zi = -0.5;
    
    // Zoom
    private static double ZOOM = 1.0;
    
    // Iterations
    private static int IT_MAX = 50;
    
    // Autres
    private final static double increaseNumber = 0.2;
   
    // Couleurs
    private Color convergenceColor = Color.WHITE;
    private Color color[] = new Color[this.getIT_MAX()];

    // ********************************************************
    // 					RESOLUTION DE L IMAGE
    // ********************************************************
    private static int WIDTH =2000;
    private static int HEIGHT=1000;
    
    // ********************************************************
    // 						GETTERS
    // ********************************************************
    
    // TAILLE
    public static int getWIDTH() {
        return WIDTH;
    }

    public static int getHEIGHT() {
        return HEIGHT;
    }

    // PARAMETRES
    public static double getZ() {
        return Z;
    }

    public static double getZi() {
        return Zi;
    }

    // COORDONNEES DU REPERE
    public double getX1_REEL(){
    	return X1_REEL;
    }

    public double getX2_REEL(){
    	return X2_REEL;
    }
    
    public double getY1_IMA(){
    	return Y1_IMA;
    }
    
    public double getY2_IMA(){
    	return Y2_IMA;
    }
    
    // ZOOM
    public double getZOOM(){
    	return ZOOM;
    }
    
    // ITERATIONS
    public int getIT_MAX(){
    	return IT_MAX;
    }
    
    // COULEURS
    public Color getConvergenceColor(){
    	return convergenceColor;
    }

    public Color[] getColor(){
        return color;
    }
    
    // AUGMENTEUR POUR LE ZOOM
    public double getIncreaseNumber(){
    	return increaseNumber;
    }
    
    // FRACTALE ACTUELLE
    public ListControllers getCurrentController(){
    	return currentController;
    }
    
    
    // *********************************************************
    // 							SETTERS
    // *********************************************************

    // ITERATIONS
    public void setIT_MAX(int itMax) throws NombreNegatifException{
    	if(itMax >= 0)
    	{
    		IT_MAX = itMax;
            this.setArrayColor();
            fire();
    	}
    	else
    	{
    		throw new NombreNegatifException("Le nombre d'iterations est negatif ou nul");
    	}
    }

    // ZOOM
    public void increaseZOOM(double zoom){
        ZOOM *= 1+zoom;
        fire();
        System.out.println(getZOOM());
    }
    
    public void setCurrentController(ListControllers c){
    	try{
    		currentController = c;
    		fire();
    	}
    	catch(Exception e){
    		System.out.println("Error : Unknown Controller.");
    		e.printStackTrace();
    	}
    }
    
    public static void setZ(double z)
    {
    	
    	Z = z;
    }
    
    public static void setZi(double zi)
    {
    	Zi = zi;
    }

    public void setArrayColor() {
    	color = new Color[getIT_MAX()];
        for(int i=0; i<this.getIT_MAX();i++){
            color[i]= Color.hsb(i/4d, 1, i/(i+5f));
            //color[i] = Color.rgb(i*5%45, 25, i%100);
        }
    }
    
    // ***********************************************************************************
    // 					FONCTIONS AVERTISSANT LES OBSERVATEURS DU MODELE
    // ***********************************************************************************
    
    private void fire(){
        setChanged();
        notifyObservers();
    }
}