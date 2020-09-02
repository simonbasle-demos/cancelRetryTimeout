package org.example;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.reactivestreams.Subscription;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.BaseSubscriber;
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
	VBox progressPane;

	@FXML
	ScrollPane scrollPane;

	@FXML
	private Button cancelButton;

	@FXML
	private Button clearButton;


	@FXML
	private Button makeButton;

	private Client client;
	private ObservableList<Object> workQueue;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		makeButton.setDefaultButton(true);
		makeButton.setOnAction(this::makeRequest);
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(this::cancelRequest);
		clearButton.setOnAction(this::clearDone);

		typeFlux.setSelected(true);

		client = new Client();
		workQueue = FXCollections.observableList(new ArrayList<>());
		workQueue.addListener(this::workQueueChanged);
	}

	private void workQueueChanged(ListChangeListener.Change<? extends Object> change) {
		Object nextToCancel = null;

		while (change.next()) {
			if (change.wasAdded()) {
				nextToCancel = change.getAddedSubList().get(change.getAddedSize() - 1);
			}
			else if (change.wasRemoved()) {
				ObservableList<?> list = change.getList();
				int size = list.size();
				if (size > 0) {
					nextToCancel = list.get(size - 1);
				}
			}

			if (nextToCancel == null) {
				cancelButton.setText("Cancel (Nothing)");
				cancelButton.setDisable(true);
				return;
			}

			cancelButton.setDisable(false);
			if (nextToCancel instanceof CompletableFuture) {
				CompletableFuture<?> f = (CompletableFuture<?>) nextToCancel;
				if (f.isDone()) {
					Platform.runLater(() -> workQueue.remove(f));
				}
				else {
					cancelButton.setText("Cancel a Future");
				}
			}
			else if (nextToCancel instanceof Disposable) {
				Disposable d = (Disposable) nextToCancel;
				if (d.isDisposed()) {
					Platform.runLater(() -> workQueue.remove(d));
				}
				else {
					cancelButton.setText("Cancel a Mono");
				}
			}
			else {
				cancelButton.setText("Cannot cancel " + nextToCancel.getClass().getSimpleName());
			}
		}
	}

	void makeRequest(ActionEvent event) {
		if (typeFuture.isSelected()) {
			FutureRequest futureRequest = client.futureRequest();
			futureRequest.showOn(progressPane);
			CompletableFuture<String> realWorkFuture = futureRequest
					.exchangeFuture();
			//we have to cheat here as CompletableFuture doesn't propagate cancel upstream
			workQueue.add(realWorkFuture);
			realWorkFuture.whenComplete((v, err) -> {
						if (v != null) {
							futureRequest.guiCompleted(v);
						}
						else if (err instanceof CancellationException) {
							futureRequest.guiCancelled();
						}
						else if (err != null) {
							futureRequest.guiFailed(err);
						}
						Platform.runLater(() -> workQueue.remove(realWorkFuture));
					});
		}
		else if (typeBlocking.isSelected()) {
			BlockingRequest blockingRequest = client.blockingRequest();
			blockingRequest.showOn(progressPane);
			try {
				String response = blockingRequest.exchangeBlocking();
				blockingRequest.guiCompleted(response);
			}
			catch (Exception e) {
				blockingRequest.guiFailed(e);
			}
		}
		else {
			ReactiveRequest reactiveRequest = client.reactiveRequest();
			reactiveRequest.showOn(progressPane);
			Mono<String> reactiveMono = reactiveRequest.exchangeReactive();

			Disposable d = subscribeGuiToMono(reactiveMono, reactiveRequest);
			workQueue.add(d);
		}
	}

	private Disposable subscribeGuiToMono(Mono<String> mono, ReactiveRequest reactiveRequest) {
		Disposable.Swap d = Disposables.swap();

		mono.subscribe(new BaseSubscriber<>() {
			@Override
			protected void hookOnSubscribe(Subscription subscription) {
				d.replace(this);
				requestUnbounded();
			}

			@Override
			protected void hookOnCancel() {
				reactiveRequest.guiCancelled();
			}

			@Override
			protected void hookOnNext(String value) {
				reactiveRequest.guiCompleted(value);
				Platform.runLater(() -> workQueue.remove(d));
			}

			@Override
			protected void hookOnError(Throwable throwable) {
				reactiveRequest.guiFailed(throwable);
				Platform.runLater(() -> workQueue.remove(d));
			}
		});

		return d;
	}

	void cancelRequest(ActionEvent event) {
		int size = workQueue.size();
		if (size == 0) return;

		Object o = workQueue.remove(size - 1);
		if (o instanceof CompletableFuture) {
			CompletableFuture<?> f = (CompletableFuture<?>) o;
			f.cancel(true);
		}
		else if (o instanceof Disposable) {
			Disposable d = (Disposable) o;
			d.dispose();
		}
	}

	void clearDone(ActionEvent event) {
		progressPane.getChildren()
		            .removeIf(node -> node instanceof RequestBar && ((RequestBar) node).isDone());
	}

}