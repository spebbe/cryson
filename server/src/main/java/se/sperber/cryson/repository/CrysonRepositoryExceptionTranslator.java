/*
  Cryson
  
  Copyright 2011-2012 Björn Sperber (cryson@sperber.se)
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
  http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package se.sperber.cryson.repository;

import se.sperber.cryson.exception.CrysonEntityConflictException;
import se.sperber.cryson.exception.CrysonException;
import se.sperber.cryson.exception.CrysonValidationFailedException;
import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class CrysonRepositoryExceptionTranslator {

  @AfterThrowing(pointcut="execution(public * se.sperber.cryson.repository.CrysonRepository.*(..))",
                 throwing="t")
  public void translateHibernateExceptions(Throwable t) throws Throwable {
    Logger.getLogger(CrysonRepositoryExceptionTranslator.class).error("Translating exception", t);
    if (t instanceof CrysonValidationFailedException) {
      throw t;
    } else if (t instanceof OptimisticLockException || t instanceof HibernateOptimisticLockingFailureException || t instanceof StaleObjectStateException) {
      throw new CrysonEntityConflictException("Optimistic locking failed", t);
    } else {
      throw new CrysonException("Unclassified error: " + t.getMessage(), t);
    }
  }

}