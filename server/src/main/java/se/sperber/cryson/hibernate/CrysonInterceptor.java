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

package se.sperber.cryson.hibernate;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Component
public class CrysonInterceptor extends EmptyInterceptor {

  @Autowired
  private DefaultListableBeanFactory defaultListableBeanFactory;
  
  Set<Interceptor> interceptors;
  
  @PostConstruct
  public void findInterceptors() {
    interceptors = new HashSet<Interceptor>(defaultListableBeanFactory.getBeansOfType(Interceptor.class).values());
    interceptors.remove(this);
  }

  @Override
  public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
    boolean result = false;
    for(Interceptor interceptor : interceptors) {
      result = interceptor.onFlushDirty(entity, id, currentState, previousState, propertyNames, types) || result;
    }
    return result;
  }

  @Override
  public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
    boolean result = false;
    for(Interceptor interceptor : interceptors) {
      result = interceptor.onSave(entity, id, state, propertyNames, types) || result;
    }
    return result;
  }

}
