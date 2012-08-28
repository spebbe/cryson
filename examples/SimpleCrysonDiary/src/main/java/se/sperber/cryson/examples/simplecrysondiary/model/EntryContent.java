package se.sperber.cryson.examples.simplecrysondiary.model;

import javax.persistence.*;

@Entity
public class EntryContent {

  @Id
  @GeneratedValue
  private Long id;

  @Lob @Column(length = Integer.MAX_VALUE)
  private String text;

  @OneToOne(fetch = FetchType.LAZY)
  private Entry entry;

}
