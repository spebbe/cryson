package se.sperber.cryson.service.exceptionmappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.hibernate.OptimisticLockException;

import se.sperber.cryson.exception.CrysonEntityConflictException;

@Provider
public class OptimisticLockExceptionMapper extends CrysonExceptionMapperBase<OptimisticLockException> {

  @Override
  protected Response toResponseInternal(OptimisticLockException e) {
    return translateCrysonException(new CrysonEntityConflictException("Optimistic locking failed", e));
  }

}
