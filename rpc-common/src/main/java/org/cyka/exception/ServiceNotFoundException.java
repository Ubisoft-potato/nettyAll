package org.cyka.exception;

public class ServiceNotFoundException extends Throwable {

  private final String msg;

  @Override
    public String getMessage() {
    return msg;
    }

  public ServiceNotFoundException(String msg) {
    this.msg = msg;
  }
}
