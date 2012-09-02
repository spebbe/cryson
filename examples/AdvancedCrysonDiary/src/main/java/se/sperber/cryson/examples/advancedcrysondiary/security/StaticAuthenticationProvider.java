package se.sperber.cryson.examples.advancedcrysondiary.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class StaticAuthenticationProvider implements AuthenticationProvider {

  private Map<String, String> users;

  @PostConstruct
  public void setupUsers() {
    users = new HashMap<String, String>();
    users.put("jack", "password");
    users.put("jill", "password");
    users.put("lurker", "password");
  }

  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (users.containsKey(authentication.getPrincipal()) && users.get(authentication.getPrincipal()).equals(authentication.getCredentials())) {
      List<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<SimpleGrantedAuthority>();
      grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
      if (authentication.getPrincipal().equals("lurker")) {
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_LURKER"));
      }
      return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), grantedAuthorities);
    } else {
      throw new BadCredentialsException("Authentication failed");
    }
  }

  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

}
