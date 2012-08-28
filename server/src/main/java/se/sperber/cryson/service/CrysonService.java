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

package se.sperber.cryson.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import se.sperber.cryson.listener.ListenerNotificationBatch;
import se.sperber.cryson.repository.CrysonRepository;
import se.sperber.cryson.serialization.CrysonSerializer;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sperber.cryson.serialization.ReflectionHelper;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

@Transactional
@Service
public class CrysonService {

  @Autowired
  private CrysonRepository crysonRepository;

  @Autowired
  private CrysonSerializer crysonSerializer;

  @Autowired
  private ReflectionHelper reflectionHelper;
  
  @Autowired
  private SessionFactory sessionFactory;

  @Value("${cryson.model.package}")
  private String modelsPackage;
  
  private Map<String, Class<?>> entityClassesBySimpleName;

  private Map<Class<?>, Integer> classInsertionOrder;

  private String cachedDefinitions;
  
  @PostConstruct
  public void findEntityClasses() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
    Set<BeanDefinition> components = provider.findCandidateComponents(modelsPackage);
    Map<String, Class<?>> result = new HashMap<String, Class<?>>();
    for (BeanDefinition component : components)
    {
      try {
        Class klazz = Class.forName(component.getBeanClassName());
        result.put(klazz.getSimpleName(), klazz);
      } catch (ClassNotFoundException e) {}
    }
    entityClassesBySimpleName = result;

    classInsertionOrder = calculateClassInsertionOrder(entityClassesBySimpleName.values());
  }

  public Response getEntityDefinition(String entityName) throws Exception {
    return Response.ok().entity(crysonSerializer.serializeWithoutAugmentation(getEntityDefinitionMap(entityName))).build();
  }

  public Response getEntityDefinitions() throws Exception {
    if (cachedDefinitions == null) {
      Map<String, Map<String, String>> entityDefinitions = new HashMap<String, Map<String, String>>();
      for(String entityClassName : entityClassesBySimpleName.keySet()) {
        entityDefinitions.put(entityClassName, getEntityDefinitionMap(entityClassName));
      }
      cachedDefinitions = crysonSerializer.serializeWithoutAugmentation(entityDefinitions);
    }
    return Response.ok().entity(cachedDefinitions).build();
  }

  private Map<String, String> getEntityDefinitionMap(String entityName) throws ClassNotFoundException {
    Map<String, String> entityDefinition = new HashMap<String, String>();

    Class klazz = entityClass(entityName);
    Set<Field> fields = reflectionHelper.getAllDeclaredFields(klazz);

    for(Field field : fields) {
      String fieldClassName = field.getType().getSimpleName();
      if (field.isAnnotationPresent(Type.class)) {
        fieldClassName = "UserType_" + fieldClassName;
      } else if (field.getGenericType() instanceof ParameterizedType) {
        fieldClassName = ((Class) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0]).getSimpleName();
      }
      entityDefinition.put(field.getName(), fieldClassName);
    }

    entityDefinition.put("crysonEntityClass", "String");

    return entityDefinition;
  }

  public Response getEntityById(String entityName, Long id, Set<String> associationsToFetch) {
    Object entity = crysonRepository.findById(qualifiedEntityClassName(entityName), id, associationsToFetch);
    if (entity != null) {
      return Response.ok(crysonSerializer.serialize(entity, associationsToFetch)).build();
    } else {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  public Response getEntitiesByIds(String entityName, List<Long> ids, Set<String> associationsToFetch) {
    List<Object> entities = crysonRepository.findByIds(qualifiedEntityClassName(entityName), ids, associationsToFetch);
    return Response.ok(crysonSerializer.serialize(entities, associationsToFetch)).build();
  }

  public Response getEntitiesByExample(String entityName, String exampleJson, Set<String> associationsToFetch) throws Exception {
    Class entityClass = entityClass(entityName);
    Object exampleEntity = crysonSerializer.deserialize(exampleJson, entityClass, null);
    List<Object> entities = crysonRepository.findByExample(qualifiedEntityClassName(entityName), exampleEntity, associationsToFetch);
    return Response.ok(crysonSerializer.serialize(entities, associationsToFetch)).build();
  }

  public Response getAllEntities(String entityName, Set<String> associationsToFetch) {
    List<Object> entities = crysonRepository.findAll(qualifiedEntityClassName(entityName), associationsToFetch);
    return Response.ok(crysonSerializer.serialize(entities, associationsToFetch)).build();
  }

  public Response createEntity(String entityName, String json, ListenerNotificationBatch listenerNotificationBatch) throws Exception {
    Class entityClass = entityClass(entityName);
    Object entity = crysonSerializer.deserialize(json, entityClass, null);
    crysonRepository.persist(entity);
    crysonRepository.refresh(entity);
    listenerNotificationBatch.entityCreated(entity);
    return Response.ok(crysonSerializer.serialize(entity)).build();
  }

  public Response commit(String json, ListenerNotificationBatch listenerNotificationBatch) throws Exception {
    JsonElement committedEntities = crysonSerializer.parse(json);

    List<Object> refreshedPersistedEntities = new ArrayList<Object>();

    Map<Long, Long> replacedTemporaryIds = new HashMap<Long, Long>();
    JsonArray persistedEntities = committedEntities.getAsJsonObject().get("persistedEntities").getAsJsonArray();
    Collection<JsonElement> sortedPersistedEntities = topologicallySortPersistedEntities(persistedEntities);
    for(JsonElement persistedEntityElement : sortedPersistedEntities) {
      Object entity = crysonSerializer.deserialize(persistedEntityElement, entityClass(persistedEntityElement), replacedTemporaryIds);
      Long temporaryId = crysonSerializer.getPrimaryKey(entity);
      if (temporaryId < 0) {
        crysonSerializer.setPrimaryKey(entity, null);
      }
      crysonRepository.persist(entity);
      refreshedPersistedEntities.add(entity);
      Long replacementId = crysonSerializer.getPrimaryKey(entity);
      replacedTemporaryIds.put(temporaryId, replacementId);
    }

    JsonArray deletedEntities = committedEntities.getAsJsonObject().get("deletedEntities").getAsJsonArray();
    for(JsonElement deletedEntityElement : deletedEntities) {
      Object entity = crysonSerializer.deserialize(deletedEntityElement, entityClass(deletedEntityElement), replacedTemporaryIds);
      crysonRepository.delete(entity);
      listenerNotificationBatch.entityDeleted(entity);
    }

    JsonArray updatedEntities = committedEntities.getAsJsonObject().get("updatedEntities").getAsJsonArray();

    for(JsonElement updatedEntityElement : updatedEntities) {
      Object entity = crysonSerializer.deserialize(updatedEntityElement, entityClass(updatedEntityElement), replacedTemporaryIds);
      crysonRepository.update(entity);
      listenerNotificationBatch.entityUpdated(entity);
    }

    for(Object refreshedPersistedEntity : refreshedPersistedEntities) {
      crysonRepository.refresh(refreshedPersistedEntity);
      listenerNotificationBatch.entityCreated(refreshedPersistedEntity);
    }

    JsonObject responseJsonObject = new JsonObject();
    responseJsonObject.add("replacedTemporaryIds", crysonSerializer.serializeToTreeWithoutAugmentation(replacedTemporaryIds));
    responseJsonObject.add("persistedEntities", crysonSerializer.serializeToTree(refreshedPersistedEntities, Collections.<String>emptySet()));
    return Response.ok(crysonSerializer.serializeTree(responseJsonObject)).build();
  }

  private Collection<JsonElement> topologicallySortPersistedEntities(JsonArray persistedEntities) {
    List<JsonElement> result = new ArrayList<JsonElement>(persistedEntities.size());

    for(JsonElement persistedEntity : persistedEntities) {
      result.add(persistedEntity);
    }

    Collections.sort(result, new Comparator<JsonElement>() {
      public int compare(JsonElement a, JsonElement b) {
        try {
          int aInsertionOrder = classInsertionOrder.get(entityClass(a));
          int bInsertionOrder = classInsertionOrder.get(entityClass(b));
          return ((aInsertionOrder < bInsertionOrder) ? -1 : ((aInsertionOrder > bInsertionOrder) ? 1 : 0));
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("Failed to find class for entity", e);
        }
      }
    });

    return result;
  }

  private Map<Class<?>, Integer> calculateClassInsertionOrder(Collection<Class<?>> classes) {
    Map<Class<?>, List<Class<?>>> classOwnerships = new HashMap<Class<?>, List<Class<?>>>();
    Set<Class<?>> nonOwningClasses = new HashSet<Class<?>>();
    for(Class<?> klazz : classes) {
      List<Class<?>> ownerships = new ArrayList<Class<?>>(reflectionHelper.ownedAssociationFields(klazz).values());
      classOwnerships.put(klazz, ownerships);
      if (ownerships.size() == 0) {
        nonOwningClasses.add(klazz);
      }
    }

    Map<Class<?>, List<Class<?>>> classOwners = new HashMap<Class<?>, List<Class<?>>>();
    for(Class<?> klazz : classes) {
      List<Class<?>> owners = new ArrayList<Class<?>>();
      for(Class<?> otherKlazz : classes) {
        if (classOwnerships.get(otherKlazz).contains(klazz)) {
          owners.add(otherKlazz);
        }
      }
      classOwners.put(klazz, owners);
    }

    List<Class<?>> result = new ArrayList<Class<?>>();
    Set<Class<?>> visitedClasses = new HashSet<Class<?>>();
    for(Class<?> nonOwningClass : nonOwningClasses) {
      topologicalSort(nonOwningClass, classOwners, result, visitedClasses);
    }

    Collections.reverse(result);

    Map<Class<?>, Integer> classInsertionOrder = new HashMap<Class<?>, Integer>();

    for(int ix = 0;ix < result.size();ix++) {
      classInsertionOrder.put(result.get(ix), ix);
    }

    return classInsertionOrder;
  }

  private void topologicalSort(Class<?> klazz, Map<Class<?>, List<Class<?>>> classOwners, List<Class<?>> result, Set<Class<?>> visitedClasses) {
    if (!visitedClasses.contains(klazz)) {
      visitedClasses.add(klazz);
      for(Class<?> owningClass : classOwners.get(klazz)) {
        topologicalSort(owningClass, classOwners, result, visitedClasses);
      }
      result.add(klazz);
    }
  }

  public Response commitEntity(Object entity, ListenerNotificationBatch listenerNotificationBatch) {
    crysonRepository.update(entity);
    listenerNotificationBatch.entityUpdated(entity);
    return Response.ok().build();
  }

  private String qualifiedEntityClassName(String entityName) {
    Class<?> klazz = entityClassesBySimpleName.get(entityName);
    return klazz.getName();
  }

  private Class entityClass(String entityName) throws ClassNotFoundException {
    return entityClassesBySimpleName.get(entityName);
  }

  private Class entityClass(JsonElement entityElement) throws ClassNotFoundException {
    return entityClass(entityElement.getAsJsonObject().get("crysonEntityClass").getAsString());
  }

}
