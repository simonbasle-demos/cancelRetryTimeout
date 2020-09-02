package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author Simon Baslé
 */
public class MainApp extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("scene.fxml"));

		Scene scene = new Scene(root);
//		scene.getStylesheets()
//		     .add(getClass().getResource("styles.css")
//		                    .toExternalForm());

		stage.setTitle("Cancel, Retry and Timeouts");
		stage.setScene(scene);
		stage.show();
	}

}
