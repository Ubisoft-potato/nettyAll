package org.cyka.async;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class CompletableResult<T> implements AsyncResult<T> {
  static final int RUNNING = 1;
  static final int FAILED = 2;
  static final int COMPLETED = 3;

  final Object lock;
  final Optional<AsyncCallback<T>> callback;

  volatile int state = RUNNING;

  T value;
  Exception exception;

  @Override
  public boolean isCompleted() {
    return state > RUNNING;
  }

  @Override
  public T getValue() throws ExecutionException {
    if (state == COMPLETED) {
      return value;
    } else if (state == FAILED) {
      throw new ExecutionException(exception);
    } else {
      throw new IllegalStateException("Execution not completed yet");
    }
  }

  @Override
  public void await() throws InterruptedException {
    synchronized (lock) {
      while (!isCompleted()) {
        lock.wait();
      }
    }
  }

  @Override
  public void setValue(T value) {
    this.value = value;
    this.state = COMPLETED;
    // invoke the call back method if AsyncCallback not empty
    this.callback.ifPresent(ac -> ac.onComplete(value, Optional.empty()));
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  public void setException(Exception exception) {
    this.exception = exception;
    this.state = FAILED;
    // invoke the call back method if AsyncCallback not empty
    this.callback.ifPresent(ac -> ac.onComplete(null, Optional.of(exception)));
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  public CompletableResult(AsyncCallback<T> callback) {
    this.lock = new Object();
    this.callback = Optional.ofNullable(callback);
  }
}
