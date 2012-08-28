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

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CrysonTestAuthenticationProvider implements AuthenticationProvider {

  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    List<SimpleGrantedAuthority> grantedAuthorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), grantedAuthorities);
  }

  public boolean supports(Class<? extends Object> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

}
