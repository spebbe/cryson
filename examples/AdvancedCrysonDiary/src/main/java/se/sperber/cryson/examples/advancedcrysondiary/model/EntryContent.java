package se.sperber.cryson.examples.advancedcrysondiary.model;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.security.core.Authentication;
import se.sperber.cryson.security.Restrictable;

import javax.persistence.*;

@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EntryContent extends BaseEntity implements Restrictable {

  @Lob @Column(length = Integer.MAX_VALUE)
  private String text;

  @OneToOne(fetch = FetchType.LAZY, mappedBy = "content", optional = false)
  @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Entry entry;


  public boolean isReadableBy(Authentication authentication) {
    if (entry == null) {
      return true;
    }
    return entry.isReadableBy(authentication);
  }

  public boolean isWritableBy(Authentication authentication) {
    if (entry == null) {
      return true;
    }
    return entry.isWritableBy(authentication);
  }

}
