package org.example;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Simon Baslé
 */
class InternalRequest extends AtomicBoolean
		implements FutureRequest, BlockingRequest, ReactiveRequest {

	private static final int DEFAULT_LABEL_PADDING = 5;

	private final String id;
	private final Duration stepDuration;

	private final ProgressBar bar;
	private final Text text;

	private RequestBar guiContainer;

	public InternalRequest(String id, Duration duration) {
		this.id = id;
		this.stepDuration = duration.dividedBy(100);

		this.bar = new ProgressBar();
		this.text = new Text("0%");
	}

	@Override
	public void showOn(Pane gui) {
		this.guiContainer = new RequestBar(this, this.bar, this.text);
		gui.getChildren().add(guiContainer);
		guiContainer.setOnMouseClicked(me -> {
			if (me.getClickCount() == 2 && bar.getProgress() == 1d) {
				gui.getChildren().remove(guiContainer);
			}
		});
		bar.setOnMouseClicked(guiContainer.getOnMouseClicked());
	}

	@Override
	public String exchangeBlocking() {
		return mono().block();
	}

	@Override
	public CompletableFuture<String> exchangeFuture() {
		return mono().toFuture();
	}

	@Override
	public Mono<String> exchangeReactive() {
		return mono();
	}

	@Override
	public Mono<String> exchangeInnerReactive() {
		guiContainer.setInner(true);
		return mono();
	}

	public Mono<String> mono() {
		return Flux.interval(Duration.ZERO, stepDuration)
		           .take(100)
		           .doOnNext(this::guiUpdateProgress)
		           .last()
		           .thenReturn(id);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return id + '{' + (get() ? "stopped" : "running") + '}';
	}

	@Override
	public void guiCancelled() {
		if (compareAndSet(false, true)) {
			Platform.runLater(() -> {
				text.setText(id + " cancelled!!");
				text.setFill(Color.WHITE);
				bar.setProgress(1d);
				bar.setStyle("-fx-accent: orange");
			});
		}
	}

	@Override
	public void guiCompleted(String result) {
		if (compareAndSet(false, true)) {
			guiUpdateProgress(100);
			System.out.println(result);
		}
	}

	@Override
	public void guiFailed(Throwable error) {
		if (compareAndSet(false, true)) {
			Platform.runLater(() -> {
				bar.setProgress(1d);
				bar.setStyle("-fx-accent: red");
				text.setFill(Color.BLACK);
				text.setText(error.toString());

				bar.setMinHeight(text.getBoundsInLocal().getHeight() + DEFAULT_LABEL_PADDING * 2);
				bar.setMinWidth (text.getBoundsInLocal().getWidth()  + DEFAULT_LABEL_PADDING * 2);
			});
		}
	}

	private void guiUpdateProgress(Number p) {
		final int progress = p.intValue();

		Platform.runLater(() -> {
			bar.setProgress(progress / 100d);
			if (progress >= 100) {
				bar.setStyle("-fx-accent: green");
				text.setText(id + " done!");
				text.setFill(Color.WHITE);
			}
			else {
				bar.setStyle(guiContainer.getProgressStyle());
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

}
