package org.cyka.async;

import java.util.concurrent.ExecutionException;

/**
 * AsyncResult interface.
 *
 * @param <T> parameter returned when getValue is invoked
 */
public interface AsyncResult<T> {

  /**
   * Status of the async task execution.
   *
   * @return <code>true</code> if execution is completed or failed
   */
  boolean isCompleted();

  /**
   * Gets the value of completed async task.
   *
   * @return evaluated value or throws ExecutionException if execution has failed
   * @throws ExecutionException if execution has failed, containing the root cause
   * @throws IllegalStateException if execution is not completed
   */
  T getValue() throws ExecutionException;

  /**
   * Blocks the current thread until the async task is completed.
   *
   * @throws InterruptedException if the execution is interrupted
   */
  void await() throws InterruptedException;


  /**
   * Sets the value from successful execution and executes callback if available. Notifies any
   * thread waiting for completion.
   *
   * @param value value of the evaluated task
   */
  void setValue(T value);

  /**
   * Sets the exception from failed execution and executes callback if available. Notifies any
   * thread waiting for completion.
   *
   * @param exception exception of the failed task
   */
  void setException(Exception exception);
}
