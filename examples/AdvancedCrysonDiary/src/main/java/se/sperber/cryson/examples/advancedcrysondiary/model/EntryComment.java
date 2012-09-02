package se.sperber.cryson.examples.advancedcrysondiary.model;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.security.core.Authentication;
import se.sperber.cryson.security.Restrictable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EntryComment extends BaseEntity implements Restrictable {

  @Lob
  private String text;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Entry entry;

  private Date created;


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
