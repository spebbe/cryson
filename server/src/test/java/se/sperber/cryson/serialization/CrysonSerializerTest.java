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

import com.google.gson.JsonElement;
import se.sperber.cryson.testutil.CrysonTestChildEntity;
import se.sperber.cryson.testutil.CrysonTestEntity;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class CrysonSerializerTest {

  private static CrysonSerializer givenCrysonSerializer() {
    CrysonSerializer crysonSerializer = new CrysonSerializer();
    ReflectionHelper reflectionHelper = new ReflectionHelper();
    LazyAssociationExclusionStrategy lazyAssociationExclusionStrategy = new LazyAssociationExclusionStrategy();
    UserTypeExclusionStrategy userTypeExclusionStrategy = new UserTypeExclusionStrategy();
    lazyAssociationExclusionStrategy.setReflectionHelper(reflectionHelper);
    crysonSerializer.setReflectionHelper(reflectionHelper);
    crysonSerializer.setLazyAssociationExclusionStrategy(lazyAssociationExclusionStrategy);
    crysonSerializer.setUserTypeExclusionStrategy(userTypeExclusionStrategy);
    crysonSerializer.setupGson();
    return crysonSerializer;
  }

  public static class WhenSerializing {
    
    @Test
    public void shouldSerialize() {
      CrysonSerializer crysonSerializer = givenCrysonSerializer();
      
      CrysonTestChildEntity testChildEntity = new CrysonTestChildEntity();
      testChildEntity.setId(100L);
      CrysonTestEntity testEntity = new CrysonTestEntity();
      testEntity.setId(1L);
      testEntity.setName("test");
      testEntity.setVersion(1L);
      testEntity.setChildEntities(Collections.singleton(testChildEntity));
      testChildEntity.setParent(testEntity);
      
      String serializedChildEntity = crysonSerializer.serialize(testChildEntity);
      String expectedSerializedChildEntity = "{\"id\":100,\"parent\":{\"id\":1,\"name\":\"test\",\"version\":1,\"crysonEntityClass\":\"CrysonTestEntity\",\"childEntities_cryson_ids\":[100]},\"crysonEntityClass\":\"CrysonTestChildEntity\"}";
      assertEquals(expectedSerializedChildEntity, serializedChildEntity);
    }
    
  }

  public static class WhenDeserializing {

    @Test
    public void shouldDeserializeWithToOneAssociation() throws Exception {
      CrysonSerializer crysonSerializer = givenCrysonSerializer();

      String serializedChildEntity = "{\"id\":100,\"parent_cryson_id\":1}";

      CrysonTestChildEntity deserializedChildEntity = crysonSerializer.deserialize(serializedChildEntity, CrysonTestChildEntity.class, null);

      assertNotNull(deserializedChildEntity);
      assertEquals((Long)100l, deserializedChildEntity.getId());
      assertEquals((Long)1l, deserializedChildEntity.getParent().getId());
    }

    @Test
    public void shouldDeserializeWithToManyAssociation() throws Exception {
      CrysonSerializer crysonSerializer = givenCrysonSerializer();

      String serializedEntity = "{\"id\":1,\"childEntities_cryson_ids\":[100]}";

      CrysonTestEntity deserializedEntity = crysonSerializer.deserialize(serializedEntity, CrysonTestEntity.class, null);

      assertNotNull(deserializedEntity);
      assertEquals((Long)1l, deserializedEntity.getId());
      assertEquals(1, deserializedEntity.getChildEntities().size());
      assertEquals((Long)100l, deserializedEntity.getChildEntities().iterator().next().getId());
    }
    
    @Test
    public void shouldDeserializeViaJsonElement() throws Exception {
      CrysonSerializer crysonSerializer = givenCrysonSerializer();

      String serializedChildEntity = "{\"id\":100,\"parent_cryson_id\":1}";

      JsonElement childEntityJsonElement = crysonSerializer.parse(serializedChildEntity);
      CrysonTestChildEntity deserializedChildEntity = crysonSerializer.deserialize(childEntityJsonElement, CrysonTestChildEntity.class, null);

      assertNotNull(deserializedChildEntity);
      assertEquals((Long)100l, deserializedChildEntity.getId());
      assertEquals((Long)1l, deserializedChildEntity.getParent().getId());
    }

  }
  
}
