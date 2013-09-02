package se.sperber.cryson.service.exceptionmappers;

import java.text.ParseException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class ParseExceptionMapper extends CrysonExceptionMapperBase<ParseException> {

  @Override
  protected Response toResponseInternal(ParseException e) {
    return Response.status(Response.Status.BAD_REQUEST).entity(buildJsonMessage(e.getMessage())).build();
  }
}
