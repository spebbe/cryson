package se.sperber.cryson.examples.advancedcrysondiary.interceptor;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.stereotype.Component;
import se.sperber.cryson.examples.advancedcrysondiary.model.BaseEntity;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

@Component
public class TimestampsInterceptor extends EmptyInterceptor {

  @Override
  public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
    if (entity instanceof BaseEntity) {
      setValue(currentState, propertyNames, "updatedAt", new Date());
      return true;
    }
    return false;
  }

  @Override
  public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
    if (entity instanceof BaseEntity) {
      setValue(state, propertyNames, "createdAt", new Date());
      setValue(state, propertyNames, "updatedAt", new Date());
      return true;
    }
    return false;
  }

  private void setValue(Object[] state, String[] propertyNames, String propertyToSet, Object value) {
    int index = Arrays.asList(propertyNames).indexOf(propertyToSet);
    if (index >= 0) {
      state[index] = value;
    }
  }

}