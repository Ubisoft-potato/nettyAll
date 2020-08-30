package org.cyka.async;

import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkArgument;

public class PooledAsyncExecutor implements AsyncExecutor {

  private final Executor executor;

  @Override
  public <T> AsyncResult<T> startProcess(Callable<T> task) {
    return startProcess(task, null);
  }

  @Override
  public <T> AsyncResult<T> startProcess(Callable<T> task, AsyncCallback<T> callback) {
    CompletableResult<T> result = new CompletableResult<>(callback);
    executor.execute(
        () -> {
          try {
            result.setValue(task.call());
          } catch (Exception ex) {
            result.setException(ex);
          }
        });
    return result;
  }

  @Override
  public <T> T endProcess(AsyncResult<T> asyncResult)
      throws ExecutionException, InterruptedException {
    if (!asyncResult.isCompleted()) {
      asyncResult.await();
    }
    return asyncResult.getValue();
  }

  public PooledAsyncExecutor() {
    this.executor =
        new ThreadPoolExecutor(
            1,
            Runtime.getRuntime().availableProcessors(),
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(512),
            new ThreadPoolExecutor.CallerRunsPolicy());
  }

  public PooledAsyncExecutor(int corePoolSize, int maxPoolSize, int taskQueueSize) {
    checkArgument(corePoolSize > 0 && maxPoolSize > 0 && taskQueueSize > 0);
    this.executor =
        new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(taskQueueSize),
            new ThreadPoolExecutor.CallerRunsPolicy());
  }
}
