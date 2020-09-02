package org.example;

import java.util.concurrent.CompletableFuture;

/**
 * @author Simon Baslé
 */
public interface FutureRequest extends GraphicalRequest {

	CompletableFuture<String> exchangeFuture();

}
