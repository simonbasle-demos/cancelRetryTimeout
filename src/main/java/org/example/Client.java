package org.example;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Simon Baslé
 */
public class Client {

	private static final int DEFAULT_LABEL_PADDING = 5;

	private final AtomicInteger     count;
	private final FXMLController    gui;
	private final Deque<Disposable> workQueue;


	Client(FXMLController controller) {
		this.count = new AtomicInteger();
		this.gui = controller;
		this.workQueue = new ArrayDeque<>();
	}

	public CompletableFuture<String> futureRequest() {
		return internalMakeRequest("ResponseFuture" + count.incrementAndGet())
				.toFuture();
	}

	public String blockingRequest() {
		return internalMakeRequest("ResponseBlocking" + count.incrementAndGet())
				.block();
	}

	public Mono<String> reactiveRequest() {
		return internalMakeRequest("ResponseReactive" + count.incrementAndGet());
	}

	public void cancelLastRequest() {
		Disposable d = workQueue.pollLast();
		if (d != null) {
			d.dispose();
		}
	}

	private Mono<String> internalMakeRequest(final String id) {
		Duration totalDuration = Duration.ofSeconds(10);
		Duration stepDuration = totalDuration.dividedBy(100);

		ProgressBar bar = new ProgressBar();
		bar.setMaxWidth(Double.MAX_VALUE);
		Text text = new Text("0%");
		StackPane pane = new StackPane(bar, text);
		gui.progressBox.getChildren().add(pane);
		pane.setOnMouseClicked(me -> {
			if (me.getClickCount() == 2 && bar.getProgress() == 1d) {
				gui.progressBox.getChildren().remove(pane);
			}
		});

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

	private void cancel(ProgressBar bar, Text text, StackPane barPane, String id) {
		Platform.runLater(() -> {
			text.setText(id + " cancelled!!");
			text.setFill(Color.WHITE);
			bar.setProgress(1d);
			bar.setStyle("-fx-accent: orange");
		});
	}

	private void updateProgress(ProgressBar bar, Text text, int progress, String id) {
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

	private void fail(ProgressBar bar, Text text, String message, String id) {
		Platform.runLater(() -> {
			bar.setProgress(1d);
			bar.setStyle("-fx-accent: orange");
			text.setText(message);

			bar.setMinHeight(text.getBoundsInLocal().getHeight() + DEFAULT_LABEL_PADDING * 2);
			bar.setMinWidth (text.getBoundsInLocal().getWidth()  + DEFAULT_LABEL_PADDING * 2);
		});
	}
}
