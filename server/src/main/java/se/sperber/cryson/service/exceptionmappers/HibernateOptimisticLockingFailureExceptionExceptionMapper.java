package se.sperber.cryson.service.exceptionmappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;

import se.sperber.cryson.exception.CrysonEntityConflictException;

@Provider
public class HibernateOptimisticLockingFailureExceptionExceptionMapper extends CrysonExceptionMapperBase<HibernateOptimisticLockingFailureException> {

  @Override
  protected Response toResponseInternal(HibernateOptimisticLockingFailureException e) {
    return translateCrysonException(new CrysonEntityConflictException("Optimistic locking failed", e));
  }
}
