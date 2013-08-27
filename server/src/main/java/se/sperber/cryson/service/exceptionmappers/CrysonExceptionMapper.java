package se.sperber.cryson.service.exceptionmappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import se.sperber.cryson.exception.CrysonException;

@Provider
public class CrysonExceptionMapper extends CrysonExceptionMapperBase<CrysonException> {
  
  @Override
  protected Response toResponseInternal(CrysonException e) {
    return translateCrysonException(e);
  }
}
