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

import com.google.gson.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

@Component
public class CrysonSerializer {

  private Gson gson;
  
  private Gson gsonAllInclusive;

  private JsonParser jsonParser;

  private final Map<Class<?>, Set<Field>> lazyFieldsCache = new ConcurrentHashMap<Class<?>, Set<Field>>();

  private final Map<Class<?>, Set<Field>> userTypeFieldsCache = new ConcurrentHashMap<Class<?>, Set<Field>>();

  @Autowired
  private ReflectionHelper reflectionHelper;

  @Autowired
  private LazyAssociationExclusionStrategy lazyAssociationExclusionStrategy;

  @Autowired
  private UserTypeExclusionStrategy userTypeExclusionStrategy;

  @PostConstruct
  public void setupGson() {
    jsonParser = new JsonParser();
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.serializeNulls();
    gsonBuilder.setDateFormat("yyyy-MM-dd HH:mm:ss Z");

    HibernateProxyTypeAdapter hibernateProxyTypeAdapter = new HibernateProxyTypeAdapter();
    gsonBuilder.registerTypeHierarchyAdapter(HibernateProxy.class, hibernateProxyTypeAdapter);

    gsonAllInclusive = gsonBuilder.create();

    gsonBuilder.setExclusionStrategies(lazyAssociationExclusionStrategy, userTypeExclusionStrategy);
    gson = gsonBuilder.create();

    hibernateProxyTypeAdapter.setGson(gson);
  }

  public JsonElement serializeToTree(Object object, Set<String> associationsToInclude) {
    JsonElement jsonElement = gson.toJsonTree(object);
    augmentJsonElement(object, jsonElement, associationsToInclude);
    return jsonElement;
  }

  public String serializeTree(JsonElement jsonElement) {
    return gson.toJson(jsonElement);
  }

  public String serialize(Object object, Set<String> associationsToInclude) {
    return serializeTree(serializeToTree(object, associationsToInclude));
  }

  public String serialize(Object object) {
    return serialize(object, Collections.<String>emptySet());
  }

  public String serializeWithoutAugmentation(Object object) {
    return gson.toJson(object);
  }

  public JsonElement serializeToTreeWithoutAugmentation(Object object) {
    return gson.toJsonTree(object);
  }

  public <T> T deserialize(String json, Class<T> classOfT, Map<Long, Long> replacedTemporaryIds) {
    JsonElement jsonElement = jsonParser.parse(json);
    return augmentEntity(gson.fromJson(json, classOfT), jsonElement, replacedTemporaryIds);
  }

  public <T> T deserialize(JsonElement jsonElement, Class<T> classOfT, Map<Long, Long> replacedTemporaryIds) {
    return augmentEntity(gson.fromJson(jsonElement, classOfT), jsonElement, replacedTemporaryIds);
  }

  private void augmentJsonElement(Object rawObject, JsonElement jsonElement, Set<String> associationsToInclude) {
    Object object = HibernateProxyTypeAdapter.initializeAndUnproxy(rawObject);
    try {
      if (object instanceof Collection) {
        int ix = 0;
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        for(Object subObject : (Collection)object) {
          JsonElement subJsonElement = jsonArray.get(ix++);
          augmentJsonElement(subObject, subJsonElement, associationsToInclude);
        }
      } else {
        jsonElement.getAsJsonObject().add("crysonEntityClass", new JsonPrimitive(object.getClass().getSimpleName()));

        for(Map.Entry<String, JsonElement> member : jsonElement.getAsJsonObject().entrySet()) {
          if (member.getValue().isJsonObject() || member.getValue().isJsonArray()) {
            Field field = getField(object, member.getKey());
            field.setAccessible(true);
            augmentJsonElement(field.get(object), member.getValue(), subAssociationsToInclude(associationsToInclude, member.getKey()));
          }
        }
        
        Set<Field> userTypeFields = getUserTypeFields(object.getClass());
        for (Field field : userTypeFields) {
          Object fieldValue = field.get(object);
          if (fieldValue != null && Map.class.isAssignableFrom(field.getType())) {
            JsonElement jsonFieldValue = gsonAllInclusive.toJsonTree(fieldValue);
            jsonElement.getAsJsonObject().add(field.getName() + "_cryson_usertype", jsonFieldValue);
          }
        }

        Set<Method> transientGetters = reflectionHelper.getAllDeclaredVirtualAttributeGetters(object.getClass());
        for (Method method : transientGetters) {
          Object methodValue = method.invoke(object);
          JsonElement jsonFieldValue = gsonAllInclusive.toJsonTree(methodValue);
          jsonElement.getAsJsonObject().add(reflectionHelper.getAttributeNameFromGetterName(method.getName()), jsonFieldValue);
        }

        Set<Field> fields = getLazyFields(object.getClass());
        for(Field field : fields) {
          Object fieldValue = field.get(object);
          if (fieldValue != null && associationsToInclude.contains(field.getName())) {
            JsonElement fieldValueJsonElement = gson.toJsonTree(fieldValue);
            augmentJsonElement(fieldValue, fieldValueJsonElement, subAssociationsToInclude(associationsToInclude, field.getName()));
            jsonElement.getAsJsonObject().add(field.getName(), fieldValueJsonElement);
          } else if (fieldValue != null) {
            if (fieldValue instanceof Collection) {
              JsonArray primaryKeyArray = new JsonArray();
              for(Object subElement : (Collection)fieldValue) {
                primaryKeyArray.add(new JsonPrimitive(getPrimaryKey(subElement)));
              }
              jsonElement.getAsJsonObject().add(field.getName() + "_cryson_ids", primaryKeyArray);
            } else {
              jsonElement.getAsJsonObject().addProperty(field.getName() + "_cryson_id", getPrimaryKey(fieldValue));
            }
          } else {
            jsonElement.getAsJsonObject().add(field.getName() + "_cryson_id", JsonNull.INSTANCE);
          }
        }
      }
    } catch(Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private Set<String> subAssociationsToInclude(Set<String> associationsToInclude, String association) {
    if (associationsToInclude.size() == 0) {
      return associationsToInclude;
    }

    Set<String> subAssociations = new HashSet<String>();
    String subAssociationPrefix = association + ".";
    for(String associationToInclude : associationsToInclude) {
      if (associationToInclude.startsWith(subAssociationPrefix)) {
        subAssociations.add(associationToInclude.replaceFirst(Matcher.quoteReplacement(subAssociationPrefix), ""));
      }
    }
    return subAssociations;
  }

  public Long getPrimaryKey(Object entity) {
    try {
      if (entity instanceof HibernateProxy) {
        return (Long)((HibernateProxy)entity).getHibernateLazyInitializer().getIdentifier();
      } else {
        try {
          Method method = entity.getClass().getMethod("getId");
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
        Method method = entity.getClass().getMethod("setId", Long.class);
        method.invoke(entity, primaryKey);
      } catch(NoSuchMethodException e) {
        Field field = getField(entity, "id");
        field.set(entity, primaryKey);
      }
    } catch(Throwable t) {
      throw new RuntimeException(t);
    }
  }
  
  private Field getField(Object object, String fieldName) {
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

  private <T> T augmentEntity(T object, JsonElement jsonElement, Map<Long, Long> replacedTemporaryIds) {
    try {
      Set<Map.Entry<String,JsonElement>> attributes = jsonElement.getAsJsonObject().entrySet();
      for(Map.Entry<String,JsonElement> attribute : attributes) {
        if (attribute.getKey().endsWith("_cryson_id")) {
          String attributeName = attribute.getKey().split("_cryson_id")[0];
          Field field = getField(object, attributeName);
          if (field != null) {
            if (attribute.getValue() != JsonNull.INSTANCE) {
              Object placeHolderObject = field.getType().newInstance();
              setPrimaryKey(placeHolderObject, primaryKeyForReplacementObject(attribute.getValue().getAsLong(), replacedTemporaryIds));
              field.set(object, placeHolderObject);
            }
          }
        } else if (attribute.getKey().endsWith("_cryson_ids")) {
          String attributeName = attribute.getKey().split("_cryson_ids")[0];
          Field field = getField(object, attributeName);
          if (field != null) {
            Iterator<JsonElement> attributeIterator = attribute.getValue().getAsJsonArray().iterator();
            Collection<Object> placeHolderObjects = emptyCollectionForField(field);
            while(attributeIterator.hasNext()) {
              JsonElement attributeValue = attributeIterator.next();
              Object placeHolderObject = ((Class)((ParameterizedType)(field.getGenericType())).getActualTypeArguments()[0]).newInstance();
              setPrimaryKey(placeHolderObject, primaryKeyForReplacementObject(attributeValue.getAsLong(), replacedTemporaryIds));
              placeHolderObjects.add(placeHolderObject);
            }
            field.set(object, placeHolderObjects);
          }
        } else if (attribute.getKey().endsWith("_cryson_usertype")) {
          String attributeName = attribute.getKey().split("_cryson_usertype")[0];
          Field field = getField(object, attributeName);
          if (field != null && Map.class.isAssignableFrom(field.getType())) {
            Map<Object, Object> placeHolderObjects = new HashMap<Object, Object>();
            for (Map.Entry<String, JsonElement> valueEntry : attribute.getValue().getAsJsonObject().entrySet()) {
              String placeHolderKeyObject = valueEntry.getKey();
              Object placeHolderValueObject = gson.fromJson(valueEntry.getValue(), Object.class); // Support string->string maps only
              placeHolderObjects.put(placeHolderKeyObject, placeHolderValueObject);
            }
            field.set(object, placeHolderObjects);
          }
        }
      }
      return object;
    } catch(Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private Long primaryKeyForReplacementObject(Long candidatePrimaryKey, Map<Long, Long> replacedTemporaryIds) {
    if (replacedTemporaryIds != null) {
      Long replacedTemporaryId = replacedTemporaryIds.get(candidatePrimaryKey);
      if (replacedTemporaryId != null) {
        return replacedTemporaryId;
      }
    }
    return candidatePrimaryKey;
  }

  private Collection<Object> emptyCollectionForField(Field field) {
    if (field.getType() == Set.class) {
      return new HashSet<Object>();
    } else {
      return new ArrayList<Object>();
    }
  }

  private Set<Field> getLazyFields(Class<?> klazz) {
    Set<Field> annotatedFields = lazyFieldsCache.get(klazz);
    if (annotatedFields == null) {
      annotatedFields = new HashSet<Field>();
      @SuppressWarnings("rawtypes")
      Class currentKlazz = klazz;
      while(currentKlazz != null) {
        Set<Field> declaredFields = reflectionHelper.getAllDeclaredFields(currentKlazz);
        for(Field declaredField : declaredFields) {
          if (reflectionHelper.isLazyField(declaredField)) {
            declaredField.setAccessible(true);
            annotatedFields.add(declaredField);
          }
        }
        currentKlazz = currentKlazz.getSuperclass();
      }
      lazyFieldsCache.put(klazz, annotatedFields);
    }
    return annotatedFields;
  }

  // Todo: refactor this copy-pasted method
  private Set<Field> getUserTypeFields(Class<?> klazz) {
    Set<Field> annotatedFields = userTypeFieldsCache.get(klazz);
    if (annotatedFields == null) {
      annotatedFields = new HashSet<Field>();
      @SuppressWarnings("rawtypes")
      Class currentKlazz = klazz;
      while(currentKlazz != null) {
        Set<Field> declaredFields = reflectionHelper.getAllDeclaredFields(currentKlazz);
        for(Field declaredField : declaredFields) {
          if (declaredField.isAnnotationPresent(org.hibernate.annotations.Type.class)) {
            declaredField.setAccessible(true);
            annotatedFields.add(declaredField);
          }
        }
        currentKlazz = currentKlazz.getSuperclass();
      }
      userTypeFieldsCache.put(klazz, annotatedFields);
    }
    return annotatedFields;
  }

  public JsonElement parse(String json) {
    return jsonParser.parse(json);
  }

  void setReflectionHelper(ReflectionHelper reflectionHelper) {
    this.reflectionHelper = reflectionHelper;
  }

  void setLazyAssociationExclusionStrategy(LazyAssociationExclusionStrategy lazyAssociationExclusionStrategy) {
    this.lazyAssociationExclusionStrategy = lazyAssociationExclusionStrategy;
  }

  void setUserTypeExclusionStrategy(UserTypeExclusionStrategy userTypeExclusionStrategy) {
    this.userTypeExclusionStrategy = userTypeExclusionStrategy;
  }

}
