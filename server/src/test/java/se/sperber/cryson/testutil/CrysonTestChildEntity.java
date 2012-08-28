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

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@Audited
public class CrysonTestChildEntity {

  @Id @GeneratedValue
  private Long id;

  @ManyToOne
  private CrysonTestEntity parent;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CrysonTestEntity getParent() {
    return parent;
  }

  public void setParent(CrysonTestEntity parent) {
    this.parent = parent;
  }

}