package application;

import controleur.MainController;
import javafx.stage.Stage;
import modele.Modele;
import vue.SphereViewer;

public class Application extends javafx.application.Application{

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Modele mm = new Modele();
		MainController mc = new MainController(mm);
		SphereViewer sphere = new SphereViewer(mc, mm);
	}


}