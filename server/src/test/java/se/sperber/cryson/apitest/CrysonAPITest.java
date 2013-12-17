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

package se.sperber.cryson.apitest;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sperber.cryson.CrysonServer;
import se.sperber.cryson.initialization.Application;
import se.sperber.cryson.serialization.CrysonSerializer;
import se.sperber.cryson.testutil.CrysonTestEntity;

import java.net.URLEncoder;

import static org.junit.Assert.*;

public class CrysonAPITest {

  private static CrysonServer crysonServer;
  private static CrysonSerializer crysonSerializer;
  private static HttpClient httpClient;
  private static CrysonTestEntity foundEntity;

  @BeforeClass
  public static void setup() throws Exception
  {
    crysonServer = new CrysonServer();
    crysonServer.init(new String[]{"cryson-test.properties"});
    crysonServer.start();
    crysonSerializer = Application.get(CrysonSerializer.class);
    cleanDatabase();
    setupHttpClient();
  }

  private static void setupHttpClient() {
    Credentials defaultcreds = new UsernamePasswordCredentials("test", "testpassword");
    httpClient = new HttpClient();
    httpClient.getState().setCredentials(new AuthScope("localhost", 8789, AuthScope.ANY_REALM), defaultcreds);
    httpClient.getParams().setAuthenticationPreemptive(true);
  }


  private static void cleanDatabase() {
    Session session = Application.get(SessionFactory.class).openSession();
    session.createSQLQuery("DELETE FROM CrysonTestChildEntity").executeUpdate();
    session.createSQLQuery("DELETE FROM CrysonTestEntity").executeUpdate();
    session.close();
  }

  @AfterClass
  public static void teardown() throws Exception
  {
    crysonServer.stop();
    crysonServer.destroy();
  }
  
  @Test
  public void shouldGetEntityDefinitions() throws Exception {
    GetMethod getMethod = new GetMethod("http://localhost:8789/cryson/definition/CrysonTestEntity");
    httpClient.executeMethod(getMethod);
    assertEquals("{\"id\":\"Long\",\"name\":\"String\",\"doubleId\":\"Long\",\"shouldBeReadable\":\"boolean\",\"childEntities\":\"CrysonTestChildEntity\",\"crysonEntityClass\":\"String\",\"version\":\"long\"}", getMethod.getResponseBodyAsString());
  }

  @Test
  public void shouldCreateEntities() throws Exception {
    String serializedEntity = "{\"id\":null, \"name\":\"created\", \"childEntities_cryson_ids\":[100]}";

    PutMethod entityPutMethod = new PutMethod("http://localhost:8789/cryson/CrysonTestEntity");
    entityPutMethod.setRequestEntity(new StringRequestEntity(serializedEntity, "application/json", "UTF-8"));
    assertEquals(200, httpClient.executeMethod(entityPutMethod));

    CrysonTestEntity testEntity = crysonSerializer.deserialize(entityPutMethod.getResponseBodyAsString(), CrysonTestEntity.class, null);
    String serializedChildEntity = "{\"id\":null,\"parent_cryson_id\":" + testEntity.getId() + "}";

    PutMethod childEntityPutMethod = new PutMethod("http://localhost:8789/cryson/CrysonTestChildEntity");
    childEntityPutMethod.setRequestEntity(new StringRequestEntity(serializedChildEntity, "application/json", "UTF-8"));
    assertEquals(200, httpClient.executeMethod(childEntityPutMethod));
  }

  @Test
  public void shouldFindAllEntities() throws Exception {
    GetMethod getMethod = new GetMethod("http://localhost:8789/cryson/CrysonTestEntity/all");
    int status = httpClient.executeMethod(getMethod);
    assertEquals(HttpStatus.SC_OK, status);

    JsonElement jsonElement = crysonSerializer.parse(getMethod.getResponseBodyAsString());
    assertEquals(1, jsonElement.getAsJsonArray().size());
    foundEntity = crysonSerializer.deserialize(jsonElement.getAsJsonArray().get(0), CrysonTestEntity.class, null);
    assertEquals(1, foundEntity.getChildEntities().size());
  }
  
  @Test
  public void shouldFindEntityById() throws Exception {
    GetMethod getMethod = new GetMethod("http://localhost:8789/cryson/CrysonTestEntity/" + foundEntity.getId());
    int status = httpClient.executeMethod(getMethod);
    assertEquals(HttpStatus.SC_OK, status);
    
    CrysonTestEntity testEntity = crysonSerializer.deserialize(getMethod.getResponseBodyAsString(), CrysonTestEntity.class, null);
    assertEquals(foundEntity.getId(), testEntity.getId());
  }

  @Test
  public void shouldFindEntitiesByIds() throws Exception {
    GetMethod getMethod = new GetMethod("http://localhost:8789/cryson/CrysonTestEntity/1000," + foundEntity.getId() + ",2000,3000");
    int status = httpClient.executeMethod(getMethod);
    assertEquals(HttpStatus.SC_OK, status);

    JsonElement jsonElement = crysonSerializer.parse(getMethod.getResponseBodyAsString());
    assertEquals(1, jsonElement.getAsJsonArray().size());
    CrysonTestEntity testEntity = crysonSerializer.deserialize(jsonElement.getAsJsonArray().get(0), CrysonTestEntity.class, null);
    assertEquals(foundEntity.getId(), testEntity.getId());
  }

  @Test
  public void shouldFindEntitiesByIdsWithPost() throws Exception {
    PostMethod postMethod = new PostMethod("http://localhost:8789/cryson/CrysonTestEntity");
    String json = "{\"raw_ids\":\"1000,"+foundEntity.getId()+",2000,3000\", \"fetch\":\"\"}";
    StringRequestEntity requestEntity = new StringRequestEntity(json, "application/json", Charsets.UTF_8.name());
    postMethod.setRequestEntity(requestEntity);
    int status = httpClient.executeMethod(postMethod);
    assertEquals(HttpStatus.SC_OK, status);

    JsonElement jsonElement = crysonSerializer.parse(postMethod.getResponseBodyAsString());
    assertEquals(1, jsonElement.getAsJsonArray().size());
    CrysonTestEntity testEntity = crysonSerializer.deserialize(jsonElement.getAsJsonArray().get(0), CrysonTestEntity.class, null);
    assertEquals(foundEntity.getId(), testEntity.getId());
  }

  @Test
  public void shouldFindEntitiesByExample() throws Exception {
    GetMethod getMethod = new GetMethod("http://localhost:8789/cryson/CrysonTestEntity/?example=" + URLEncoder.encode(crysonSerializer.serialize(foundEntity), "UTF-8"));
    int status = httpClient.executeMethod(getMethod);
    assertEquals(HttpStatus.SC_OK, status);

    JsonElement jsonElement = crysonSerializer.parse(getMethod.getResponseBodyAsString());
    assertEquals(1, jsonElement.getAsJsonArray().size());
    CrysonTestEntity testEntity = crysonSerializer.deserialize(jsonElement.getAsJsonArray().get(0), CrysonTestEntity.class, null);
    assertEquals(foundEntity.getId(), testEntity.getId());
    assertEquals(foundEntity.getChildEntities().size(), testEntity.getChildEntities().size());
  }

  @Test
  public void shouldNotEmbedUnspecifiedLazyAssociations() throws Exception {
    GetMethod getMethod = new GetMethod("http://localhost:8789/cryson/CrysonTestEntity/" + foundEntity.getId());
    int status = httpClient.executeMethod(getMethod);
    assertEquals(HttpStatus.SC_OK, status);

    JsonElement jsonElement = crysonSerializer.parse(getMethod.getResponseBodyAsString());
    assertNull(jsonElement.getAsJsonObject().get("childEntities"));
    assertNotNull(jsonElement.getAsJsonObject().get("childEntities_cryson_ids"));
  }

  @Test
  public void shouldEmbedSpecifiedAssociations() throws Exception {
    GetMethod getMethod = new GetMethod("http://localhost:8789/cryson/CrysonTestEntity/" + foundEntity.getId() + "?fetch=childEntities");
    int status = httpClient.executeMethod(getMethod);
    assertEquals(HttpStatus.SC_OK, status);

    JsonElement jsonElement = crysonSerializer.parse(getMethod.getResponseBodyAsString());
    assertNotNull(jsonElement.getAsJsonObject().get("childEntities"));
    assertNull(jsonElement.getAsJsonObject().get("childEntities_cryson_ids"));
  }

  @Test
  public void shouldFindEntitiesByNamedQuery() throws Exception {
    PostMethod postMethod = new PostMethod("http://localhost:8789/cryson/namedQuery/CrysonTestEntity.findByName?fetch=childEntities");
    postMethod.setRequestEntity(new StringRequestEntity("{\"name\":\"created\"}", "application/json", "UTF-8"));
    int status = httpClient.executeMethod(postMethod);
    assertEquals(HttpStatus.SC_OK, status);

    JsonElement jsonElement = crysonSerializer.parse(postMethod.getResponseBodyAsString());
    assertEquals(1, jsonElement.getAsJsonArray().size());
    CrysonTestEntity testEntity = crysonSerializer.deserialize(jsonElement.getAsJsonArray().get(0), CrysonTestEntity.class, null);
    assertEquals(foundEntity.getId(), testEntity.getId());
    assertEquals((long)foundEntity.getChildEntities().iterator().next().getId(), jsonElement.getAsJsonArray().get(0).getAsJsonObject().get("childEntities").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsLong());
  }

  @Test
  public void shouldFindEntitiesByNamedQueryWithMultipleNames() throws Exception {
    PostMethod postMethod = new PostMethod("http://localhost:8789/cryson/namedQuery/CrysonTestEntity.findByNames?fetch=childEntities");
    postMethod.setRequestEntity(new StringRequestEntity("{\"names\":[\"notme\",\"created\",\"notthere\"]}", "application/json", "UTF-8"));
    int status = httpClient.executeMethod(postMethod);
    assertEquals(HttpStatus.SC_OK, status);

    JsonElement jsonElement = crysonSerializer.parse(postMethod.getResponseBodyAsString());
    assertEquals(1, jsonElement.getAsJsonArray().size());
    CrysonTestEntity testEntity = crysonSerializer.deserialize(jsonElement.getAsJsonArray().get(0), CrysonTestEntity.class, null);
    assertEquals(foundEntity.getId(), testEntity.getId());
    assertEquals((long)foundEntity.getChildEntities().iterator().next().getId(), jsonElement.getAsJsonArray().get(0).getAsJsonObject().get("childEntities").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsLong());
  }

  @Test
  public void shouldCommitEntitiesWithoutVersions() throws Exception {
    Long parentId = foundEntity.getId();
    Long childEntityId = foundEntity.getChildEntities().iterator().next().getId();
    String commitJson = "{\"updatedEntities\":[{\"crysonEntityClass\":\"CrysonTestChildEntity\",\"id\":" + childEntityId + "}], \"deletedEntities\":[], \"persistedEntities\":[]}";

    PostMethod postMethod = new PostMethod("http://localhost:8789/cryson/commit");
    postMethod.setRequestEntity(new StringRequestEntity(commitJson, "application/json", "UTF-8"));
    assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
    assertEquals("{\"replacedTemporaryIds\":{},\"persistedEntities\":[],\"updatedEntities\":[{\"id\":" + childEntityId + ",\"parent\":null,\"crysonEntityClass\":\"CrysonTestChildEntity\"}]}", postMethod.getResponseBodyAsString());
  }

  @Test
  public void shouldCommitEntities() throws Exception {
    Long entityId = foundEntity.getId();
    Long childEntityId = foundEntity.getChildEntities().iterator().next().getId();
    String commitJson = "{\"updatedEntities\":[{\"crysonEntityClass\":\"CrysonTestEntity\",\"id\":" + entityId + ",\"name\":\"updated\",\"childEntities_cryson_ids\":[], \"version\":0}], \"deletedEntities\":[{\"crysonEntityClass\":\"CrysonTestChildEntity\",\"id\":" + childEntityId + "}], \"persistedEntities\":[]}";

    PostMethod postMethod = new PostMethod("http://localhost:8789/cryson/commit");
    postMethod.setRequestEntity(new StringRequestEntity(commitJson, "application/json", "UTF-8"));
    assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
    assertEquals("{\"replacedTemporaryIds\":{},\"persistedEntities\":[],\"updatedEntities\":[{\"id\":" + entityId + ",\"name\":\"updated\",\"version\":1,\"crysonEntityClass\":\"CrysonTestEntity\",\"doubleId\":" + (entityId*2) + ",\"childEntities_cryson_ids\":[]}]}", postMethod.getResponseBodyAsString());
  }

  @Test
  public void shouldNotCommitStaleEntities() throws Exception {
    Long entityId = foundEntity.getId();
    Long childEntityId = foundEntity.getChildEntities().iterator().next().getId();
    String commitJson = "{\"updatedEntities\":[{\"crysonEntityClass\":\"CrysonTestEntity\",\"id\":" + entityId + ",\"name\":\"will not update\",\"childEntities_cryson_ids\":[]}], \"deletedEntities\":[], \"persistedEntities\":[]}";

    PostMethod postMethod = new PostMethod("http://localhost:8789/cryson/commit");
    postMethod.setRequestEntity(new StringRequestEntity(commitJson, "application/json", "UTF-8"));
    assertEquals(HttpStatus.SC_CONFLICT, httpClient.executeMethod(postMethod));
    assertEquals("{\"message\":\"Optimistic locking failed\"}", postMethod.getResponseBodyAsString());
  }

  @Test
  public void shouldNotCommitInvalidEntities() throws Exception {
    Long entityId = foundEntity.getId();
    String commitJson = "{\"updatedEntities\":[{\"crysonEntityClass\":\"CrysonTestEntity\",\"id\":" + entityId + ",\"name\":\"LONGNAMELONGNAMELONGNAMELONGNAMELONGNAME\",\"childEntities_cryson_ids\":[]}], \"deletedEntities\":[], \"persistedEntities\":[]}";

    PostMethod postMethod = new PostMethod("http://localhost:8789/cryson/commit");
    postMethod.setRequestEntity(new StringRequestEntity(commitJson, "application/json", "UTF-8"));
    assertEquals(HttpStatus.SC_FORBIDDEN, httpClient.executeMethod(postMethod));
    assertEquals("{\"message\":\"CrysonTestEntity name size must be between 0 and 30\\n\",\"validationFailures\":[{\"entityClass\":\"CrysonTestEntity\",\"entityId\":" + entityId + ",\"keyPath\":\"name\",\"message\":\"size must be between 0 and 30\"}]}", postMethod.getResponseBodyAsString());
  }

  @Test
  public void shouldTopologicallySortPersistedEntities() throws Exception {
    String commitJson = "{\"updatedEntities\":[], \"deletedEntities\":[], \"persistedEntities\":[{\"crysonEntityClass\":\"CrysonTestChildEntity\",\"id\":-1,\"parent_cryson_id\":-2},{\"crysonEntityClass\":\"CrysonTestEntity\",\"id\":-2,\"name\":\"test\",\"childEntities_cryson_ids\":[-1]}]}";
    PostMethod postMethod = new PostMethod("http://localhost:8789/cryson/commit");
    postMethod.setRequestEntity(new StringRequestEntity(commitJson, "application/json", "UTF-8"));
    assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
  }

}
