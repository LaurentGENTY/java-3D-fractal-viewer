package vue;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;

import controleur.MainController;
import exceptions.NombreNegatifException;
import exceptions.WrongFormatException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import modele.Modele;
import modele.Modele.ListControllers;

// ************************************************************************
// 							CODE SOURCE DE LA SPHERE
//			https://gist.github.com/jewelsea/7206287 : credits -> jewelsea
//						Modifie par Palluel Luis et Genty Laurent
// 								le 15/11/17
// ************************************************************************

public class SphereViewer extends Stage  implements Observer{

	// Attributs du MVC
	private MainController mc;
	private Modele mm;

	// Scene qui va contenir tous les elements de la vue
	private Scene scene;

	// Groupe contenant la sphere
	private Group group;

	// Attributs de la sphere + vue
	private static int GLOBALWIDTH = 1200;
	private static int GLOBALHEIGHT = 1000;
	private static int SPHERE_RADIUS  = GLOBALWIDTH/4;
	private Sphere sphere = new Sphere(SPHERE_RADIUS,1000);
	private int WIDTH_BOX_LEFT = GLOBALWIDTH/4;
	private int HEIGHT_BOX_LEFT = GLOBALHEIGHT;

	// Liste des pane qui seront presents dans le groupe
	private Pane pane;
	private GridPane grid;

	// Liste des boutons presents sur la vue
	private VBox boxParameters;
	private ScrollBar scroll;
	
	// Zoom
	private HBox buttonsZooms;
	private Button zoomPlus;
	private Button zoomMoins;
	
	// Fractales
	private RadioButton julia;
	private RadioButton levy;
	private RadioButton square;
	private RadioButton mandelBrot;
	private ToggleGroup changeFractale;
	
	// Rotations
	private Label enX;
	private Label enY;
	private Slider sliderX;
	private Slider sliderY;
	
	// Iterations
	private Label nombreIte;
	private TextField ite;
		
	// Parametres
	private Label alphaText;
	private TextField alpha;
	private Label betaText;
	private TextField beta;
	private ColorPicker colorPicker;
	
	// Autres
	private Button reset;
	
	// Boutton valider
	private Button validateParam;
	
	// Attributs generant la rotation selon la souris
	private double mouseX;
	private double mouseY;
	private double lastRotateX;
	private double lastRotateY;
	private double x;
	private double y;
	
	// Camera
	private PerspectiveCamera camera = new PerspectiveCamera();

	// Attribut gerant le zoom de la sphere
	private int Z_ZOOM = 0;

	// Attributs de sauvegarde
	private File file;
	private Button save;
	
	// Attributs autres
	private RadioButton currentFractale;
	private ScrollPane scrollPane;

	// CONSTRUCTEUR

	public SphereViewer(MainController mc, Modele mm) {

		this.setTitle("PROJET FRACTALE PALLUEL - GENTY");
		
		// Affectation du controleur et modele
		this.mm = mm;
		this.mc = mc;

		// On ajoute en tant qu'observer nous meme Ãƒ   l'observable modele
		mm.addObserver(this);

		// Creation du menu de gauche
		boxParameters = new VBox(20);
		boxParameters.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
		boxParameters.setPrefWidth(WIDTH_BOX_LEFT);
		boxParameters.setPrefHeight(HEIGHT_BOX_LEFT);
		boxParameters.setPadding(new Insets(0, 5, 0, 5));
		boxParameters.isResizable();		
		
		scrollPane = new ScrollPane();
		scrollPane.setFitToHeight(true);
		
		
		// Boutons dans le menu de gauche
		buttonsZooms = new HBox();
		
		//Boutons Zoom
		zoomPlus = new Button("Zoom +");
		zoomPlus.isResizable();
		zoomMoins = new Button("Zoom -");
		zoomMoins.isResizable();
		save = new Button("Sauvegarder");
		
		// Ajout des boutons zoom
		buttonsZooms.getChildren().addAll(zoomPlus,zoomMoins,save);
		buttonsZooms.setPadding(new Insets(0,5,0,5));

		// Listes fractales
		mandelBrot = new RadioButton("MandelBrot");
		mandelBrot.setToggleGroup(changeFractale);
		mandelBrot.setSelected(true);
		julia = new RadioButton("Julia");
		julia.setToggleGroup(changeFractale);
		levy = new RadioButton("Levy");
		levy.setToggleGroup(changeFractale);
		square = new RadioButton("Square");
		square.setToggleGroup(changeFractale);
		
		currentFractale = mandelBrot;

		// Boutons Slider
		enX = new Label("Rotation X : 0°");
		sliderX = new Slider();
		sliderX.setMax(360);
		sliderX.setMin(0);
		sliderX.setValue(0);
		sliderX.setShowTickLabels(true);
		sliderX.setShowTickMarks(true);
		sliderX.setMajorTickUnit(45);

		enY = new Label("Rotation Y : 0°");
		sliderY = new Slider();
		sliderY.setMax(360);
		sliderY.setMin(0);
		sliderY.setValue(0);
		sliderY.setShowTickLabels(true);
		sliderY.setShowTickMarks(true);
		sliderY.setMajorTickUnit(45);

		// Iterations
		nombreIte = new Label("Changer nombre iterations");
		ite = new TextField(Integer.toString(mm.getIT_MAX()));
		alphaText = new Label("Alpha");
		alpha = new TextField();
		alpha.setText(Double.toString((double) Modele.getZ()));
		betaText = new Label("Beta");
		beta = new TextField();
		beta.setText(Double.toString((double) Modele.getZi()));

		// Couleurs
		colorPicker = new ColorPicker();
		colorPicker.isResizable();
		
		// Bouton valider
		validateParam = new Button("Valider les parametres");

		// Bouton reset
		reset = new Button("RESET");
		reset.isResizable();

		// Ajout de tous les elements dans le menu de gauchee
		boxParameters.getChildren().addAll(buttonsZooms, new Separator(),
				mandelBrot, julia, levy, square, enX, sliderX,enY,sliderY, new Separator(),
				nombreIte, ite, alphaText,alpha, betaText, beta, colorPicker, validateParam, reset);

		// Creation de sphere
		group = buildSphere();
		sphere.setTranslateZ(0);
		// TEST ZOOM DE BASE
		//-1055
		
		
		// Creation du pane correspondant a la fractale
		pane = new Pane();
		pane.getChildren().add(group);
		pane.setPrefWidth(GLOBALWIDTH/4*3);
		pane.setPrefHeight(GLOBALHEIGHT);
		pane.setLayoutX(WIDTH_BOX_LEFT+(GLOBALWIDTH/4));


		// Grille de l'ecran
		grid = new GridPane();
		grid.add(pane, 1, 0);
		grid.add(boxParameters, 0, 0);

		
		// Mise en place de la scene
		scene = new Scene(grid,GLOBALWIDTH,GLOBALHEIGHT);
		scene.setCamera(camera);

		/*URL url = this.getClass().getResource("view.css");
	    if (url == null) {
	        System.out.println("Resource not found. Aborting.");
	        System.exit(-1);
	    }
	    String css = url.toExternalForm(); 
	    scene.getStylesheets().add(css);*/
	    
	    scene.getStylesheets().add("src/application/view.css");
	    
	    setScene(scene);
		show();

		// ********************************************************
		//		PARTIE REGROUPANT LES EVENEMENTS SUR LA VUE
		// ********************************************************

		// *****************************************
		//				RACCOURCIS CLAVIER
		// *****************************************

		scene.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.NUMPAD1) {
				System.out.println("PLUS");
				zoom();
			}
			if (event.getCode() == KeyCode.NUMPAD2) {
				System.out.println("MINUS");
				deZoom();
			}
			if (event.getCode() == KeyCode.ENTER) {

				//
			}
		});
		
		// Si on appuie sur la souris on recupere les coordonnees
		pane.setOnMousePressed(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent mouse)
			{
				mouseX = mouse.getSceneX();
				mouseY = mouse.getSceneY();
			}
		});
		
		// Si on clique glisse on recupere les valeurs des differnces entre chaque frame de notre drag and drop afin de savoir dans quel sens on va
		pane.setOnMouseDragged(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent mouse)
			{
				
				// On va calculer la difference entre le moment ou l'on a appuye sur la souris et le deplacement actuel
				x = lastRotateX + (mouseX - mouse.getSceneX())/10.;
				y = lastRotateY - (mouseY - mouse.getSceneY())/10.;
				
				sphere.getTransforms().clear();	

				sphere.getTransforms().add(new Rotate(y, new Point3D(1, 0, 0)));
				sphere.getTransforms().add(new Rotate(x, new Point3D(0, 1, 0)));
			}

		});
		
		// Quand on lache la souris cela veut dire qu'on sauvegarde la rotation
		pane.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				lastRotateX = x;
				lastRotateY = y;
			}
		});


		// Zoomer sur la fractale
		zoomPlus.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				Z_ZOOM -=100;
				sphere.setTranslateZ(Z_ZOOM);
				sphere.setTranslateX(sphere.getTranslateX()-12);
			}
		});

		// Dezoomer sur la fractale
		zoomMoins.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				Z_ZOOM +=100;
				sphere.setTranslateZ(Z_ZOOM);
				sphere.setTranslateX(sphere.getTranslateX()+12);
			}
		});
		

		// *************************************************
		// 			SAUVEGARDER IMAGE FRACTALE
		// *************************************************

		save.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				try
				{
					file = new File(mc.getCurrentController().toString()+".png");
					ImageIO.write(SwingFXUtils.fromFXImage(mc.getCurrentController().paintSet(), null), "png", file);
					System.out.println("FRACTALE ACTUELLE SAUVEGARDE DANS LE DOSSIER DE COMPILATION");
                } catch (IOException ex) {
                	System.err.println(ex);
                }
				 
			}
		});


		// *************************************************
		// 				CHANGEMENTS DE FRACTALES
		// *************************************************
		
		mandelBrot.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				deselectLast(mandelBrot);
				mc.setCurrentController(ListControllers.MANDELBROT);
			}
		});

		julia.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				deselectLast(julia);
				mc.setCurrentController(ListControllers.JULIA);
			}
		});

		levy.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				deselectLast(levy);
				mc.setCurrentController(ListControllers.LEVY);
			}
		});

		square.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				deselectLast(square);
				mc.setCurrentController(ListControllers.SQUARE);
			}
		});

		// ****************************************************
		// 					EVENEMENTS SLIDERS
		// ****************************************************
		
		// Selon la position du curseur sur le slider on rotate
		sliderX.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				enX.textProperty().setValue("Rotation X : " + String.valueOf((int)sliderX.getValue()+ "°"));
			}
		});

		sliderY.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
			}
		});
		
		// Si on clique sur le bouton valideParam on valide TOUS les parametres 
		validateParam.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				
				if(isNumeric(ite.getText()))
				{
					int iterations = Integer.parseInt(ite.getText());
					try {
						mm.setIT_MAX(iterations);
					} catch (NombreNegatifException e) {
						System.err.println(e);
					}
				}
				else
				{
					System.err.println("Mauvais format pour le nombre d'itérations : " + ite.getText());
				}
				if(alpha.getText() != "")
				{
					if(isNumeric(alpha.getText()))
					{	
						Modele.setZ(Double.parseDouble(alpha.getText()));
					}
					else
					{
						System.err.println("MAUVAIS FORMAT : Alpha");
					}
				}
				if(beta.getText() != "")
				{
					if(isNumeric(beta.getText()))
					{
						Modele.setZi(Double.parseDouble(beta.getText()));
					}
					else
					{
						System.err.println("MAUVAIS FORMAT : Beta");
					}
				}
			}
		});
		

		// *************************************************
		// 					RESET SCENE
		// *************************************************
		
		reset.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				resetALL();
			}
		});

	}

	
	// ****************************************
	// 			FONCTIONS ZOOM / DEZOOM
	// ****************************************
	
	private void deZoom() {
		Z_ZOOM +=100;
		sphere.setTranslateZ(Z_ZOOM);
	}

	private void zoom() {
		Z_ZOOM -=100;
		sphere.setTranslateZ(Z_ZOOM);
	}


	// ***********************************************************
	// 						FONCTION BUILDER SPHERE
	// ***********************************************************
	
	private Group buildSphere() {

		// On place la sphere au milieu de l'ecran de droite
		sphere.setTranslateX(WIDTH_BOX_LEFT + SPHERE_RADIUS/2);
		sphere.setTranslateY(HEIGHT_BOX_LEFT / 2);

		// On cree une image JavaFX de taille Modele.getWIDTH / Modele. getHEIGHT
		WritableImage image = new WritableImage(Modele.getWIDTH(),Modele.getHEIGHT());

		// On affecte cette image a  l'image de la fractale actuelle
		image = mc.getCurrentController().paintSet();

		// On cree un instance de PhongMaterial --> sphere avec ombrage de Phong
		PhongMaterial sphereMaterial = new PhongMaterial();

		// En tant que diffuse map ("image classique") on plaque notre image du controleur
		
		sphereMaterial.setSpecularPower(500);
		sphereMaterial.setDiffuseMap(image);
		//sphereMaterial.setBumpMap(image);
		//sphereMaterial.setSpecularMap(image);

		sphere.setMaterial(sphereMaterial);

		return new Group(sphere);
	}


	// ********************************************************
	//					FONCTIONS UPDATE OBSERVER
	// ********************************************************

	// Une fois qu'on a change la fractale a savoir changer le nombre d'itérations ou bien le zoom, on doit alors refresh l'image sur la sphere
	@Override
	public void update(Observable o, Object arg) {
		refreshSphere();
	}

	// Fonction qui change l'image de la sphere
	public void refreshSphere()
	{
		group = buildSphere();
		pane.getChildren().add(group);
	}

	//**********************************************
	//					FUNCTIONS UTILS
	// *********************************************

	// Fonction permettant de verifier si le string est un entier
	public static boolean isNumeric(String str)  
	{  
		try  
		{  
			double d = Double.parseDouble(str);  
		}  
		catch(NumberFormatException nfe)  
		{  
			return false;  
		}  
		return true;  
	}
	
	// Fonction qui desectionne le dernier radio button
	private void deselectLast(RadioButton rd) {
		currentFractale.setSelected(false);
		currentFractale = rd;
	}
	
	// Fonction qui reinitialise tous les parametres
	private void resetALL() {
		sphere.setTranslateX(WIDTH_BOX_LEFT + SPHERE_RADIUS/2);
		sphere.setTranslateY(HEIGHT_BOX_LEFT / 2);
		sphere.setTranslateZ(0);
		
		sphere.getTransforms().clear();
		lastRotateX = 0;
		lastRotateY = 0;
		x = 0;
		y = 0;
		
		try {
			mm.setIT_MAX(50);
		} catch (NombreNegatifException e) {
			e.printStackTrace();
		}
		Modele.setZ(0.3);
		Modele.setZi(-0.5);
		refreshSphere();				
	}
}
