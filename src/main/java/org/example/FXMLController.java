package org.example;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
	private VBox progressBox;

	@FXML
	private Button cancelButton;

	@FXML
	private Button makeButton;

	private final AtomicInteger     count     = new AtomicInteger();
	private final Deque<Disposable> workQueue = new ArrayDeque<>();

	private static final int DEFAULT_LABEL_PADDING = 5;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		makeButton.setDefaultButton(true);
		makeButton.setOnAction(this::makeRequest);
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(this::cancelRequest);
	}

	void makeRequest(ActionEvent event) {
		if (typeFuture.isSelected()) {
			CompletableFuture<String> future = internalMakeRequest("ResponseFuture" + count.incrementAndGet())
					.toFuture();

			future.thenAcceptAsync(System.out::println);
		}
		else if (typeBlocking.isSelected()) {
			String response = internalMakeRequest("ResponseBlocking" + count.incrementAndGet())
					.block();
			System.out.println(response);
		}
		else {
			Mono<String> request = internalMakeRequest("ResponseReactive" + count.incrementAndGet());
			request.subscribe(System.out::println);
		}
	}

	void cancelRequest(ActionEvent event) {
		Disposable d = workQueue.pollLast();
		if (d != null) {
			d.dispose();
		}
	}

	Mono<String> internalMakeRequest(final String id) {
		Duration totalDuration = Duration.ofSeconds(10);
		Duration stepDuration = totalDuration.dividedBy(100);

		ProgressBar bar = new ProgressBar();
		bar.setMaxWidth(Double.MAX_VALUE);
		Text text = new Text("0%");
		StackPane pane = new StackPane(bar, text);
		progressBox.getChildren().add(pane);

		return Flux.interval(stepDuration)
		           .take(100)
		           .doOnCancel(() -> cancel(bar, text, pane, id))
		           .doOnNext(tick -> updateProgress(bar, text, tick.intValue(), id))
		           .doOnError(error -> fail(bar, text, error.getMessage(), id))
		           .doOnComplete(() -> updateProgress(bar, text, 100, id))
		           .doOnSubscribe(s -> workQueue.add(s::cancel))
		           .then()
		           .thenReturn(id);
	}

	void cancel(ProgressBar bar, Text text, StackPane barPane, String id) {
		Platform.runLater(() -> {
			text.setText(id + " cancelled!!");
			text.setFill(Color.WHITE);
			bar.setProgress(1d);
			bar.setStyle("-fx-accent: orange");
		});
		Schedulers.single()
		          .schedule(() -> Platform.runLater(() -> progressBox.getChildren().remove(barPane)),
				          5, TimeUnit.SECONDS);
	}

	void updateProgress(ProgressBar bar, Text text, int progress, String id) {
		Platform.runLater(() -> {
			bar.setProgress(progress / 100d);

			if (progress >= 100) {
				bar.setStyle("-fx-accent: green");
				text.setText(id + " done!");
				text.setFill(Color.WHITE);
			}
			else {
				bar.setStyle("");
				text.setText(progress + "%");
				if (progress >= 50) {
					text.setFill(Color.WHITE);
				}
				else {
					text.setFill(Color.BLACK);
				}
			}

			bar.setMinHeight(text.getBoundsInLocal().getHeight() + DEFAULT_LABEL_PADDING * 2);
			bar.setMinWidth (text.getBoundsInLocal().getWidth()  + DEFAULT_LABEL_PADDING * 2);
		});
	}

	void fail(ProgressBar bar, Text text, String message, String id) {
		Platform.runLater(() -> {
			bar.setProgress(1d);
			bar.setStyle("-fx-accent: orange");
			text.setText(message);

			bar.setMinHeight(text.getBoundsInLocal().getHeight() + DEFAULT_LABEL_PADDING * 2);
			bar.setMinWidth (text.getBoundsInLocal().getWidth()  + DEFAULT_LABEL_PADDING * 2);
		});
	}

}