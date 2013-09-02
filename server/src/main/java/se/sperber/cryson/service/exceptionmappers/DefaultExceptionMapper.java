package se.sperber.cryson.service.exceptionmappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class DefaultExceptionMapper extends CrysonExceptionMapperBase<Throwable> {

  @Override
  protected Response toResponseInternal(Throwable e) {
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(buildJsonMessage(e.getMessage())).build();
  }
}
