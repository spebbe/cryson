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

import com.google.common.collect.Sets;
import com.google.gson.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import se.sperber.cryson.security.Restrictable;

import javax.annotation.PostConstruct;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;

@Component
public class CrysonSerializer {

  private Gson gson;
  
  private Gson gsonAllInclusive;

  private JsonParser jsonParser;

  private final Map<Class<?>, Set<Field>> lazyFieldsCache = new ConcurrentHashMap<Class<?>, Set<Field>>();

  private final Map<Class<?>, Set<Field>> userTypeFieldsCache = new ConcurrentHashMap<Class<?>, Set<Field>>();

  private final Set<String> allowedUnauthorizedAttributeNames = Sets.newHashSet("id", "crysonEntityClass");

  @Autowired
  private ReflectionHelper reflectionHelper;

  @Autowired
  private LazyAssociationExclusionStrategy lazyAssociationExclusionStrategy;

  @Autowired
  private UserTypeExclusionStrategy userTypeExclusionStrategy;

  @Autowired
  private CrysonExcludeExclusionStrategy crysonExcludeExclusionStrategy;

  @PostConstruct
  public void setupGson() {
    jsonParser = new JsonParser();
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.serializeNulls();
    gsonBuilder.setDateFormat("yyyy-MM-dd HH:mm:ss Z");

    HibernateProxyTypeAdapter hibernateProxyTypeAdapter = new HibernateProxyTypeAdapter();
    gsonBuilder.registerTypeHierarchyAdapter(HibernateProxy.class, hibernateProxyTypeAdapter);

    gsonAllInclusive = gsonBuilder.create();

    gsonBuilder.setExclusionStrategies(lazyAssociationExclusionStrategy, userTypeExclusionStrategy, crysonExcludeExclusionStrategy);
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

  public String serialize(Collection<Object> objects, Set<String> associationsToInclude,
                          Set<String> associationsToExclude, Map<String, Map<Long, Set<Long>>> lazyFields) {
    JsonArray jsonArray = new JsonArray();
    objects.stream()
      .map(o -> serializeToTree(o, associationsToInclude, associationsToExclude, lazyFields))
      .forEach(jsonArray::add);
    return serializeTree(jsonArray);
  }

  private JsonElement serializeToTree(Object object,
                                      Set<String> associationsToInclude,
                                      Set<String> associationsToExclude,
                                      Map<String, Map<Long, Set<Long>>> lazyFields) {
    JsonElement jsonElement = gson.toJsonTree(object);
    augmentJsonElement(object, jsonElement, associationsToInclude, associationsToExclude, lazyFields);
    return jsonElement;
  }

  public String serialize(Object object) {
    return serialize(object, Collections.<String>emptySet());
  }

  public String serializeUnauthorizedEntity(String entityName, Long id) {
    UnauthorizedEntity entity = new UnauthorizedEntity(entityName, id);
    return gson.toJson(entity);
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

  private void augmentJsonElement(Object rawObject, JsonElement jsonElement, Set<String> associationsToInclude){
    augmentJsonElement(rawObject, jsonElement, associationsToInclude, Collections.emptySet(), Collections.emptyMap());
  }

  private void augmentJsonElement(Object rawObject, JsonElement jsonElement, Set<String> associationsToInclude, Set<String> associationsToExclude, Map<String, Map<Long, Set<Long>>> lazyFields) {
    Object object = HibernateProxyTypeAdapter.initializeAndUnproxy(rawObject);
    try {
      if (object instanceof Collection) {
        int ix = 0;
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        for (Object subObject : (Collection) object) {
          JsonElement subJsonElement = jsonArray.get(ix++);
          augmentJsonElement(subObject, subJsonElement, associationsToInclude, associationsToExclude, lazyFields);
        }
      } else {
        jsonElement.getAsJsonObject().add("crysonEntityClass", new JsonPrimitive(object.getClass().getSimpleName()));

        if (object instanceof Restrictable) {
          if (!((Restrictable) object).isReadableBy(SecurityContextHolder.getContext().getAuthentication())) {
            transformJsonObjectToUnauthorizedEntity(jsonElement.getAsJsonObject());
            return;
          }
        }

        for (Map.Entry<String, JsonElement> member : jsonElement.getAsJsonObject().entrySet()) {
          if (member.getValue().isJsonObject() || member.getValue().isJsonArray()) {
            Field field = reflectionHelper.getField(object, member.getKey());
            field.setAccessible(true);
            augmentJsonElement(field.get(object), member.getValue(), subAssociations(associationsToInclude, member.getKey()), subAssociations(associationsToExclude, member.getKey()), Collections.emptyMap());
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
          String name = reflectionHelper.getAttributeNameFromGetterName(method.getName());
          if(associationsToExclude.contains(name))continue;
          Object methodValue = method.invoke(object);
          JsonElement jsonFieldValue = gsonAllInclusive.toJsonTree(methodValue);
          jsonElement.getAsJsonObject().add(name, jsonFieldValue);
        }

        Set<Field> fields = getLazyFields(object.getClass());
        for (Field field : fields) {
          String fieldName = field.getName();
          if(associationsToExclude.contains(fieldName))continue;
          if (associationsToInclude.contains(fieldName)) {
            Object fieldValue = field.get(object);
            if (fieldValue != null) {
              JsonElement fieldValueJsonElement = gson.toJsonTree(fieldValue);
              augmentJsonElement(fieldValue, fieldValueJsonElement, subAssociations(associationsToInclude, fieldName), subAssociations(associationsToExclude, fieldName),Collections.emptyMap());
              jsonElement.getAsJsonObject().add(fieldName, fieldValueJsonElement);
            }
          } else if (lazyFields.containsKey(fieldName)) {
            Set<Long> fieldValue = lazyFields.get(fieldName).get(reflectionHelper.getPrimaryKey(rawObject));
            if (Collection.class.isAssignableFrom(field.getType())) {
              JsonArray primaryKeyArray = new JsonArray();
              if (fieldValue != null) {
                fieldValue.stream().map(JsonPrimitive::new).forEach(primaryKeyArray::add);
              }
              jsonElement.getAsJsonObject().add(fieldName + "_cryson_ids", primaryKeyArray);
            } else {
              if (fieldValue != null) {
                fieldValue.stream().findFirst()
                  .ifPresent(key -> jsonElement.getAsJsonObject().addProperty(fieldName + "_cryson_id", key));
              } else {
                jsonElement.getAsJsonObject().add(fieldName + "_cryson_id", JsonNull.INSTANCE);
              }
            }
          } else {
            Object fieldValue = field.get(object);
            if (fieldValue != null) {
              if (fieldValue instanceof Collection) {
                JsonArray primaryKeyArray = new JsonArray();
                for (Object subElement : (Collection) fieldValue) {
                  primaryKeyArray.add(new JsonPrimitive(reflectionHelper.getPrimaryKey(subElement)));
                }
                jsonElement.getAsJsonObject().add(fieldName + "_cryson_ids", primaryKeyArray);
              } else {
                jsonElement.getAsJsonObject().addProperty(fieldName + "_cryson_id", reflectionHelper.getPrimaryKey(fieldValue));
              }
            } else {
              jsonElement.getAsJsonObject().add(fieldName + "_cryson_id", JsonNull.INSTANCE);
            }
          }
        }
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private void transformJsonObjectToUnauthorizedEntity(JsonObject jsonObject) {
    for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
      if (!allowedUnauthorizedAttributeNames.contains(entry.getKey())) {
        jsonObject.remove(entry.getKey());
      }
    }
    jsonObject.addProperty("crysonUnauthorized", true);
  }

  private Set<String> subAssociations(Set<String> associationsToInclude, String association) {
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

  public String getEntityClassName(Object entity) {
    if (entity instanceof HibernateProxy) {
      return ((HibernateProxy)entity).getHibernateLazyInitializer().getPersistentClass().getName();
    } else {
      return entity.getClass().getName();
    }
  }

  private <T> T augmentEntity(T object, JsonElement jsonElement, Map<Long, Long> replacedTemporaryIds) {
    try {
      Set<Map.Entry<String,JsonElement>> attributes = jsonElement.getAsJsonObject().entrySet();
      for(Map.Entry<String,JsonElement> attribute : attributes) {
        if (attribute.getKey().endsWith("_cryson_id")) {
          String attributeName = attribute.getKey().split("_cryson_id")[0];
          Field field = reflectionHelper.getField(object, attributeName);
          if (field != null) {
            if (attribute.getValue() != JsonNull.INSTANCE) {
              Object placeHolderObject = field.getType().newInstance();
              reflectionHelper.setPrimaryKey(placeHolderObject, primaryKeyForReplacementObject(attribute.getValue().getAsLong(), replacedTemporaryIds));
              field.set(object, placeHolderObject);
            }
          }
        } else if (attribute.getKey().endsWith("_cryson_ids")) {
          String attributeName = attribute.getKey().split("_cryson_ids")[0];
          Field field = reflectionHelper.getField(object, attributeName);
          if (field != null) {
            Iterator<JsonElement> attributeIterator = attribute.getValue().getAsJsonArray().iterator();
            Collection<Object> placeHolderObjects = emptyCollectionForField(field);
            while(attributeIterator.hasNext()) {
              JsonElement attributeValue = attributeIterator.next();
              Object placeHolderObject = ((Class)((ParameterizedType)(field.getGenericType())).getActualTypeArguments()[0]).newInstance();
              reflectionHelper.setPrimaryKey(placeHolderObject, primaryKeyForReplacementObject(attributeValue.getAsLong(), replacedTemporaryIds));
              placeHolderObjects.add(placeHolderObject);
            }
            field.set(object, placeHolderObjects);
          }
        } else if (attribute.getKey().endsWith("_cryson_usertype")) {
          String attributeName = attribute.getKey().split("_cryson_usertype")[0];
          Field field = reflectionHelper.getField(object, attributeName);
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

  public void setCrysonExcludeExclusionStrategy(CrysonExcludeExclusionStrategy crysonExcludeExclusionStrategy) {
    this.crysonExcludeExclusionStrategy = crysonExcludeExclusionStrategy;
  }
}
