package org.example;

import javafx.scene.layout.Pane;

/**
 * @author Simon Baslé
 */
public interface GraphicalRequest {

	void guiCancelled();

	void guiCompleted(String result);

	void guiFailed(Throwable error);

	String getId();

	void showOn(Pane gui);

}
