package org.cyka.exception;

public class ServiceNotFoundException extends Throwable {
  public ServiceNotFoundException(String msg) {
    super(msg);
  }
}
