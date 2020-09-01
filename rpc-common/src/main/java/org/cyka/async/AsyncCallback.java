package org.cyka.async;

import java.util.Optional;

public interface AsyncCallback<T> {

  void onComplete(T value, Optional<Exception> ex) throws Exception;
}
