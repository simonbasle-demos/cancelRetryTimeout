package org.example;

import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

/**
 * @author Simon Baslé
 */
public class RequestBar extends StackPane {

	private final InternalRequest parent;
	private final ProgressBar bar;
	private final Text text;

	public RequestBar(InternalRequest parent, ProgressBar bar, Text text) {
		super(bar, text);
		this.parent = parent;
		this.bar = bar;
		this.text = text;

		bar.setMaxWidth(Double.MAX_VALUE);
	}

	public ProgressBar getBar() {
		return bar;
	}

	public Text getText() {
		return text;
	}

	public boolean isDone() {
		return parent.get();
	}
}
