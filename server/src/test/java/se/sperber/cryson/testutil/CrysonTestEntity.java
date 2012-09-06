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

package se.sperber.cryson.testutil;

import se.sperber.cryson.security.Restrictable;
import org.hibernate.envers.Audited;
import org.springframework.security.core.Authentication;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Set;

@Entity
@Audited
@NamedQueries({
        @NamedQuery(name = "CrysonTestEntity.findByName",
                query = "SELECT e FROM CrysonTestEntity e WHERE e.name = :name")
})
public class CrysonTestEntity implements Serializable, Restrictable {

  @Id @GeneratedValue
  private Long id;

  @Size(max = 30)
  private String name;
  
  @Version
  private long version;

  @OneToMany(mappedBy = "parent")
  private Set<CrysonTestChildEntity> childEntities;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public Set<CrysonTestChildEntity> getChildEntities() {
    return childEntities;
  }

  public void setChildEntities(Set<CrysonTestChildEntity> childEntities) {
    this.childEntities = childEntities;
  }

  @Override
  public boolean isReadableBy(Authentication authentication) {
    return true;
  }

  @Override
  public boolean isWritableBy(Authentication authentication) {
    return true;
  }
}