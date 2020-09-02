package org.example;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Simon Baslé
 */
public class Client {

	private final AtomicInteger     count;

	Client() {
		this.count = new AtomicInteger();
	}

	public FutureRequest futureRequest() {
		return internalMakeRequest("ResponseFuture" + count.incrementAndGet());
	}

	public BlockingRequest blockingRequest() {
		return internalMakeRequest("ResponseBlocking" + count.incrementAndGet());
	}

	public ReactiveRequest reactiveRequest() {
		return internalMakeRequest("ResponseReactive" + count.incrementAndGet());
	}

	private InternalRequest internalMakeRequest(final String id) {
		return new InternalRequest(id, Duration.ofSeconds(3));
	}
}
