package se.sperber.cryson.exception;

import javax.ws.rs.core.Response;

public class CrysonEntityNotFoundException extends CrysonException {

  public CrysonEntityNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public int getStatusCode() {
    return Response.Status.NOT_FOUND.getStatusCode();
  }

}
