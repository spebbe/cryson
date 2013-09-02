package se.sperber.cryson.service.exceptionmappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.hibernate.StaleObjectStateException;

import se.sperber.cryson.exception.CrysonEntityConflictException;

@Provider
public class StaleObjectStateExceptionMapper extends CrysonExceptionMapperBase<StaleObjectStateException> {

  @Override
  protected Response toResponseInternal(StaleObjectStateException e) {
    return translateCrysonException(new CrysonEntityConflictException("Optimistic locking failed", e));
  }
}
