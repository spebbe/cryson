package se.sperber.cryson.examples.advancedcrysondiary.model;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import se.sperber.cryson.security.Restrictable;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Entry extends BaseEntity implements Restrictable {

  private String title;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private EntryContent content;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "entry")
  @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  @BatchSize(size = 10)
  private Set<EntryComment> comments;

  private Date date;

  @Column(nullable = false)
  private String userName;

  public String getTitle() {
    return title;
  }

  public String getUserName() {
    return userName;
  }


  public boolean isReadableBy(Authentication authentication) {
    return authentication.getPrincipal().equals(userName) || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_LURKER"));
  }

  public boolean isWritableBy(Authentication authentication) {
    return !authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_LURKER")) && authentication.getPrincipal().equals(userName);
  }
}
