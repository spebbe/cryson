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

package se.sperber.cryson.serialization;

import com.google.common.base.Strings;
import com.google.gson.FieldAttributes;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.stereotype.Component;
import se.sperber.cryson.annotation.VirtualAttribute;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ReflectionHelper {
  
  private ConcurrentMap<Field, Boolean> lazyFieldCache = new ConcurrentHashMap<Field, Boolean>();
  private ConcurrentMap<Class, List<Field>> versionFieldCache = new ConcurrentHashMap<Class, List<Field>>();
  private ConcurrentMap<Class, List<Field>> oneToOneFieldCache = new ConcurrentHashMap<Class, List<Field>>();
  private ConcurrentMap<Class, Set<Method>> virtualAttributeGetterCache = new ConcurrentHashMap<Class, Set<Method>>();
  private ConcurrentMap<Class, Method> primaryKeyGetterCache = new ConcurrentHashMap<Class, Method>();
  private ConcurrentMap<Class, Method> primaryKeySetterCache = new ConcurrentHashMap<Class, Method>();

  public boolean isLazyField(Field field) {
    if (lazyFieldCache.containsKey(field)) {
      return lazyFieldCache.get(field);
    }
    
    FetchType fetchType = null;
    if (field.isAnnotationPresent(ElementCollection.class)) {
      fetchType = field.getAnnotation(ElementCollection.class).fetch();
    } else if (field.isAnnotationPresent(ManyToMany.class)) {
      fetchType = field.getAnnotation(ManyToMany.class).fetch();
    } else if (field.isAnnotationPresent(ManyToOne.class)) {
      fetchType = field.getAnnotation(ManyToOne.class).fetch();
    } else if (field.isAnnotationPresent(OneToMany.class)) {
      fetchType = field.getAnnotation(OneToMany.class).fetch();
    } else if (field.isAnnotationPresent(OneToOne.class)) {
      fetchType = field.getAnnotation(OneToOne.class).fetch();
    }
    boolean result = fetchType != null && fetchType == FetchType.LAZY;
    
    lazyFieldCache.put(field, result);
    return result;
  }

  public Map<Field, Class<?>> ownedAssociationFields(Class<?> klazz) {
    Map<Field, Class<?>> result = new HashMap<Field, Class<?>>();

    for(Field field : getAllDeclaredFields(klazz)) {
      Class<?> targetClass = getAssociationTargetClass(field);
      if (targetClass != klazz) {
        if (field.isAnnotationPresent(ElementCollection.class)) {
          result.put(field, targetClass);
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
          if (Strings.isNullOrEmpty(field.getAnnotation(ManyToMany.class).mappedBy())) {
            result.put(field, targetClass);
          }
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
          if (!targetClassOwnsClass(targetClass, klazz)) {
            result.put(field, targetClass);
          }
        } else if (field.isAnnotationPresent(OneToMany.class)) {
          if (Strings.isNullOrEmpty(field.getAnnotation(OneToMany.class).mappedBy())) {
            result.put(field, targetClass);
          }
        } else if (field.isAnnotationPresent(OneToOne.class)) {
          if (Strings.isNullOrEmpty(field.getAnnotation(OneToOne.class).mappedBy())) {
            result.put(field, targetClass);
          }
        }
      }
    }

    return result;
  }

  private boolean targetClassOwnsClass(Class<?> targetClass, Class<?> klazz) {
    Collection<Class<?>> ownedAssociationFields = ownedAssociationFields(targetClass).values();
    Class currentKlazz = klazz;
    while(currentKlazz != Object.class) {
      if (ownedAssociationFields.contains(currentKlazz)) {
        return true;
      }
      currentKlazz = currentKlazz.getSuperclass();
    }
    return false;
  }

  private Class<?> getAssociationTargetClass(Field field) {
    if ((field.getGenericType() instanceof ParameterizedType)) {
      return ((Class) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0]);
    } else {
      return field.getType();
    }
  }

  public boolean isLazyFieldAttribute(FieldAttributes f) {
    try {
      Field fieldField = FieldAttributes.class.getDeclaredField("field");
      fieldField.setAccessible(true);
      return isLazyField((Field)fieldField.get(f));
    } catch (NoSuchFieldException e) {
      throw new IllegalStateException("Couldn't find 'field' of FieldAttributes. Shouldn't happen...", e);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Couldn't read 'field' of FieldAttributes. Shouldn't happen...", e);
    }
  }

  public Set<Field> getAllDeclaredFields(Class klazz) {
    Class currentKlazz = klazz;
    Set<Field> fields = new HashSet<Field>();
    while(currentKlazz != Object.class) {
      Field currentFields[] = currentKlazz.getDeclaredFields();
      for(Field field : currentFields) {
        fields.add(field);
      }
      currentKlazz = currentKlazz.getSuperclass();
    }
    return fields;
  }

  public Set<Method> getAllDeclaredVirtualAttributeGetters(Class klazz) {
    if (virtualAttributeGetterCache.containsKey(klazz)) {
      return virtualAttributeGetterCache.get(klazz);
    }
    Class currentKlazz = klazz;
    Set<Method> methods = new HashSet<Method>();
    while(currentKlazz != Object.class) {
      Method currentMethods[] = currentKlazz.getMethods();
      for(Method method : currentMethods) {
        if (method.isAnnotationPresent(VirtualAttribute.class) && method.getName().startsWith("get")) {
          methods.add(method);
        }
      }
      currentKlazz = currentKlazz.getSuperclass();
    }
    virtualAttributeGetterCache.put(klazz, methods);
    return methods;
  }

  public String getAttributeNameFromGetterName(String methodName) {
    String capitalizedAttributeName = methodName.replaceFirst("get", "");
    return capitalizedAttributeName.substring(0, 1).toLowerCase() + capitalizedAttributeName.substring(1);
  }

  public List<Field> getOneToOneFields(Object entity) {
    if (oneToOneFieldCache.containsKey(entity.getClass())) {
      return oneToOneFieldCache.get(entity.getClass());
    }
    List<Field> oneToOneFields = new ArrayList<Field>();
    for(Field field : getAllDeclaredFields(entity.getClass())) {
      if (field.isAnnotationPresent(OneToOne.class)) {
        field.setAccessible(true);
        oneToOneFields.add(field);
      }
    }
    oneToOneFieldCache.put(entity.getClass(), oneToOneFields);
    return oneToOneFields;
  }

  public Long getVersion(Object entity) {
    try {
      Field versionField = null;
      if (versionFieldCache.containsKey(entity.getClass())) {
        List<Field> versionFields = versionFieldCache.get(entity.getClass());
        versionField = versionFields.isEmpty() ? null : versionFields.get(0);
      } else {
        for (Field field : getAllDeclaredFields(entity.getClass())) {
          if (field.isAnnotationPresent(Version.class)) {
            field.setAccessible(true);
            versionFieldCache.put(entity.getClass(), Collections.singletonList(field));
            versionField = field;
            break;
          }
        }
        if (versionField == null) {
          versionFieldCache.put(entity.getClass(), Collections.<Field>emptyList());
        }
      }
      if (versionField != null) {
        return (Long)versionField.get(entity);
      } else {
        return null;
      }
    } catch(IllegalAccessException e) {
      return null;
    }
  }

  public String getVersionFieldName(Object entity) {
    List<Field> versionFields = versionFieldCache.get(entity.getClass());
    if (versionFields.isEmpty()) {
      return null;
    } else {
      return versionFields.get(0).getName();
    }
  }

  public Long getPrimaryKey(Object entity) {
    try {
      if (entity instanceof HibernateProxy) {
        return (Long)((HibernateProxy)entity).getHibernateLazyInitializer().getIdentifier();
      } else {
        try {
          Method method = primaryKeyGetterCache.get(entity.getClass());
          if (method == null) {
            method = entity.getClass().getMethod("getId");
            primaryKeyGetterCache.put(entity.getClass(), method);
          }
          return (Long)method.invoke(entity);
        } catch(NoSuchMethodException e) {
          Field field = getField(entity, "id");
          return (Long)field.get(entity);
        }
      }
    } catch(Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public void setPrimaryKey(Object entity, Long primaryKey) {
    try {
      try {
        Method method = primaryKeySetterCache.get(entity.getClass());
        if (method == null) {
          method = entity.getClass().getMethod("setId", Long.class);
          primaryKeySetterCache.put(entity.getClass(), method);
        }
        method.invoke(entity, primaryKey);
      } catch(NoSuchMethodException e) {
        Field field = getField(entity, "id");
        field.set(entity, primaryKey);
      }
    } catch(Throwable t) {
      throw new RuntimeException(t);
    }
  }

  Field getField(Object object, String fieldName) {
    Field result = null;
    Class klazz = object.getClass();
    while(result == null && klazz != Object.class) {
      try {
        result = klazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {}
      klazz = klazz.getSuperclass();
    }
    if (result != null) {
      result.setAccessible(true);
    }
    return result;
  }

}
