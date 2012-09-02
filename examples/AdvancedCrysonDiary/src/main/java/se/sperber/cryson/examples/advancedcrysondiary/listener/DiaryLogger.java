package se.sperber.cryson.examples.advancedcrysondiary.listener;

import org.springframework.stereotype.Component;
import se.sperber.cryson.examples.advancedcrysondiary.model.Entry;
import se.sperber.cryson.listener.CommitNotification;
import se.sperber.cryson.listener.CrysonListener;

@Component
public class DiaryLogger implements CrysonListener {

  public void commitCompleted(CommitNotification commitNotification) {
    for(Object createdObject : commitNotification.getCreatedEntities()) {
      if (createdObject instanceof Entry) {
        Entry createdEntry = (Entry)createdObject;
        System.out.println("### " + createdEntry.getUserName() + " created diary entry '" + createdEntry.getTitle() + "'");
      }
    }
  }
}
