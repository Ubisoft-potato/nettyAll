package org.cyka.async;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.google.common.collect.Lists.newArrayList;

public class CompletableResult<T> implements AsyncResult<T> {
  static final int RUNNING = 1;
  static final int FAILED = 2;
  static final int COMPLETED = 3;

  final Object lock;
  final List<AsyncCallback<T>> callbacks;

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
    for (AsyncCallback<T> callback : this.callbacks) {
      callback.onComplete(value, Optional.empty());
    }
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  public void setException(Exception exception) {
    this.exception = exception;
    this.state = FAILED;
    // invoke the call back method if AsyncCallback not empty
    for (AsyncCallback<T> callback : this.callbacks) {
      callback.onComplete(null, Optional.of(exception));
    }
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  @Override
  public void addCallback(AsyncCallback<T> callback) {
    // todo check value already set
    if (Objects.nonNull(callback)) this.callbacks.add(callback);
  }

  public CompletableResult() {
    this.lock = new Object();
    this.callbacks = newArrayList();
  }

  public CompletableResult(AsyncCallback<T> callback) {
    this.lock = new Object();
    this.callbacks = newArrayList();
    if (callback != null) callbacks.add(callback);
  }
}
