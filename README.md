#Demo code for "Cancel, Retry and Timeouts" talk
_Keep Your Sanity Thanks to Reactive Programming_

Talk first given at virtual SpringOne 2020 conference.

## Running the demo
Simply launch the `run` gradle task:

```
./gradlew run
```

The interesting parts of the code are mostly in `FXMLController.java`:
 - `makeRequest` for 3 modes of simple requests and how to add "listeners" to update the GUI
 - `makeComplexRequest` to see more complex Reactive example (play with `timeout` and `retry`/`retryWhen` operators there!)
 - `cancelRequest` to see manual cancellation: it picks an object in the `workQueue` and detects how to cancel it
 depending on whether it is a `CompletableFuture` or a `Disposable`
 
 All these `makeRequestXxx` methods end up adding an object to the `workQueue` to handle "cancel most recent running request" use case.