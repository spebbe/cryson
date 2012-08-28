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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import java.lang.reflect.Type;

public class HibernateProxyTypeAdapter implements JsonSerializer<HibernateProxy> {

  private Gson gson;

  @Override
  public JsonElement serialize(HibernateProxy hibernateProxy, Type type, JsonSerializationContext jsonSerializationContext) {
    return gson.toJsonTree(initializeAndUnproxy(hibernateProxy));
  }

  static <T> T initializeAndUnproxy(T entity) {
    if (entity == null) {
      throw new NullPointerException("Entity passed for initialization is null");
    }

    Hibernate.initialize(entity);
    if (entity instanceof HibernateProxy) {
      entity = (T) ((HibernateProxy) entity).getHibernateLazyInitializer()
              .getImplementation();
    }
    return entity;
  }

  void setGson(Gson gson) {
    this.gson = gson;
  }
}
