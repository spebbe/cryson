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

package se.sperber.cryson.security;


import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CrysonPermissionEvaluator implements PermissionEvaluator {

  public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
    if (targetDomainObject instanceof Restrictable) {
      Restrictable restrictable = (Restrictable)targetDomainObject;
      if ("read".equals(permission)) {
        return restrictable.isReadableBy(authentication);
      } else if ("write".equals(permission)) {
        return restrictable.isWritableBy(authentication);
      } else {
        return true;
      }
    }
    return true;
  }

  public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
    // Never called, AFAIK...
    return false;
  }
}
