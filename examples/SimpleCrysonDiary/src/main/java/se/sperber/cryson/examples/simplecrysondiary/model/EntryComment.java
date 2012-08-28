package se.sperber.cryson.examples.simplecrysondiary.model;

import javax.persistence.*;
import java.util.Date;

@Entity
public class EntryComment {

  @Id
  @GeneratedValue
  private Long id;

  @Lob
  private String text;

  @ManyToOne(fetch = FetchType.LAZY)
  private Entry entry;

  private Date created;

}
