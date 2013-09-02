/*
  Cryson
  
  Copyright 2011-2012 Bj��rn Sperber (cryson@sperber.se)
  
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import se.sperber.cryson.listener.CrysonListener;
import se.sperber.cryson.listener.ListenerNotificationBatch;
import se.sperber.cryson.serialization.CrysonSerializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Service
@Path("/")
@Produces("application/json")
@PreAuthorize("isAuthenticated()")
public class CrysonFrontendService {

  @Autowired
  private DefaultListableBeanFactory defaultListableBeanFactory;

  @Autowired
  private CrysonService crysonService;

  @Autowired
  private CrysonSerializer crysonSerializer;

  private Set<CrysonListener> crysonListeners;

  private static final Logger logger = Logger.getLogger(CrysonFrontendService.class);

  @PostConstruct
  public void findListeners() {
    crysonListeners = new HashSet<CrysonListener>(defaultListableBeanFactory.getBeansOfType(CrysonListener.class).values());
  }
  
  @GET
  @Path("definition/{entity_name}")
  public Response getEntityDefinition(@PathParam("entity_name") String entityName) throws Throwable {
      return crysonService.getEntityDefinition(entityName);
  }

  @GET
  @Path("definitions")
  public Response getEntityDefinitions() throws Throwable {
      return crysonService.getEntityDefinitions();
  }

  @GET
  @Path("{entity_name}/{rawIds: [0-9,]+}")
  public Response getEntityById(@PathParam("entity_name") String entityName, @PathParam("rawIds") String rawIds, @QueryParam("fetch") String rawAssociationsToFetch) {
      Set<String> associationsToFetch = splitAssociationsToFetch(rawAssociationsToFetch);

      String[] stringIds = rawIds.split(",");
      if (stringIds.length == 1) {
        return crysonService.getEntityById(entityName, Long.parseLong(stringIds[0]), associationsToFetch);
      } else {
        List<Long> ids = new ArrayList<Long>(stringIds.length);
        for(int ix = 0;ix < stringIds.length;ix++) {
          ids.add(Long.parseLong(stringIds[ix]));
        }
        return crysonService.getEntitiesByIds(entityName, ids, associationsToFetch);
      }
  }

  @GET
  @Path("{entity_name}")
  public Response getEntitiesByExample(@PathParam("entity_name") String entityName, @QueryParam("example") String exampleJson, @QueryParam("fetch") String rawAssociationsToFetch) throws Throwable {
      Set<String> associationsToFetch = splitAssociationsToFetch(rawAssociationsToFetch);
      return crysonService.getEntitiesByExample(entityName, exampleJson, associationsToFetch);
  }

  @GET
  @Path("{entity_name}/all")
  public Response getAllEntities(@PathParam("entity_name") String entityName, @QueryParam("fetch") String rawAssociationsToFetch) {
      Set<String> associationsToFetch = splitAssociationsToFetch(rawAssociationsToFetch);
      return crysonService.getAllEntities(entityName, associationsToFetch);
  }

  @GET
  @Path("namedQuery/{query_name}")
  public Response getEntitiesByNamedQuery(@PathParam("query_name") String queryName, @Context UriInfo uriInfo, @QueryParam("fetch") String rawAssociationsToFetch) {
      Set<String> associationsToFetch = splitAssociationsToFetch(rawAssociationsToFetch);
      MultivaluedMap<String,String> queryParameters = uriInfo.getQueryParameters();
      queryParameters.remove("fetch");
      return crysonService.getEntitiesByNamedQuery(queryName, queryParameters, associationsToFetch);
  }

  @POST
  @Path("namedQuery/{query_name}")
  public Response getEntitiesByNamedQueryPost(@PathParam("query_name") String queryName, @Context UriInfo uriInfo, @QueryParam("fetch") String rawAssociationsToFetch, String json) {
      JsonElement parameters = crysonSerializer.parse(json);
      Set<String> associationsToFetch = splitAssociationsToFetch(rawAssociationsToFetch);
      return crysonService.getEntitiesByNamedQueryJson(queryName, associationsToFetch, parameters);
  }

  @PUT
  @Path("{entity_name}")
  public Response createEntity(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders, @PathParam("entity_name") String entityName, String json) throws Throwable {
      ListenerNotificationBatch listenerNotificationBatch = new ListenerNotificationBatch(uriInfo, httpHeaders);
      Response response = crysonService.createEntity(entityName, json, listenerNotificationBatch);
      notifyCommit(listenerNotificationBatch);
      return response;
  }

  @POST
  @Path("commit")
  public Response commit(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders, String json) throws Throwable {
      ListenerNotificationBatch listenerNotificationBatch = new ListenerNotificationBatch(uriInfo, httpHeaders);
      JsonElement committedEntities = crysonSerializer.parse(json);
      crysonService.validatePermissions(committedEntities);
      List<Object> persistedEntites = new ArrayList<Object>();
      List<Object> updatedEntities = new ArrayList<Object>();
      JsonObject responseJsonObject = crysonService.commit(committedEntities, listenerNotificationBatch, persistedEntites, updatedEntities);
      crysonService.refreshEntities(responseJsonObject, listenerNotificationBatch, persistedEntites, updatedEntities);
      Response response = Response.ok(crysonSerializer.serializeTree(responseJsonObject)).build();

      notifyCommit(listenerNotificationBatch);
      return response;
  }
  
  public Response commitEntity(Object entity, UriInfo uriInfo, HttpHeaders httpHeaders) {
      ListenerNotificationBatch listenerNotificationBatch = new ListenerNotificationBatch(uriInfo, httpHeaders);
      Response response = crysonService.commitEntity(entity, listenerNotificationBatch);
      notifyCommit(listenerNotificationBatch);
      return response;
  }

  private void notifyCommit(ListenerNotificationBatch listenerNotificationBatch) {
    for (CrysonListener crysonListener : crysonListeners) {
      try {
        crysonListener.commitCompleted(listenerNotificationBatch);
      } catch(Throwable t) {
        logger.error("Cryson listener of class " + crysonListener.getClass().getSimpleName() + " threw after commit.", t);
      }
    }
  }


  private Set<String> splitAssociationsToFetch(String rawAssociationsToFetch) {
    if (rawAssociationsToFetch == null || rawAssociationsToFetch.equals("")) {
      return Collections.emptySet();
    }

    return new HashSet<String>(Arrays.asList(rawAssociationsToFetch.split(",")));
  }

}
