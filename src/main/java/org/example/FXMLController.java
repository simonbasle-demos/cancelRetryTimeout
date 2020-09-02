package org.example;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import reactor.core.publisher.Mono;

public class FXMLController implements Initializable {

	@FXML
	private RadioButton typeBlocking;

	@FXML
	private ToggleGroup type;

	@FXML
	private RadioButton typeFuture;

	@FXML
	private RadioButton typeFlux;

	@FXML
	VBox progressBox;

	@FXML
	private Button cancelButton;

	@FXML
	private Button makeButton;

	private Client client;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		makeButton.setDefaultButton(true);
		makeButton.setOnAction(this::makeRequest);
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(this::cancelRequest);
		typeFlux.setSelected(true);

		client = new Client(this);
	}

	void makeRequest(ActionEvent event) {
		if (typeFuture.isSelected()) {
			CompletableFuture<String> future = client.futureRequest();
			future.thenAcceptAsync(System.out::println);
		}
		else if (typeBlocking.isSelected()) {
			String response = client.blockingRequest();
			System.out.println(response);
		}
		else {
			Mono<String> request = client.reactiveRequest();
			request.subscribe(System.out::println);
		}
	}

	void cancelRequest(ActionEvent event) {
		client.cancelLastRequest();
	}

}