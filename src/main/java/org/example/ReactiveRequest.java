package org.example;

import reactor.core.publisher.Mono;

/**
 * @author Simon Baslé
 */
public interface ReactiveRequest extends GraphicalRequest {

	Mono<String> exchangeReactive();

	Mono<String> exchangeInnerReactive();

}
