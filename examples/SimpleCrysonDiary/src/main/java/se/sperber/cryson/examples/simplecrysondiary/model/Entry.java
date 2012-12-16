package se.sperber.cryson.examples.simplecrysondiary.model;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
public class Entry {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  @OneToMany(fetch = FetchType.EAGER, mappedBy = "entry")
  private Set<EntryContent> contents;

  @OneToMany(fetch = FetchType.EAGER, mappedBy = "entry")
  private Set<EntryComment> comments;

  private Date date;

}
