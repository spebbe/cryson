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

package se.sperber.cryson.exception;

import javax.validation.ConstraintViolation;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class CrysonValidationFailedException extends CrysonException {

  private Set<ConstraintViolation<Object>> constraintViolations;

  public CrysonValidationFailedException(String message, Set<ConstraintViolation<Object>> constraintViolations) {
    super(message, null);
    this.constraintViolations = constraintViolations;
  }

  @Override
  public int getStatusCode() {
    return Response.Status.FORBIDDEN.getStatusCode();
  }

  @Override
  public Map<String, Serializable> getSerializableMessage() {
    Map<String, Serializable> result = super.getSerializableMessage();
    ArrayList<ValidationFailure> validationFailures = new ArrayList<ValidationFailure>();
    for(ConstraintViolation<Object> constraintViolation : constraintViolations) {
      validationFailures.add(new ValidationFailure(constraintViolation.getRootBeanClass().getSimpleName(),
              getPrimaryKey(constraintViolation.getRootBean()),
              constraintViolation.getPropertyPath().toString(), constraintViolation.getMessage()));
    }
    result.put("validationFailures", validationFailures);
    return result;
  }

  private Long getPrimaryKey(Object entity) {
    try {
      Method method = entity.getClass().getMethod("getId");
      return (Long)method.invoke(entity);
    } catch(Throwable t) {
      try {
        Field field = null;
        Class klazz = entity.getClass();
        while(field == null && klazz != Object.class) {
          try {
            field = klazz.getDeclaredField("id");
          } catch (NoSuchFieldException e) {}
          klazz = klazz.getSuperclass();
        }
        if (field != null) {
          field.setAccessible(true);
        }
        return (Long)field.get(entity);
      } catch(Throwable t2) {
        return null;
      }
    }
  }

  private static class ValidationFailure implements Serializable {

    private String entityClass;
    private Long entityId;
    private String keyPath;
    private String message;

    private ValidationFailure(String entityClass, Long entityId, String keyPath, String message) {
      this.entityClass = entityClass;
      this.entityId = entityId;
      this.keyPath = keyPath;
      this.message = message;
    }

  }

}
