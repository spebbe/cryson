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

package se.sperber.cryson.listener;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;
import java.util.Set;

public class ListenerNotificationBatch implements CommitNotification {
  
  private Set<Object> createdEntities = new HashSet<Object>();
  private Set<Object> updatedEntities = new HashSet<Object>();
  private Set<Object> deletedEntities = new HashSet<Object>();

  private UriInfo uriInfo;
  private HttpHeaders httpHeaders;

  public ListenerNotificationBatch(UriInfo uriInfo, HttpHeaders httpHeaders) {
    this.uriInfo = uriInfo;
    this.httpHeaders = httpHeaders;
  }


  public Iterable<Object> getCreatedEntities() {
    return createdEntities;
  }

  public Iterable<Object> getUpdatedEntities() {
    return updatedEntities;
  }

  public Iterable<Object> getDeletedEntities() {
    return deletedEntities;
  }

  public HttpHeaders getHttpHeaders() {
    return httpHeaders;
  }

  public UriInfo getUriInfo() {
    return uriInfo;
  }

  public void entityCreated(Object entity) {
    createdEntities.add(entity);
  }

  public void entityUpdated(Object entity) {
    updatedEntities.add(entity);
  }

  public void entityDeleted(Object entity) {
    deletedEntities.add(entity);
  }

}
