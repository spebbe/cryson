package se.sperber.cryson.service.exceptionmappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.security.access.AccessDeniedException;

@Provider
public class AccessDeniedExceptionMapper extends CrysonExceptionMapperBase<AccessDeniedException> {

  @Override
  protected Response toResponseInternal(AccessDeniedException e) {
    return Response.status(Response.Status.UNAUTHORIZED).entity(buildJsonMessage(e.getMessage())).build();
  }
}
